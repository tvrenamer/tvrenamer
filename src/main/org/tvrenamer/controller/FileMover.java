package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.ProgressObserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileMover implements Callable<Boolean> {
    private static Logger logger = Logger.getLogger(FileMover.class.getName());

    private final FileEpisode episode;
    private final String destRootName;
    private final String destBasename;
    private final String destSuffix;
    private final ProgressObserver observer;

    public FileMover(FileEpisode episode, ProgressObserver observer) {
        this.episode = episode;
        this.observer = observer;

        destRootName = episode.getMoveToDirectory();
        destBasename = episode.getRenamedBasename();
        destSuffix = episode.getFilenameSuffix();
    }

    /**
     * Gets the name of the directory we should move the file to, as a string.
     *
     * We call it the "moveToDirectory" because "destinationDirectory" is used more
     * to refer to the top-level directory: the one the user specified in the dialog
     * for user preferences.  This is the subdirectory of that folder that the file
     * should actually be placed in.
     *
     * But, it doesn't make sense if we don't have enough information about the episode.
     * We never should have gotten here if that's the case, but we'll do a sanity check
     * here.
     *
     * @return the name of the directory we should move the file to, as a string.
     */
    String getMoveToDirectory() {
        // Skip files not successfully downloaded and ready to be moved
        if (!episode.isReady()) {
            logger.info("selected but not ready: " + episode.getFilepath());
            return null;
        }

        return episode.getMoveToDirectory();
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
                    observer.setStatus(StringUtils.formatFileSize(copied));
                    observer.setValue(copied);
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
            FileUtilities.deleteFile(source);
        } else {
            logger.warning("failed to move " + source);
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
     * @return true on complete success, false otherwise.
     */
    private boolean doActualMove(final Path srcPath, final Path destPath, final boolean tryRename) {
        logger.fine("Going to move\n  '" + srcPath + "'\n  '" + destPath + "'");
        Path actualDest = null;
        if (tryRename) {
            try {
                actualDest = Files.move(srcPath, destPath);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to move " + srcPath, ioe);
                return false;
            }
        } else {
            logger.info("different disks: " + srcPath + " and " + destPath);
            observer.setMaximum(episode.getFileSize());
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
        episode.setPath(actualDest);
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
        Path destDir = Paths.get(destRootName);
        if (Files.notExists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to create directory " + destDir, ioe);
                return false;
            }
        }
        if (!Files.exists(destDir)) {
            logger.warning("could not create destination directory " + destDir
                           + "; not attempting to move " + srcPath);
            return false;
        }
        if (!Files.isDirectory(destDir)) {
            logger.warning("cannot use specified destination " + destDir
                           + "because it is not a directory; not attempting to move "
                           + srcPath);
            return false;
        }

        String filename = destBasename + destSuffix;
        Path destPath = destDir.resolve(filename);
        if (Files.exists(destPath)) {
            if (destPath.equals(srcPath)) {
                logger.info("nothing to be done to " + srcPath);
                return true;
            }
            logger.warning("cannot move; destination exists:\n  " + destPath);
            return false;
        }
        if (!Files.isWritable(destDir)) {
            logger.warning("cannot write file to " + destDir);
            return false;
        }

        boolean tryRename = FileUtilities.areSameDisk(srcPath, destDir);
        Path srcDir = srcPath.getParent();

        episode.setMoving();
        if (false == doActualMove(srcPath, destPath, tryRename)) {
            return false;
        }

        logger.fine("successful:\n  " + srcPath.toAbsolutePath().toString()
                    + "\n  " + destPath.toAbsolutePath().toString());
        FileUtilities.removeWhileEmpty(srcDir);
        return true;
    }

    /**
     * Do the move.
     *
     * Using the attributes set in this instance, execute the move functionality.
     * In reality, this method is little more than a wrapper for getting the return
     * value right and cleaning up the observer afterwards.
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
            logger.warning("exception caught doing file move: " + e);
        } finally {
            // We only use the label if we're doing the "copy-and-delete" method.
            // But we have created it beforehand, not knowing which method we'd be using.
            // So, regardless of how we tried to move the file, and regardless of whether
            // it succeeded or not, we need to get rid of the label.
            observer.cleanUp();
        }
        if (success) {
            episode.setRenamed();
        } else {
            episode.setFailToMove();
        }
        return success;
    }
}
