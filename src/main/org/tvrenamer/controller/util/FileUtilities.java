package org.tvrenamer.controller.util;

import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
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
     * @param canStop
     *            if true, the move can be stopped by interrupting the thread
     * @return true on success, false otherwise.
     *
     * @since jEdit 4.3pre9
     */
    public static boolean moveFile(File source, File dest, ProgressObserver observer, boolean canStop) {
        return doAction(source, dest, observer, canStop, true);
    } // }}}

    private static boolean doAction(File source, File dest, ProgressObserver observer, boolean canStop,
        boolean deleteOnSuccess)
    {
        boolean ok = false;

        if ((dest.exists() && dest.canWrite()) || (!dest.exists() && dest.getParentFile().canWrite())) {
            OutputStream fos = null;
            InputStream fis = null;
            try {
                fos = new FileOutputStream(dest);
                fis = new FileInputStream(source);
                ok = IOUtilities.copyStream(32768, observer, fis, fos, canStop);
            } catch (IOException ioe) {
                Log.log(Log.WARNING, IOUtilities.class, "Error moving file: " + ioe + " : " + ioe.getMessage());
            } finally {
                IOUtilities.closeQuietly(fos);
                IOUtilities.closeQuietly(fis);
            }

            if (ok && deleteOnSuccess) {
                source.delete();
            }
        }
        return ok;
    }

    public static boolean areSameDisk(File fileA, File fileB) {
        String pathA = fileA.getAbsolutePath();
        String pathB = fileB.getAbsolutePath();
        File[] roots = File.listRoots();
        if (roots.length < 2) {
            return true;
        }
        for (File root : roots) {
            String rootPath = root.getAbsolutePath();
            if (pathA.startsWith(rootPath)) {
                if (pathB.startsWith(rootPath)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
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
