package org.tvrenamer.controller;

import org.tvrenamer.view.ProgressBarUpdater;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MoveRunner implements Runnable {
    private static Logger logger = Logger.getLogger(MoveRunner.class.getName());

    private static final int DEFAULT_TIMEOUT = 120;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private final Thread progressThread = new Thread(this);
    private final Queue<Future<Boolean>> futures = new LinkedList<>();
    private final ProgressBarUpdater updater;
    private final int numMoves;
    private final int timeout;

    /**
     * Does the activity of the thread, which is to dequeue a move task, and block
     * until it returns, then update the progress bar and repeat the whole thing,
     * until the queue is empty.
     */
    @Override
    public void run() {
        while (true) {
            int remaining = futures.size();
            updater.setProgress(numMoves, remaining);

            if (remaining > 0) {
                final Future<Boolean> future = futures.remove();
                try {
                    Boolean success = future.get(timeout, TimeUnit.SECONDS);
                    logger.finer("future returned: " + success);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    logger.log(Level.WARNING, "exception executing move", e);
                }
            } else {
                updater.finish();
                return;
            }
        }
    }

    /**
     * Runs the thread for this FileMover, to move all the files.
     *
     * This actually could be done right in the constructor, as that is, in fact, the only
     * way it's only currently used.  But it's nice to let it be more explicit.
     *
     */
    public void runThread() {
        progressThread.start();
    }

    /**
     * If the given key is not present in the map, returns a new, empty list.
     * Otherwise, returns the value mapped to the key.
     *
     */
    // TODO: make this a generic facility?
    private static List<FileMover> getListValue(Map<String, List<FileMover>> table,
                                                String key)
    {
        if (table.containsKey(key)) {
            return table.get(key);
        }

        List<FileMover> newList = new LinkedList<FileMover>();
        table.put(key, newList);
        return newList;
    }

    /**
     * Adds an index to files that would otherwise conflict with other files.
     *
     * There are a lot of ways to approach the indexing, as discussed in the
     * doc of resolveConflicts, below; but as a first pass, we:
     * - consider only the basename for a conflict
     * - leave existing files as they are
     * - add indexes to conflicting files in the files we're moving
     *
     * @param moves the files which we want to move to the destination
     * @param existing the files which are already at the destination, and
     *        which the user has not specifically asked to move
     * @return nothing; modifies the entries of "moves" in-place
     */
    private static void addIndices(List<FileMover> moves, Set<Path> existing) {
        int index = existing.size();
        moves.sort(new Comparator<FileMover>() {
                public int compare(FileMover m1, FileMover m2) {
                    return (int) (m2.getFileSize() - m1.getFileSize());
                }
            });
        for (FileMover move : moves) {
            index++;
            if (index > 1) {
                move.destIndex = index;
            }
        }
    }

    /**
     * Finds existing conflicts
     *
     *
     */
    private static Set<Path> existingConflicts(String destDirName,
                                               String basename,
                                               List<FileMover> moves)
    {
        Set<Path> hits = new HashSet<>();
        Path destDir = Paths.get(destDirName);
        if (Files.exists(destDir) && Files.isDirectory(destDir)) {
            // TODO: there are better ways of filtering files than globbing,
            // especially when we're dealing with files that may have literal
            // brackets in their names.
            String glob = basename.replaceAll("\\[", "\\\\[") + "*";
            try (DirectoryStream<Path> contents
                 = Files.newDirectoryStream(destDir, glob))
            {
                Iterator<Path> it = contents.iterator();
                while (it.hasNext()) {
                    hits.add(it.next());
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception descending " + destDir, ioe);
            }
        }
        if (!hits.isEmpty()) {
            Set<Path> toMove = new HashSet<>();
            for (FileMover move : moves) {
                toMove.add(move.getCurrentPath());
            }
            try {
                for (Path pathToMove : toMove) {
                    Set<Path> newHits = new HashSet<>();
                    for (Path hit : hits) {
                        logger.info("comparing " + pathToMove + " and " + hit);
                        if (!Files.isSameFile(pathToMove, hit)) {
                            logger.fine("conflict: " + hit);
                            newHits.add(hit);
                        }
                    }
                    hits = newHits;
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "IO Exception comparing files", ioe);
            }
        }
        return hits;
    }

    /**
     * Resolves conflicts between episode names
     *
     * There are many different ways of renaming.  Some questions we might
     * deal with in the future:
     * - can we rename the files that are already in the destination?
     * - assuming two files refer to the same episode, is it still a conflict if:
     *   - they are different resolution?
     *   - they are different file formats (e.g., avi, mp4)?
     * - what do we do with identical files?
     *   - could treat as any other "conflict", or move into a special folder
     *   - but if we verify they are byte-for-byte duplicates, really no point
     *   - when we log all moves, for undo-ability, need to keep track of
     *     multiple file names that mapped to the same result
     * - do we prioritize by file type?  file size?  resolution?
     *     source (dvdrip, etc.)?
     * - can we integrate with a library that gives us information about the
     *   content (actual video quality, length, etc.)?
     *
     *
     */
    private static void resolveConflicts(List<FileMover> listOfMoves, String destDir) {
        Map<String, List<FileMover>> basenames = new HashMap<>();
        for (FileMover move : listOfMoves) {
            getListValue(basenames, move.getDestBasename()).add(move);
        }
        for (Map.Entry<String, List<FileMover>> entry : basenames.entrySet()) {
            String basename = entry.getKey();
            List<FileMover> moves = entry.getValue();
            Set<Path> existing = existingConflicts(destDir, basename, moves);
            int nFiles = existing.size() + moves.size();
            if (nFiles > 1) {
                addIndices(moves, existing);
            }
        }
    }

    /**
     * Turns a flat list of file moves into a hash map keyed on destination directory;
     *
     *
     */
    private static Map<String, List<FileMover>> mapByDestDir(final List<FileMover> episodes) {
        final Map<String, List<FileMover>> toMove = new HashMap<>();

        for (final FileMover pendingMove : episodes) {
            String moveToDir = pendingMove.getMoveToDirectory();
            List<FileMover> existingDirMoves = getListValue(toMove, moveToDir);
            existingDirMoves.add(pendingMove);
        }

        return toMove;
    }

    /**
     * Creates a MoveRunner to move all the episodes in the list, and update the progress
     * bar, using the specified timeout.
     *
     * @param episodes a list of FileMovers to execute
     * @param updater a ProgressBarUpdater to be informed of our progress
     * @param timeout the number of seconds to allow each FileMover to run, before killing it
     *
     */
    public MoveRunner(final List<FileMover> episodes,
                      final ProgressBarUpdater updater,
                      final int timeout)
    {
        this.updater = updater;
        this.timeout = timeout;

        progressThread.setName("MoveRunnerThread");
        progressThread.setDaemon(true);

        final Map<String, List<FileMover>> mappings = mapByDestDir(episodes);
        for (Map.Entry<String, List<FileMover>> entry : mappings.entrySet()) {
            resolveConflicts(entry.getValue(), entry.getKey());
        }

        int count = 0;
        for (List<FileMover> moves : mappings.values()) {
            for (FileMover move : moves) {
                futures.add(EXECUTOR.submit(move));
                count++;
            }
        }
        numMoves = count;
        logger.fine("have " + numMoves + " files to move");
    }

    /**
     * Creates a MoveRunner to move all the episodes in the list, and update the progress
     * bar, using the default timeout.
     *
     * @param episodes a list of FileMovers to execute
     * @param updater a ProgressBarUpdater to be informed of our progress
     *
     */
    public MoveRunner(final List<FileMover> episodes, final ProgressBarUpdater updater) {
        this(episodes, updater, DEFAULT_TIMEOUT);
    }

    /**
     * Shut down all the threads.
     *
     * This is intended for usage just in case the program wants to shut down while the
     * moves are still running.
     *
     */
    public static void shutDown() {
        EXECUTOR.shutdownNow();
    }
}
