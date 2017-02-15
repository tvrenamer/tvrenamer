package org.tvrenamer.controller.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.gjt.sp.util.IOUtilities;
import org.gjt.sp.util.Log;
import org.gjt.sp.util.ProgressObserver;

/**
 *
 * @author Vipul Delwadia
 * @since 2010/09/14
 *
 */
public class FileUtilities {
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

    // {{{ copyFile() method, based on the moveFile() method above
    /**
     * Copies the source file to the destination.
     *
     * If the destination cannot be created or is a read-only file, the method returns <code>false</code>. Otherwise,
     * the contents of the source are copied to the destination and <code>true</code> is returned.
     *
     * @param source
     *            The source file to copy.
     * @param dest
     *            The destination where to copy the file.
     * @param observer
     *            The observer to notify (can be null).
     * @param canStop
     *            if true, the copy can be stopped by interrupting the thread
     * @return true on success, false otherwise.
     *
     * @since jEdit 4.3pre9
     */
    public static boolean copyFile(File source, File dest, ProgressObserver observer, boolean canStop) {
        return doAction(source, dest, observer, canStop, false);
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

            if (ok && deleteOnSuccess)
                source.delete();
        }
        return ok;
    }
}
