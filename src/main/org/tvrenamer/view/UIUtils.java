package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.UserPreferences;

import java.awt.HeadlessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

class UIUtils {
    private static final Logger logger = Logger.getLogger(UIUtils.class.getName());
    private static final UserPreferences prefs = UserPreferences.getInstance();

    private static Shell shell = null;

    private UIUtils() {
        // utility class; prevent instantiation
    }

    /**
     * Give this class a pointer to the UI's shell.
     *
     * @param shell
     *            the shell to use.
     */
    public static void setShell(Shell shell) {
        UIUtils.shell = shell;
    }

    public static void showMessageBox(final SWTMessageBoxType type, final String title,
                                      final String message, final Exception exception)
    {
        if (shell == null) {
            // Shell not established yet, try using JOptionPane instead
            try {
                JOptionPane.showMessageDialog(null, message);
                return;
            } catch (HeadlessException he) {
                logger.warning("Could not show message graphically: " + message);
                return;
            }
        }

        Display.getDefault().syncExec(() -> {
            MessageBox msgBox = new MessageBox(shell, type.getSwtIconValue());
            msgBox.setText(title);

            if (exception == null) {
                msgBox.setMessage(message);
            } else {
                msgBox.setMessage(message + "\n" + exception.getLocalizedMessage());
            }

            msgBox.open();
        });
    }

    /**
     * Show a message box of the given type with the given message content and window title.
     *
     * @param type the {@link SWTMessageBoxType} to create
     * @param title the window title
     * @param message the message content
     */
    public static void showMessageBox(final SWTMessageBoxType type,
                                      final String title, final String message)
    {
        showMessageBox(type, title, message, null);
    }

    /**
     * Read an image.
     *
     * @param resourcePath
     *     the relative path to try to locate the file as a resource
     * @param filePath
     *     the path to try to locate the file directly in the file system
     * @return an Image read from the given path
     */
    public static Image readImageFromPath(final String resourcePath,
                                          final String filePath)
    {
        Display display = Display.getCurrent();
        Image rval = null;
        try (InputStream in = UIUtils.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                rval = new Image(display, in);
            }
        } catch (IOException ioe) {
            logger.warning("exception trying to read image from stream " + resourcePath);
        }
        if (rval == null) {
            rval = new Image(display, filePath);
        }
        return rval;
    }

    /**
     * Read an image.
     *
     * @param resourcePath
     *     the relative path to try to locate the file as a resource
     * @return an Image read from the given path
     */
    public static Image readImageFromPath(final String resourcePath) {
        return readImageFromPath(resourcePath, ICON_PARENT_DIRECTORY + "/" + resourcePath);
    }

    public static void checkDestinationDirectory() {
        boolean success = prefs.ensureDestDir();
        if (!success) {
            logger.warning(CANT_CREATE_DEST);
            showMessageBox(SWTMessageBoxType.ERROR, ERROR_LABEL, CANT_CREATE_DEST + ": '"
                           + prefs.getDestinationDirectoryName() + "'. " + MOVE_NOW_DISABLED);
        }
    }
}
