package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.controller.UrlLauncher;
import org.tvrenamer.model.util.Environment;

import java.awt.HeadlessException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public final class UIStarter {
    private static final Logger logger = Logger.getLogger(UIStarter.class.getName());

    final Shell shell;
    final Display display;
    private final Image appIcon;
    private final ResultsTable resultsTable;

    /*
     * Read an image.
     *
     * @param resourcePath
     *     the relative path to try to locate the file as a resource
     * @param filePath
     *     the path to try to locate the file directly in the file system
     * @return an Image read from the given path
     */
    private static Image readImageFromPath(final String resourcePath, final String filePath) {
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

    private void showMessageBox(final SWTMessageBoxType type, final String title,
                                final String message, final Exception exception)
    {
        if (shell.isDisposed()) {
            // Shell is gone, try using JOptionPane instead
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

    /**
     * Set the Shell's icon.<p>
     *
     * It seems that certain activities cause the icon to be "lost", and this method can
     * be called to re-establish it.
     */
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

    @SuppressWarnings("SameReturnValue")
    private int onException(Exception exception) {
        logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
        showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL, UNKNOWN_EXCEPTION, exception);
        shell.dispose();
        return 1;
    }

    void quit() {
        shell.dispose();
    }

    private void makeMenuItem(final Menu parent, final String text,
                              final Listener listener, final char shortcut)
    {
        MenuItem newItem = new MenuItem(parent, SWT.PUSH);
        newItem.setText(text + "\tCtrl+" + shortcut);
        newItem.addListener(SWT.Selection, listener);
        newItem.setAccelerator(SWT.CONTROL | shortcut);
    }

    private Menu setupHelpMenuBar(final Menu menuBar) {
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("Help");

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpHelpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpHelpItem.setText("Help");

        MenuItem helpVisitWebPageItem = new MenuItem(helpMenu, SWT.PUSH);
        helpVisitWebPageItem.setText("Visit Web Page");
        helpVisitWebPageItem.addSelectionListener(new UrlLauncher(TVRENAMER_PROJECT_URL));

        return helpMenu;
    }

    private void setupMenuBar() {
        Menu menuBarMenu = new Menu(shell, SWT.BAR);
        Menu helpMenu;

        Listener preferencesListener = e -> {
            PreferencesDialog preferencesDialog = new PreferencesDialog(shell);
            preferencesDialog.open();
        };
        Listener aboutListener = e -> {
            AboutDialog aboutDialog = new AboutDialog(this);
            aboutDialog.open();
        };
        Listener quitListener = e -> quit();

        if (Environment.IS_MAC_OSX) {
            // Add the special Mac OSX Preferences, About and Quit menus.
            CocoaUIEnhancer enhancer = new CocoaUIEnhancer();
            enhancer.hookApplicationMenu(display, quitListener, aboutListener, preferencesListener);

            setupHelpMenuBar(menuBarMenu);
        } else {
            // Add the normal Preferences, About and Quit menus.
            MenuItem fileMenuItem = new MenuItem(menuBarMenu, SWT.CASCADE);
            fileMenuItem.setText("File");

            Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
            fileMenuItem.setMenu(fileMenu);

            makeMenuItem(fileMenu, PREFERENCES_LABEL, preferencesListener, 'P');
            makeMenuItem(fileMenu, EXIT_LABEL, quitListener, 'Q');

            helpMenu = setupHelpMenuBar(menuBarMenu);

            // The About item is added to the OSX bar, so we need to add it manually here
            MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
            helpAboutItem.setText("About");
            helpAboutItem.addListener(SWT.Selection, aboutListener);
        }

        shell.setMenuBar(menuBarMenu);
    }

    /**
     * Start up the UI.
     *
     * The UIStarter class is the top level UI driver for the application.  Assuming we are
     * running in UI mode (the only way currently supported), creating a UIStarter should be
     * one of the very first things we do, should only be done once, and the instance should
     * live until the program is being shut down.
     *
     * The UIStarter automatically creates a {@link ResultsTable}, which is the main class
     * that drives all the application-specific action.  This class sets up generic stuff,
     * like the Display, the Shell, the icon, etc.
     *
     */
    public UIStarter() {
        // Setup display and shell
        Display.setAppName(APPLICATION_NAME);
        display = new Display();
        shell = new Shell(display);

        shell.setText(APPLICATION_NAME);

        appIcon = readImageFromPath(APPLICATION_ICON_PATH);
        setAppIcon();

        GridLayout shellGridLayout = new GridLayout(3, false);
        shell.setLayout(shellGridLayout);

        // Create the main window
        resultsTable = new ResultsTable(this);
        setupMenuBar();
    }

    /**
     * Run the UI/event loop.
     *
     * @return 0 on normal exit, nonzero on error
     */
    public int run() {
        try {
            shell.pack(true);
            positionWindow();

            // Start the shell
            shell.pack();
            shell.open();

            resultsTable.ready();

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            display.dispose();
            return 0;
        } catch (Exception exception) {
            return onException(exception);
        }
    }
}
