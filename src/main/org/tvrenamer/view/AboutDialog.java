package org.tvrenamer.view;

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
import org.tvrenamer.model.util.Constants;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * The About Dialog box.
 */
public class AboutDialog extends Dialog {
    private static Logger logger = Logger.getLogger(AboutDialog.class.getName());

    private static final String TVRENAMER_REPOSITORY_URL = "http://tvrenamer.org/source";
    private static final String TVRENAMER_LICENSE_URL = "http://www.gnu.org/licenses/gpl-2.0.html";
    private static final String TVRENAMER_SUPPORT_EMAIL = "support@tvrenamer.org";
    public static final String TVRENAMER_PROJECT_URL = "http://tvrenamer.org";
    private static final String TVRENAMER_PROJECT_ISSUES_URL = TVRENAMER_PROJECT_URL + "/issues";
    private static Shell aboutShell;

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
     * Creates the dialog's contents.
     *
     */
    private void createContents() {
        GridLayout shellGridLayout = new GridLayout();
        shellGridLayout.numColumns = 2;
        shellGridLayout.marginRight = 15;
        shellGridLayout.marginBottom = 5;
        aboutShell.setLayout(shellGridLayout);

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
        applicationLabel.setText(Constants.APPLICATION_NAME);
        applicationLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label versionLabel = new Label(aboutShell, SWT.NONE);
        versionLabel.setFont(new Font(aboutShell.getDisplay(), getDefaultSystemFont().getName(), getDefaultSystemFont()
            .getHeight() + 2, SWT.BOLD));
        versionLabel.setText("Version: " + Constants.VERSION_NUMBER);
        versionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label descriptionLabel = new Label(aboutShell, SWT.NONE);
        descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        descriptionLabel.setText("TVRenamer is a Java GUI utility to rename TV episodes from TV listings");

        final Link licenseLink = new Link(aboutShell, SWT.NONE);
        licenseLink.setText("Licensed under the <a href=\"" + TVRENAMER_LICENSE_URL
            + "\">GNU General Public License v2</a>");
        licenseLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        licenseLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_LICENSE_URL);
            }
        });

        final Link projectPageLink = new Link(aboutShell, SWT.NONE);
        projectPageLink.setText("<a href=\"" + TVRENAMER_PROJECT_URL + "\">Project Page</a>");
        projectPageLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        projectPageLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_PROJECT_URL);
            }
        });

        final Link issuesLink = new Link(aboutShell, SWT.NONE);
        issuesLink.setText("<a href=\"" + TVRENAMER_PROJECT_ISSUES_URL + "\">Issue Tracker</a>");
        issuesLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        issuesLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_PROJECT_ISSUES_URL);
            }
        });

        final Link supportEmailLink = new Link(aboutShell, SWT.NONE);
        supportEmailLink.setText("<a href=\"mailto:" + TVRENAMER_SUPPORT_EMAIL + "\">Send support email</a>");
        supportEmailLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        supportEmailLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent arg0) {
                    Program.launch("mailto:" + TVRENAMER_SUPPORT_EMAIL);
                }
            });

        final Link sourceCodeLink = new Link(aboutShell, SWT.NONE);
        sourceCodeLink.setText("<a href=\"" + TVRENAMER_REPOSITORY_URL + "\">Source Code</a>");
        sourceCodeLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        sourceCodeLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_REPOSITORY_URL);
            }
        });

        Button updateCheckButton = new Button(aboutShell, SWT.PUSH);
        updateCheckButton.setText("Check for Updates...");
        GridData gridDataUpdateCheck = new GridData();
        gridDataUpdateCheck.widthHint = 160;
        gridDataUpdateCheck.horizontalAlignment = GridData.END;
        updateCheckButton.setLayoutData(gridDataUpdateCheck);

        updateCheckButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                Boolean updateAvailable = UpdateChecker.isUpdateAvailable();

                if (updateAvailable == null) {
                    // Don't need to do anything here as the error message has been displayed already
                } else if (updateAvailable) {
                    StringBuilder messageBuilder = new StringBuilder();
                    messageBuilder.append("There is a new version available!\n\n");
                    messageBuilder.append("You are currently running ");
                    messageBuilder.append(Constants.VERSION_NUMBER);
                    messageBuilder.append(", but there is an update available\n\n");
                    messageBuilder.append("Please visit ");
                    messageBuilder.append(TVRENAMER_PROJECT_URL);
                    messageBuilder.append(" to download the new version.");

                    logger.fine(messageBuilder.toString());
                    UIUtils.showMessageBox(SWTMessageBoxType.OK, "New Version Available!", messageBuilder.toString());
                } else {
                    StringBuilder messageBuilder = new StringBuilder();
                    messageBuilder.append("There is a no new version available\n\n");
                    messageBuilder.append("Please check the website (");
                    messageBuilder.append(TVRENAMER_PROJECT_URL);
                    messageBuilder.append(") for any news or check back later.");
                    UIUtils.showMessageBox(SWTMessageBoxType.WARNING, "No New Version Available",
                                           messageBuilder.toString());
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
}
