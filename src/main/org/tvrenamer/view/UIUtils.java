package org.tvrenamer.view;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.util.Constants.OSType;

import java.awt.HeadlessException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class UIUtils {

    private static Logger logger = Logger.getLogger(UIUtils.class.getName());
    private static Shell shell;

    /**
     * Constructor.
     *
     * @param shell
     *            the shell to use.
     */
    public UIUtils(Shell shell) {
        UIUtils.shell = shell;
    }

    /**
     * Determine the system default font
     *
     * @return the system default font
     */
    public static FontData getDefaultSystemFont() {
        FontData defaultFont = null;
        try {
            defaultFont = shell.getDisplay().getSystemFont().getFontData()[0];
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error attempting to determine system default font", e);
        }

        return defaultFont;
    }

    public static void showMessageBox(final SWTMessageBoxType type, final String title, final String message, final Exception exception) {
        if (shell == null) {
            // Shell not established yet, try using JOPtionPane instead
            try {
                JOptionPane.showMessageDialog(null, message);
                return;
            } catch (HeadlessException he) {
                logger.warning("Could not show message graphically: " + message);
                return;
            }
        }

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                MessageBox msgBox = new MessageBox(shell, type.swtIconValue);
                msgBox.setText(title);

                if (exception == null) {
                    msgBox.setMessage(message);
                } else {
                    msgBox.setMessage(message + "/n" + exception.getLocalizedMessage());
                }

                msgBox.open();
            }
        });
    }

    /**
     * Show a message box of the given type with the given message content and window title.
     *
     * @param type the {@link SWTMessageBoxType} to create
     * @param title the window title
     * @param message the message content
     */
    public static void showMessageBox(final SWTMessageBoxType type, final String title, final String message) {
        showMessageBox(type, title, message, null);
    }

    public static void handleNoConnection(Exception exception) {
        String message = "Unable connect to the TV listing website, please check your internet connection.  "
            + "\nNote that proxies are not currently supported.";
        logger.log(Level.WARNING, message, exception);
        showMessageBox(SWTMessageBoxType.ERROR, "Error", message);
    }

    public static OSType getOSType() {
        if (System.getProperty("os.name").contains("Mac")) {
            return OSType.MAC;
        }
        if (System.getProperty("os.name").contains("Windows")) {
            return OSType.WINDOWS;
        }
        return OSType.LINUX;
    }
}
