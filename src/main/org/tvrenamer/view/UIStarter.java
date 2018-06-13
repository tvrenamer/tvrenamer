package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;

import java.awt.HeadlessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public final class UIStarter implements Observer {
    private static final Logger logger = Logger.getLogger(UIStarter.class.getName());

    final Shell shell;
    final Display display;
    final Image appIcon;
    final ResultsTable resultsTable;
    final UserPreferences prefs = UserPreferences.getInstance();

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
        try (InputStream in = UIStarter.class.getResourceAsStream(resourcePath)) {
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

    /**
     * Determine the system default font
     *
     * @return the system default font
     */
    public FontData getDefaultSystemFont() {
        FontData defaultFont = null;
        try {
            defaultFont = display.getSystemFont().getFontData()[0];
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error attempting to determine system default font", e);
        }

        return defaultFont;
    }

    public void showMessageBox(final SWTMessageBoxType type, final String title,
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

        display.syncExec(() -> {
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
    public void showMessageBox(final SWTMessageBoxType type,
                               final String title, final String message)
    {
        showMessageBox(type, title, message, null);
    }

    public void setAppIcon() {
        if (appIcon == null) {
            logger.warning("unable to get application icon");
        } else {
            shell.setImage(appIcon);
        }
    }

    private void positionWindow() {
        // place the window near the lower right-hand corner
        Monitor primary = display.getPrimaryMonitor();
        Rectangle bounds = primary.getBounds();
        Rectangle rect = shell.getBounds();
        int x = bounds.x + (bounds.width - rect.width) - 5;
        int y = bounds.y + (bounds.height - rect.height) - 35;
        shell.setLocation(x, y);
    }

    public UIStarter() {
        // Setup display and shell
        Display.setAppName(APPLICATION_NAME);
        display = new Display();
        shell = new Shell(display);

        shell.setText(APPLICATION_NAME);

        appIcon = readImageFromPath(TVRENAMER_ICON_PATH);
        setAppIcon();

        GridLayout shellGridLayout = new GridLayout(3, false);
        shell.setLayout(shellGridLayout);

        // Create the main window
        resultsTable = new ResultsTable(this);
    }

    private void checkDestinationDirectory() {
        boolean success = prefs.ensureDestDir();
        if (!success) {
            logger.warning(CANT_CREATE_DEST);
            showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL, CANT_CREATE_DEST + ": '"
                           + prefs.getDestinationDirectoryName() + "'. " + MOVE_NOW_DISABLED);
        }
    }

    @Override
    public void update(final Observable observable, final Object value) {
        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            final UserPreference userPref = (UserPreference) value;
            if ((userPref == UserPreference.DEST_DIR)
                || (userPref == UserPreference.MOVE_ENABLED))
            {
                checkDestinationDirectory();
            }
        }
    }

    public int run() {
        try {
            shell.pack(true);
            positionWindow();

            // Start the shell
            shell.pack();
            shell.open();

            checkDestinationDirectory();
            prefs.addObserver(this);

            resultsTable.ready();

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            display.dispose();
            return 0;
        } catch (Exception exception) {
            showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL, UNKNOWN_EXCEPTION, exception);
            logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
            shell.dispose();
            return 1;
        }
    }
}
