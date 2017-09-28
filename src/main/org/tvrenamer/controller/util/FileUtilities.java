package org.tvrenamer.controller.util;

import java.io.IOException;
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
    private static final Logger logger = Logger.getLogger(FileUtilities.class.getName());

    public static void loggingOff() {
        logger.setLevel(Level.SEVERE);
    }

    public static void loggingOn() {
        logger.setLevel(Level.INFO);
    }

    @SuppressWarnings("UnusedReturnValue")
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
        return Files.notExists(source);
    }

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
        try {
            FileStore fsA = Files.getFileStore(pathA);
            return fsA.equals(Files.getFileStore(pathB));
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "IOException trying to get file stores.", ioe);
            return false;
        }
    }

    @SuppressWarnings("unused")
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
        return Files.exists(dir);
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean isDirEmpty(final Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception checking directory " + dir, ioe);
            return false;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static boolean rmdir(final Path dir) {
        try {
            Files.delete(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to remove directory " + dir, ioe);
            return false;
        }
        return Files.notExists(dir);
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
