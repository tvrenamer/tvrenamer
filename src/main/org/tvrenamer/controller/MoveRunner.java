package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.view.ProgressBarUpdater;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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

        int count = 0;
        for (FileMover move : episodes) {
            futures.add(EXECUTOR.submit(move));
            count++;
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
