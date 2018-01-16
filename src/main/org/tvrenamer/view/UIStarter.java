package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.UIUtils.showMessageBox;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.UserPreferences;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UIStarter {
    private static final Logger logger = Logger.getLogger(UIStarter.class.getName());

    Shell shell;
    Display display;

    private ResultsTable resultsTable;

    private void init() {
        // Setup display and shell
        GridLayout shellGridLayout = new GridLayout(3, false);
        Display.setAppName(APPLICATION_NAME);
        display = new Display();

        shell = new Shell(display);
        shell.setText(APPLICATION_NAME);
        shell.setLayout(shellGridLayout);

        // Setup the util class
        UIUtils.setShell(shell);
        UIUtils.checkDestinationDirectory(UserPreferences.getInstance());

        // Create the main window
        resultsTable = new ResultsTable(this);

        setupIcons();

        shell.pack(true);
    }

    void uiCleanup() {
        shell.dispose();
        display.dispose();
    }

    private void setupIcons() {
        try {
            InputStream icon = getClass().getResourceAsStream(TVRENAMER_ICON_PATH);
            if (icon != null) {
                shell.setImage(new Image(display, icon));
            } else {
                shell.setImage(new Image(display, TVRENAMER_ICON_DIRECT_PATH));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int launch() {
        try {
            // place the window in the centre of the primary monitor
            Monitor primary = display.getPrimaryMonitor();
            Rectangle bounds = primary.getBounds();
            Rectangle rect = shell.getBounds();
            int x = bounds.x + (bounds.width - rect.width) - 5;
            int y = bounds.y + (bounds.height - rect.height) - 35;
            shell.setLocation(x, y);

            // Start the shell
            shell.pack();
            shell.open();
            resultsTable.ready();

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            return 0;
        } catch (Exception exception) {
            showMessageBox(SWTMessageBoxType.ERROR, ERROR_LABEL, UNKNOWN_EXCEPTION, exception);
            logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
            return 1;
        }
    }

    public int run() {
        init();
        int rval = launch();
        uiCleanup();
        return rval;
    }
}
