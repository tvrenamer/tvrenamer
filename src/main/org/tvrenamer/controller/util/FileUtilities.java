package org.tvrenamer.controller.util;

import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.ProgressObserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Vipul Delwadia
 * @since 2010/09/14
 *
 */
public class FileUtilities {
    private static Logger logger = Logger.getLogger(FileUtilities.class.getName());

    public static boolean deleteFile(Path source) {
        if (Files.notExists(source)) {
            logger.warning("cannot delete file, does not exist: " + source);
            return false;
        }
        try {
            Files.delete(source);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error deleting file " + source, ioe);
            return false;
        }
        if (Files.notExists(source)) {
            return true;
        }
        return false;
    }

    // {{{ moveFile() method, based on the moveFile() method in gjt
    /**
     * Moves the source file to the destination.
     *
     * If the destination cannot be created or is a read-only file, the method returns <code>false</code>. Otherwise,
     * the contents of the source are copied to the destination, the source is deleted, and <code>true</code> is
     * returned.
     *
     * @param source
     *            The source file to move.
     * @param dest
     *            The destination where to move the file.
     * @param observer
     *            The observer to notify (can be null).
     * @return true on success, false otherwise.
     *
     * Based on a version originally implemented in jEdit 4.3pre9
     */
    public static boolean moveFile(Path source, Path dest, ProgressObserver observer) {
        if (Files.notExists(source)) {
            logger.warning("source file to move does not exist: " + source);
            return false;
        }
        if (Files.exists(dest)) {
            logger.warning("will not overwrite file: " + dest);
            return false;
        }
        Path destDir = dest.getParent();
        if (!Files.isWritable(destDir)) {
            logger.warning("cannot write file to " + destDir);
            return false;
        }

        boolean ok = false;

        try (OutputStream fos = Files.newOutputStream(dest);
             InputStream fis = Files.newInputStream(source)
             )
        {
            ok = IOUtilities.copyStream(32768, observer, fis, fos, true);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error moving file " + source + ": " + ioe.getMessage(), ioe);
        }

        if (ok) {
            deleteFile(source);
        }
        return ok;
    } // }}}

    /**
     * areSameDisk -- returns true if two Paths exist on the same FileStore.
     *
     * The intended usage is to find out if "moving" a file can be done with
     * a simple rename, or if the bits must be copied to a new disk.  In this
     * case, pass in the source file and the destination _folder_, making sure
     * to create the destination folder first if it doesn't exist.  (Or, pass
     * in its parent, or parent's parent, etc.)
     *
     * @param pathA - an existing path
     * @param pathB - a different existing path
     * @return true if both Paths exist and are located on the same FileStore
     *
     */
    public static boolean areSameDisk(Path pathA, Path pathB) {
        if (Files.notExists(pathA)) {
            logger.warning("areSameDisk: path " + pathA + " does not exist.");
            return false;
        }
        if (Files.notExists(pathB)) {
            logger.warning("areSameDisk: path " + pathB + " does not exist.");
            return false;
        }
        FileStore fsA = null;
        FileStore fsB = null;
        try {
            fsA = Files.getFileStore(pathA);
            fsB = Files.getFileStore(pathB);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "IOException trying to get file stores.", ioe);
            return false;
        }
        if (fsA.equals(fsB)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isSameFile(final Path path1, final Path path2) {
        try {
            return Files.isSameFile(path1, path2);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception checking files "
                       + path1 + " and " + path2, ioe);
            return false;
        }
    }

    public static boolean mkdirs(final Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to create directory " + dir, ioe);
            return false;
        }
        if (Files.exists(dir)) {
            return true;
        }
        return false;
    }

    public static boolean isDirEmpty(final Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception checking directory " + dir, ioe);
            return false;
        }
    }

    public static boolean rmdir(final Path dir) {
        try {
            Files.delete(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to remove directory " + dir, ioe);
            return false;
        }
        if (Files.notExists(dir)) {
            return true;
        }
        return false;
    }

    public static boolean removeWhileEmpty(final Path dir) {
        if (dir == null) {
            return false;
        }
        if (Files.notExists(dir)) {
            return false;
        }
        if (!Files.isDirectory(dir)) {
            return false;
        }
        if (!isDirEmpty(dir)) {
            // If the directory is not empty, then doing nothing is correct,
            // and we have succeeded.
            return true;
        }

        Path parent = dir.getParent();
        boolean success = rmdir(dir);
        if (success) {
            logger.info("removed empty directory " + dir);
            if (parent != null) {
                return removeWhileEmpty(parent);
            }
        }
        return success;
    }
}
