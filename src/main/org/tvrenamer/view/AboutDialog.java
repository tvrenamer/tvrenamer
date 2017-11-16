package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.UIUtils.getDefaultSystemFont;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
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

import java.util.logging.Logger;

/**
 * The About Dialog box.
 */
final class AboutDialog extends Dialog {
    private static final Logger logger = Logger.getLogger(AboutDialog.class.getName());

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
        aboutShell.setText(ABOUT_LABEL);

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
        iconLabel.setImage(UIUtils.readImageFromPath(TVRENAMER_ICON_PATH, TVRENAMER_ICON_DIRECT_PATH));

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

        versionLabel.setText(VERSION_LABEL);
        versionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));

        Label descriptionLabel = new Label(aboutShell, SWT.NONE);
        descriptionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true));
        descriptionLabel.setText(TVRENAMER_DESCRIPTION);
    }

    /**
     * Utility method for creating a URL link.
     *
     * SWT allows very generic links, that could do any arbitrary action when clicked,
     * but we just one basic ones that have a URL and open it when clicked.
     *
     * @param intro
     *          text to place before the link; can be empty, but not null
     * @param url
     *          the URL to link to
     * @param label
     *          the text to use for the link
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
        createUrlLink(LICENSE_TEXT_1, TVRENAMER_LICENSE_URL, LICENSE_TEXT_2);
        createUrlLink("", TVRENAMER_PROJECT_URL, PROJECT_PAGE);
        createUrlLink("", TVRENAMER_ISSUES_URL, ISSUE_TRACKER);
        createUrlLink("", EMAIL_LINK, SEND_SUPPORT_EMAIL);
        createUrlLink("", TVRENAMER_REPOSITORY_URL, SOURCE_CODE_LINK);
    }

    /**
     * Creates the buttons
     *
     */
    private void createButtons() {
        Button updateCheckButton = new Button(aboutShell, SWT.PUSH);
        updateCheckButton.setText(UPDATE_TEXT);
        GridData gridDataUpdateCheck = new GridData();
        gridDataUpdateCheck.widthHint = 160;
        gridDataUpdateCheck.horizontalAlignment = GridData.END;
        updateCheckButton.setLayoutData(gridDataUpdateCheck);

        updateCheckButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                boolean updateAvailable = UpdateChecker.isUpdateAvailable();

                if (updateAvailable) {
                    logger.fine(NEW_VERSION_AVAILABLE);
                    UIUtils.showMessageBox(SWTMessageBoxType.OK, NEW_VERSION_TITLE,
                                           NEW_VERSION_AVAILABLE);
                } else {
                    UIUtils.showMessageBox(SWTMessageBoxType.WARNING, NO_NEW_VERSION_TITLE,
                                           NO_NEW_VERSION_AVAILABLE);
                }
            }
        });

        Button okButton = new Button(aboutShell, SWT.PUSH);
        okButton.setText(OK_LABEL);
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
