package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.UIUtils.showMessageBox;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.model.SWTMessageBoxType;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class UIStarter {
    private static final Logger logger = Logger.getLogger(UIStarter.class.getName());

    final Shell shell;
    final Display display;
    final ResultsTable resultsTable;

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

        GridLayout shellGridLayout = new GridLayout(3, false);
        shell.setLayout(shellGridLayout);

        // Setup the util class
        UIUtils.setShell(shell);

        // Create the main window
        resultsTable = new ResultsTable(this);
    }

    public int run() {
        try {
            UIUtils.checkDestinationDirectory();

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
            showMessageBox(SWTMessageBoxType.ERROR, ERROR_LABEL, UNKNOWN_EXCEPTION, exception);
            logger.log(Level.SEVERE, UNKNOWN_EXCEPTION, exception);
            shell.dispose();
            return 1;
        }
    }
}
