package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.UIUtils.getDefaultSystemFont;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

import org.tvrenamer.controller.UpdateChecker;
import org.tvrenamer.model.SWTMessageBoxType;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * The About Dialog box.
 */
final class AboutDialog extends Dialog {
    private static final Logger logger = Logger.getLogger(AboutDialog.class.getName());

    private static final String TVRENAMER_LICENSE_URL = "http://www.gnu.org/licenses/gpl-2.0.html";

    private Shell aboutShell;

    /**
     * AboutDialog constructor
     *
     * @param parent
     *            the parent {@link Shell}
     */
    public AboutDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    public void open() {
        // Create the dialog window
        aboutShell = new Shell(getParent(), getStyle());
        aboutShell.setText("About TVRenamer");

        // Add the contents of the dialog window
        createContents();

        aboutShell.pack();
        aboutShell.open();
        Display display = getParent().getDisplay();
        while (!aboutShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    /**
     * Creates the grid layout
     *
     */
    private void createGridLayout() {
        GridLayout shellGridLayout = new GridLayout();
        shellGridLayout.numColumns = 2;
        shellGridLayout.marginRight = 15;
        shellGridLayout.marginBottom = 5;
        aboutShell.setLayout(shellGridLayout);
    }

    /**
     * Creates the labels
     *
     */
    private void createLabels() {
        Label iconLabel = new Label(aboutShell, SWT.NONE);
        GridData iconGridData = new GridData();
        iconGridData.verticalAlignment = GridData.FILL;
        iconGridData.horizontalAlignment = GridData.FILL;
        // Force the icon to take up the whole of the right column
        iconGridData.verticalSpan = 10;
        iconGridData.grabExcessVerticalSpace = false;
        iconGridData.grabExcessHorizontalSpace = false;
        iconLabel.setLayoutData(iconGridData);

        InputStream icon = getClass().getResourceAsStream("/icons/tvrenamer.png");
        if (icon != null) {
            iconLabel.setImage(new Image(Display.getCurrent(), icon));
        } else {
            iconLabel.setImage(new Image(Display.getCurrent(), "res/icons/tvrenamer.png"));
        }

        Label applicationLabel = new Label(aboutShell, SWT.NONE);
        applicationLabel.setFont(new Font(aboutShell.getDisplay(), getDefaultSystemFont().getName(),
            getDefaultSystemFont().getHeight() + 4, SWT.BOLD));
        applicationLabel.setText(APPLICATION_NAME);
        applicationLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label versionLabel = new Label(aboutShell, SWT.NONE);
        versionLabel.setFont(new Font(aboutShell.getDisplay(),
                                      getDefaultSystemFont().getName(),
                                      getDefaultSystemFont().getHeight() + 2,
                                      SWT.BOLD));

        versionLabel.setText("Version: " + VERSION_NUMBER);
        versionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label descriptionLabel = new Label(aboutShell, SWT.NONE);
        descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        descriptionLabel.setText("TVRenamer is a Java GUI utility to rename TV episodes from TV listings");
    }

    /**
     * Utility method for creating a URL link.
     *
     * SWT allows very generic links, that could do any arbitrary action when clicked,
     * but we just one basic ones that have a URL and open it when clicked.
     */
    private void createUrlLink(String intro, String url, String label) {
        final Link link = new Link(aboutShell, SWT.NONE);
        link.setText(intro + "<a href=\"" + url + "\">" + label + "</a>");
        link.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    Program.launch(url);
                }
            });
    }

    /**
     * Creates the links
     *
     */
    private void createLinks() {
        createUrlLink("Licensed under the ", TVRENAMER_LICENSE_URL, "GNU General Public License v2");
        createUrlLink("", TVRENAMER_PROJECT_URL, "Project Page");
        createUrlLink("", TVRENAMER_PROJECT_ISSUES_URL, "Issue Tracker");
        createUrlLink("", "mailto:" + TVRENAMER_SUPPORT_EMAIL, "Send support email");
        createUrlLink("", TVRENAMER_REPOSITORY_URL, "Source Code");
    }

    /**
     * Creates the buttons
     *
     */
    private void createButtons() {
        Button updateCheckButton = new Button(aboutShell, SWT.PUSH);
        updateCheckButton.setText("Check for Updates...");
        GridData gridDataUpdateCheck = new GridData();
        gridDataUpdateCheck.widthHint = 160;
        gridDataUpdateCheck.horizontalAlignment = GridData.END;
        updateCheckButton.setLayoutData(gridDataUpdateCheck);

        updateCheckButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean updateAvailable = UpdateChecker.isUpdateAvailable();

                if (updateAvailable) {
                    String message = "There is a new version available!\n\n"
                        + "You are currently running "
                        + VERSION_NUMBER
                        + ", but there is an update available\n\n"
                        + "Please visit "
                        + TVRENAMER_PROJECT_URL
                        + " to download the new version.";

                    logger.fine(message);
                    UIUtils.showMessageBox(SWTMessageBoxType.OK, "New Version Available!", message);
                } else {
                    String message = "There is a no new version available\n\n"
                        + "Please check the website ("
                        + TVRENAMER_PROJECT_URL
                        + ") for any news or check back later.";
                    UIUtils.showMessageBox(SWTMessageBoxType.WARNING, "No New Version Available",
                                           message);
                }
            }
        });

        Button okButton = new Button(aboutShell, SWT.PUSH);
        okButton.setText("OK");
        GridData gridDataOK = new GridData();
        gridDataOK.widthHint = 160;
        gridDataOK.horizontalAlignment = GridData.END;
        okButton.setLayoutData(gridDataOK);
        okButton.setFocus();

        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                aboutShell.close();
            }
        });

        // Set the OK button as the default, so
        // user can press Enter to dismiss
        aboutShell.setDefaultButton(okButton);
    }

    /**
     * Creates the dialog's contents.
     *
     */
    private void createContents() {
        createGridLayout();
        createLabels();
        createLinks();
        createButtons();
    }
}
