package org.tvrenamer.controller.util;

import org.tvrenamer.model.MoveObserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AccessDeniedException;
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
        } catch (AccessDeniedException ade) {
            logger.warning("Could not delete file \"" + file
                           + "\"; access denied");
            return false;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error deleting file " + file, ioe);
            return false;
        }
        return Files.notExists(file);
    }

    /**
     * Try to figure out the state of the world after a call to Files.move
     * did not succeed, nor did it completely fail.
     *
     * <p>Note, java.nio.file.Files.move() is defined to return the destination.
     * Is it possible that the file could be moved, but to a different
     * destination than was requested?  Nothing in the Javadoc suggests that it
     * could.
     *
     * <p>As far as I can tell, the best reason for Files.move to return a Path,
     * rather than a boolean "success" value, is to simplify the user's code
     * when the destination is passed in as an expression, so that instead of:
     * <pre>
     * <code>
     *    Path destFile = destDir.resolve(basename + suffix)
     *    boolean success = Files.move(src, destFile);
     *    if (success) {
     *      // do something with destFile
     * </code>
     * </pre>
     * ... you can do:
     * <pre>
     * <code>
     *    Path destFile = Files.move(src, destDir.resolve(basename + suffix));
     *    if (destFile != null) {
     *      // do something with destFile
     * </code>
     * </pre>
     *
     * <p>Nevertheless, I don't know that for 100% certain.  The specification
     * implies a hypothetical possibility that the file might be moved, but to a
     * location different from target.  Particularly since the whole point of
     * this program is to move files, I want to try to be as careful as we can
     * possibly be to not lose (track of) any files.
     *
     * @param srcFile
     *    the file we wanted to rename
     * @param destFile
     *    the destination we wanted file to be renamed to
     * @param actualDest
     *    the value that Files.move() returned to us
     * @return
     *    presumably null, but could return a Path if, somehow, the source file
     *    appears to have been moved despite the apparent failure
     */
    private static Path unexpectedMoveResult(final Path srcFile, final Path destFile,
                                             final Path actualDest)
    {
        if (Files.exists(srcFile)) {
            // This implies that the original file was not touched.  Java may have
            // done an incomplete copy.
            if (Files.exists(destFile)) {
                logger.warning("may have done an incomplete copy of " + srcFile
                               + " to " + destFile);
            }
            if (Files.exists(actualDest)) {
                logger.warning("may have done an incomplete copy of " + srcFile
                               + " to " + actualDest);
            }
            return null;
        }
        // If we get here, the file is gone, but it was not moved to the
        // location we asked for.  Hopefully, this is impossible, but I'm
        // not 100% sure that it 100% is.
        if (Files.exists(destFile)) {
            // The destination didn't exist before, and now it does.
            // Seems like success?
            logger.warning(srcFile.toString() + " is gone and " + destFile
                           + " exists, so rename seemed successful");
            logger.warning("Nevertheless, something went wrong.");
            return null;
        }
        if (actualDest == null) {
            // Again, likely completely impossible.  No idea what it would mean.
            logger.warning("Panic!  No idea what happened to " + srcFile);
            return null;
        }
        if (Files.exists(actualDest)) {
            // This indicates the srcFile was moved to a location that is not
            // the same file as the requested destFile.
            // Maybe this happens if destFile was actually a directory?
            logger.warning("somehow moved file to different destination: " + actualDest);
        }
        logger.info("craziest possible outcome");
        logger.info("src file gone, dest file not there, result of move call "
                    + "not null, but also not there.  Fubar.");
        return actualDest;
    }

    /**
     * Rename the given file to the given destination.  If the file is renamed,
     * returns the new Path.  If the file could not be renamed, returns null.
     *
     * <p>The destination must be a non-existent path, intended to be the file
     * that the srcFile is renamed to.  This method does not support the use
     * case where you tell us what directory you want the file moved into; you
     * are expected to provide the path, explicitly including the destination
     * file name (even if it's identical to the source file name).
     *
     * <p>The purpose of this method, as opposed to just calling Files.move()
     * directly, is to detect specific problems and communicate them via
     * logging.  It is a wrapper around Files.move().
     *
     * <p>If an unexpected result occurs, calls {@link #unexpectedMoveResult}.
     *
     * @param srcFile
     *    the file to be renamed
     * @param destFile
     *    the destination for the file to be renamed to; should not exist
     *    (either as a file or a directory)
     * @return
     *    the new destination if the file was renamed; null if it was not
     */
    public static Path renameFile(final Path srcFile, final Path destFile) {
        if (Files.notExists(srcFile)) {
            logger.warning("cannot rename file, does not exist: " + srcFile);
            return null;
        }
        if (Files.exists(destFile)) {
            if (Files.isDirectory(destFile)) {
                logger.warning("renameFile does not take a directory; "
                               + "supply the entire path");
            } else {
                logger.warning("will not overwrite existing file: " + destFile);
            }
            return null;
        }
        Path actualDest = null;
        try {
            actualDest = Files.move(srcFile, destFile);
            if (Files.isSameFile(destFile, actualDest)) {
                return destFile;
            }
        } catch (AccessDeniedException ade) {
            logger.warning("Could not rename file \"" + srcFile
                           + "\"; access denied");
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error renaming file " + srcFile, ioe);
        }
        // If we got here, things did not go as expected.  Try to make sense
        // of the state of the world.
        if (Files.exists(srcFile) && Files.notExists(destFile)) {
            // Looks like we did nothing.
            return null;
        }
        // If we get here, something really weird happened.  Handle it in
        // a separate method.
        return unexpectedMoveResult(srcFile, destFile, actualDest);
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
     * Return true if the given arguments refer to the same actual, existing file on
     * the file system.  On file systems that support symbolic links, two Paths could
     * be the same file even if their locations appear completely different.
     *
     * @param path1
     *    first Path to compare; it is expected that this Path exists
     * @param path2
     *    second Path to compare; this may or may not exist
     * @return
     *    true if the paths refer to the same file, false if they don't;
     *    logs an exception if one occurs while trying to check, including
     *    if path1 does not exist; but does not log one if path2 doesn't
     */
    public static boolean isSameFile(final Path path1, final Path path2) {
        try {
            //noinspection SimplifiableIfStatement
            if (Files.notExists(path2)) {
                return false;
            }
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
     * Copies the source file to the destination, providing progress updates.
     *
     * <p>If the destination cannot be created or is a read-only file, the
     * method returns <code>false</code>.  Otherwise, the contents of the source
     * are copied to the destination, and <code>true</code> is returned.
     *
     * <p>TODO: the newly created file will not necessarily have the same
     * attributes as the original.  In some cases, like ownership, that might
     * actually be desirable (have the copy be owned by the user running the
     * program), and also might be impossible to change even if the user does
     * prefer to maintain the original owner.  But there may be other attributes
     * we should try to adopt.  What about writability?  And the other, somewhat
     * newer system-specific attributes: the ones accessible via "chattr" on
     * Linux, "chflags" on OS X?  What about NTFS file streams, and ACLs?  A
     * file copy created just copying the content into a brand new file can
     * behave significantly differently from the original.
     *
     * @param source
     *            The source file to move.
     * @param dest
     *            The destination where to move the file.
     * @param observer
     *            The observer to notify, if any.  May be null.
     * @return true on success, false otherwise.
     *
     * Based on a version originally implemented in jEdit 4.3pre9
     */
    public static boolean copyWithUpdates(final Path source, final Path dest,
                                          final MoveObserver observer)
    {
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
            String errMsg = "Error moving file " + source;
            if (ioe.getMessage() != null) {
                errMsg += ": " + ioe.getMessage();
            }
            logger.log(Level.WARNING, errMsg, ioe);
        }

        if (!ok) {
            logger.warning("failed to move " + source);
        }
        return ok;
    }

    /**
     * Given a Path, if the Path exists, returns it.  If not, but its parent
     * exists, returns that, etc.  That is, returns the closest ancestor
     * (including itself) that exists.
     *
     * @param checkPath the path to check for the closest existing ancestor
     * @return the longest path, from the root dir towards the given path,
     *   that exists
     */
    public static Path existingAncestor(final Path checkPath) {
        if (checkPath == null) {
            return null;
        }
        Path root = checkPath.getRoot();
        Path existent = checkPath;
        while (Files.notExists(existent)) {
            if (root.equals(existent)) {
                // Presumably, this can't happen, because it suggests
                // the root dir doesn't exist, which doesn't make sense.
                // But just to be sure to avoid an infinite iteration...
                return null;
            }
            existent = existent.getParent();
            if (existent == null) {
                return null;
            }
        }

        return existent;
    }

    /**
     * Returns whether or not a Path is a writable directory.  The argument may be null.
     *
     * @param path
     *    the path to check; may be null
     * @return true if path names a directory that is writable by the user running this
     *    process; false otherwise.
     */
    public static boolean isWritableDirectory(final Path path) {
        return ((path != null)
                && Files.isDirectory(path)
                && Files.isWritable(path));
    }

    /**
     * Takes a Path which is a directory that the user wants to write into.
     *
     * @param path
     *    the path that the caller will want to write into
     * @return true if the given Path is a writable directory, or if it
     *    presumably could be created as such; false otherwise.
     *
     *    As an example, if the value is /Users/me/Files/Videos/TV, and no such
     *    file exists, we just keep going up the tree until something exists.
     *    If we find /Users/me/Files exists, and it's a writable directory, then
     *    presumably we could create a "Videos" directory in it, and "TV" in that,
     *    thereby creating the directory.  But if /Users/me/Files is not a directory,
     *    or is not writable, then we know we cannot create the target, and so we
     *    return false.
     */
    public static boolean checkForCreatableDirectory(final Path path) {
        return isWritableDirectory(existingAncestor(path));
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
