package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.ProgressObserver;
import org.tvrenamer.model.UserPreferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMover implements Callable<Boolean> {
    private static final Logger logger = Logger.getLogger(FileMover.class.getName());
    private static final UserPreferences userPrefs = UserPreferences.getInstance();

    private final FileEpisode episode;
    private final Path destRoot;
    private final String destBasename;
    private final String destSuffix;
    private ProgressObserver observer = null;
    Integer destIndex = null;

    /**
     * Constructs a FileMover to move the given episode.
     *
     * @param episode
     *    the FileEpisode we intend to move
     */
    public FileMover(FileEpisode episode) {
        this.episode = episode;

        destRoot = episode.getMoveToPath();
        destBasename = episode.getDestinationBasename();
        destSuffix = episode.getFilenameSuffix();
    }

    /**
     * Sets the progress observer for this FileMover
     *
     * @param observer
     *   the observer to add
     */
    public void addObserver(ProgressObserver observer) {
        this.observer = observer;
    }

    /**
     * Gets the current location of the file to be moved
     *
     * @return the Path where the file is currently located
     */
    Path getCurrentPath() {
        return episode.getPath();
    }

    /**
     * Gets the size (in bytes) of the file to be moved
     *
     * @return the size of the file
     */
    long getFileSize() {
        return episode.getFileSize();
    }

    /**
     * The filename of the destination we want to move the file to.
     * We may not be able to actually use this filename due to a conflict,
     * in which case, we will probably add an index and use a subdirectory.
     * But this is the name we WANT to change it to.
     *
     * @return the filename that we want to move the file to
     */
    String getDesiredDestName() {
        return destBasename + destSuffix;
    }

    /**
     * Gets the name of the directory we should move the file to, as a string.
     *
     * We call it the "moveToDirectory" because "destinationDirectory" is used more
     * to refer to the top-level directory: the one the user specified in the dialog
     * for user preferences.  This is the subdirectory of that folder that the file
     * should actually be placed in.
     *
     * @return the name of the directory we should move the file to, as a string.
     */
    String getMoveToDirectory() {
        return destRoot.toString();
    }

    /**
     * Copies the source file to the destination, and deletes the source.
     *
     * If the destination cannot be created or is a read-only file, the method returns
     * <code>false</code>. Otherwise, the contents of the source are copied to the destination,
     * the source is deleted, and <code>true</code> is returned.
     *
     * @param source
     *            The source file to move.
     * @param dest
     *            The destination where to move the file.
     * @return true on success, false otherwise.
     *
     * Based on a version originally implemented in jEdit 4.3pre9
     */
    private boolean copyAndDelete(final Path source, final Path dest) {
        if (observer != null) {
            observer.initializeProgress(episode.getFileSize());
        }
        boolean ok = false;
        try (OutputStream fos = Files.newOutputStream(dest);
             InputStream fis = Files.newInputStream(source))
        {
            byte[] buffer = new byte[32768];
            int n;
            long copied = 0L;
            while (-1 != (n = fis.read(buffer))) {
                fos.write(buffer, 0, n);
                copied += n;
                if (observer != null) {
                    observer.setProgressStatus(StringUtils.formatFileSize(copied));
                    observer.setProgressValue(copied);
                }
                if (Thread.interrupted()) {
                    break;
                }
            }
            if (-1 == n) {
                ok = true;
            }
        } catch (IOException ioe) {
            ok = false;
            logger.log(Level.WARNING, "Error moving file " + source + ": " + ioe.getMessage(), ioe);
        }

        if (ok) {
            // TODO: the newly created file will not necessarily have the same attributes as
            // the original.  In some cases, like ownership, that might actually be desirable
            // (have the copy be owned by the user running the program).  But there may be
            // other attributes we should try to adopt.  In any case, requires investigation.
            ok = FileUtilities.deleteFile(source);
        } else {
            logger.warning("failed to move " + source);
        }
        if (observer != null) {
            observer.finishProgress(ok);
        }
        return ok;
    }

    /**
     * Execute the file move action.  This method assumes that all sanity checks have been
     * completed and that everything is ready to go: source file and destination directory
     * exist, destination file doesn't, etc.
     *
     * At the end, if the move was successful, it sets the file modification time.
     *
     * @param srcPath
     *    the Path to the file to be moved
     * @param destPath
     *    the Path to which the file should be moved
     * @param tryRename
     *    if false, do not try to simply rename the file; always do a "copy-and-delete"
     * @return true on complete success, false otherwise.
     */
    private boolean doActualMove(final Path srcPath, final Path destPath, final boolean tryRename) {
        logger.fine("Going to move\n  '" + srcPath + "'\n  '" + destPath + "'");
        Path actualDest;
        if (tryRename) {
            try {
                actualDest = Files.move(srcPath, destPath);
                if (observer != null) {
                    observer.finishProgress(true);
                }
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to move " + srcPath, ioe);
                if (observer != null) {
                    observer.finishProgress(false);
                }
                return false;
            }
        } else {
            logger.info("different disks: " + srcPath + " and " + destPath);
            boolean success = copyAndDelete(srcPath, destPath);
            // TODO: what about file attributes?  In the case of owner, it might be
            // desirable to change it, or not.  What about writability?  And the
            // newer, more system-specific attributes, like "this file was downloaded
            // from the internet"?
            if (success) {
                actualDest = destPath;
            } else {
                actualDest = null;
            }
        }
        if (actualDest != null) {
            episode.setPath(actualDest);
        }
        boolean same = destPath.equals(actualDest);
        if (!same) {
            logger.warning("actual destination did not match intended:\n  "
                           + actualDest + "\n  " + destPath);
            return false;
        }

        // TODO: why do we set the file modification time to "now"?  Would like to
        // at least make this behavior configurable.
        try {
            FileTime now = FileTime.fromMillis(System.currentTimeMillis());
            Files.setLastModifiedTime(actualDest, now);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Unable to set modification time " + srcPath, ioe);
            // Well, the file got moved to the right place already.  One could argue
            // for returning true.  But, true is only if *everything* worked.
            return false;
        }

        return true;
    }

    /**
     * Execute the move using real paths.  Also does side-effects, like
     * updating the FileEpisode.
     *
     * @param realSrc
     *    the "real" Path of the source file to be moved
     * @param destPath
     *    the "real" destination where the file should be moved; can contain
     *    non-existent directories, which will be created
     * @param destDir
     *    an existent ancestor of destPath
     * @return true on success, false otherwise.
     */
    private boolean tryToMoveRealPaths(Path realSrc, Path destPath, Path destDir) {
        boolean tryRename = FileUtilities.areSameDisk(realSrc, destDir);
        Path srcDir = realSrc.getParent();

        episode.setMoving();
        boolean success = doActualMove(realSrc, destPath, tryRename);
        if (!success) {
            logger.info("failed to move " + realSrc);
            return false;
        }

        logger.info("successful:\n  " + realSrc + "\n  " + destPath);
        if (userPrefs.isRemoveEmptiedDirectories()) {
            FileUtilities.removeWhileEmpty(srcDir);
        }
        return true;
    }

    /**
     * Add a version string to the destination filename.
     *
     * @return destination filename with a version added
     */
    private String addVersionString() {
        return destBasename + " (" + destIndex + ")" + destSuffix;
    }

    /**
     * Check/verify numerous things, and if everything is as it should be,
     * execute the move.
     *
     * This sanity-checks the move: the source file must exist, the destination
     * file should not, etc.  It may actually change things, e.g., if the
     * destination directory doesn't exist, it will try to create it.  It also
     * gathers information, like whether the source and destination are on the
     * same file store.  And it does side-effects, like updating the FileEpisode.
     *
     * @return true on success, false otherwise.
     */
    private boolean tryToMoveFile() {
        Path srcPath = episode.getPath();
        if (Files.notExists(srcPath)) {
            logger.info("Path no longer exists: " + srcPath);
            episode.setDoesNotExist();
            return false;
        }
        Path realSrc;
        try {
            realSrc = srcPath.toRealPath();
        } catch (IOException ioe) {
            logger.warning("could not get real path of " + srcPath);
            return false;
        }
        Path destDir = destRoot;
        String filename = destBasename + destSuffix;
        if (destIndex != null) {
            if (userPrefs.isMoveEnabled()) {
                destDir = destRoot.resolve(DUPLICATES_DIRECTORY);
            }
            filename = addVersionString();
        }

        if (!FileUtilities.ensureWritableDirectory(destDir)) {
            logger.warning("not attempting to move " + srcPath);
            return false;
        }

        try {
            destDir = destDir.toRealPath();
        } catch (IOException ioe) {
            logger.warning("could not get real path of " + destDir);
            return false;
        }

        Path destPath = destDir.resolve(filename);
        if (Files.exists(destPath)) {
            if (destPath.equals(realSrc)) {
                logger.info("nothing to be done to " + srcPath);
                return true;
            }
            logger.warning("cannot move; destination exists:\n  " + destPath);
            return false;
        }

        return tryToMoveRealPaths(realSrc, destPath, destDir);
    }

    /**
     * Do the move.
     *
     * Using the attributes set in this instance, execute the move functionality.
     * In reality, this method is little more than a wrapper for getting the return
     * value right.
     *
     * @return true on success, false otherwise.
     */
    @Override
    public Boolean call() {
        boolean success = false;
        try {
            // There are numerous reasons why the move would fail.  Instead of calling
            // setFailToMove on the episode in each individual case, make the functionality
            // into a subfunction, and set the episode here for any of the failure cases.
            success = tryToMoveFile();
        } catch (Exception e) {
            logger.log(Level.WARNING, "exception caught doing file move", e);
        }
        if (success) {
            episode.setRenamed();
        } else {
            episode.setFailToMove();
        }
        return success;
    }
}
