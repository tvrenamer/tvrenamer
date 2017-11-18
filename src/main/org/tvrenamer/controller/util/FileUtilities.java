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

    /**
     * Delete the given file.  This method is intended to be used with "regular" files;
     * to delete an empty directory, use rmdir().
     *
     * (Implementation detail: this method actually should work fine to remove an empty
     *  directory; the preference for rmdir() is purely a convention.)
     *
     * @param file
     *    the file to be deleted
     * @return
     *    true if the file existed and was deleted; false if not
     */
    public static boolean deleteFile(Path file) {
        if (Files.notExists(file)) {
            logger.warning("cannot delete file, does not exist: " + file);
            return false;
        }
        try {
            Files.delete(file);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error deleting file " + file, ioe);
            return false;
        }
        return Files.notExists(file);
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

    /**
     * Return true if the given arguments refer to the same actual file on the
     * file system.  On file systems that support symbolic links, two Paths could
     * be the same file even if their locations appear completely different.
     *
     * @param path1
     *    first Path to compare
     * @param path2
     *    second Path to compare
     * @return
     *    true if the paths refer to the same file, false if they don't
     */
    @SuppressWarnings("SameParameterValue")
    public static boolean isSameFile(final Path path1, final Path path2) {
        try {
            return Files.isSameFile(path1, path2);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception checking files "
                       + path1 + " and " + path2, ioe);
            return false;
        }
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     * No exception is thrown if the directory could not be created because it
     * already exists.
     *
     * If this method fails, then it may do so after creating some, but not all,
     * of the parent directories.
     *
     * @param dir - the directory to create
     * @return
     *    true if the the directory exists at the conclusion of this method:
     *    that is, true if the directory already existed, or if we created it;
     *    false if it we could not create the directory
     */
    public static boolean mkdirs(final Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to create directory " + dir, ioe);
            return false;
        }
        return Files.exists(dir);
    }

    /**
     * Takes a Path which is a directory that the user wants to write into.  Makes sure
     * that the directory exists (or creates it if it doesn't) and is writable.  If the
     * directory cannot be created, or is not a directory, or is not writable, this method
     * fails.
     *
     * @param destDir
     *    the Path that the caller will want to write into
     * @return true if, upon completion of this method, the desired Path exists, is a
     *         directory, and is writable.  False otherwise.
     */
    public static boolean ensureWritableDirectory(final Path destDir) {
        if (Files.notExists(destDir)) {
            try {
                Files.createDirectories(destDir);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, "Unable to create directory " + destDir, ioe);
                return false;
            }
        }
        if (!Files.exists(destDir)) {
            logger.warning("could not create destination directory " + destDir);
            return false;
        }
        if (!Files.isDirectory(destDir)) {
            logger.warning("cannot use specified destination " + destDir
                           + " because it is not a directory");
            return false;
        }
        if (!Files.isWritable(destDir)) {
            logger.warning("cannot write file to " + destDir);
            return false;
        }

        return true;
    }

    /**
     * Return true if the given argument is an empty directory.
     *
     * @param dir
     *    the directory to check for emptiness
     * @return
     *    true if the path existed and was an empty directory; false otherwise
     */
    public static boolean isDirEmpty(final Path dir) {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator().hasNext();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception checking directory " + dir, ioe);
            return false;
        }
    }

    /**
     * If the given argument is an empty directory, remove it.
     *
     * @param dir
     *    the directory to delete if empty
     * @return
     *    true if the path existed and was deleted; false if not
     */
    public static boolean rmdir(final Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try {
            Files.delete(dir);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "exception trying to remove directory " + dir, ioe);
            return false;
        }
        return Files.notExists(dir);
    }

    /**
     * If the given argument is an empty directory, remove it; and then, check
     * if removing it caused its parent to be empty, and if so, remove the parent;
     * and so on, until we hit a non-empty directory.
     *
     * @param dir
     *    the leaf directory to check for emptiness
     * @return true if the Path is an existent directory, and we succeeded in removing
     *    any empty directories we tried; false if the Path was null, didn't exist,
     *    or was not a directory, or if we can't remove a directory we tried to remove
     */
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
