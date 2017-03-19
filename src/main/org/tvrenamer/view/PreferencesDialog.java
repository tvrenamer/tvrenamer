package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.tvrenamer.model.ProxySettings;
import org.tvrenamer.model.ReplacementToken;
import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.TVRenamerIOException;
import org.tvrenamer.model.UserPreferences;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PreferencesDialog extends Dialog {

    private static Logger logger = Logger.getLogger(PreferencesDialog.class.getName());
    private static Shell preferencesShell;
    private static int DND_OPERATIONS = DND.DROP_MOVE;
    private TabFolder tabFolder;

    private final UserPreferences prefs;

    // The controls to save
    private Button moveEnabledCheckbox;
    private Button renameEnabledCheckbox;
    private Text destDirText;
    private Text seasonPrefixText;
    private Button seasonPrefixLeadingZeroCheckbox;
    private Text replacementStringText;
    private Text ignoreWordsText;
    private Button proxyEnabledCheckbox;
    private Text proxyHostText;
    private Text proxyPortText;
    private Button proxyAuthenticationRequiredCheckbox;
    private Text proxyUsernameText;
    private Text proxyPasswordText;
    private Button checkForUpdatesCheckbox;
    private Button recurseFoldersCheckbox;

    /**
     * PreferencesDialog constructor
     *
     * @param parent
     *            the parent {@link Shell}
     */
    public PreferencesDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        this.prefs = UserPreferences.getInstance();
    }

    public void open() {
        // Create the dialog window
        preferencesShell = new Shell(getParent(), getStyle());
        preferencesShell.setText(PREFERENCES_LABEL);

        // Add the contents of the dialog window
        createContents();

        preferencesShell.pack();
        preferencesShell.open();
        Display display = getParent().getDisplay();
        while (!preferencesShell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

    private void createContents() {
        GridLayout shellGridLayout = new GridLayout(3, false);
        preferencesShell.setLayout(shellGridLayout);

        Label helpLabel = new Label(preferencesShell, SWT.NONE);
        helpLabel.setText(HELP_TOOLTIP);
        helpLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true, shellGridLayout.numColumns, 1));

        tabFolder = new TabFolder(preferencesShell, getStyle());
        tabFolder.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true, shellGridLayout.numColumns, 1));

        createGeneralTab();
        createRenameTab();
        createProxyTab();

        createActionButtonGroup();
    }


    private void createGeneralTab() {
        TabItem item = new TabItem(tabFolder, SWT.NULL);
        item.setText(GENERAL_LABEL);

        Composite generalGroup= new Composite(tabFolder, SWT.NONE);
        generalGroup.setLayout(new GridLayout(3, false));
        generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
        generalGroup.setToolTipText(GENERAL_TOOLTIP);

        moveEnabledCheckbox = new Button(generalGroup, SWT.CHECK);
        moveEnabledCheckbox.setText(MOVE_ENABLED_TEXT);
        moveEnabledCheckbox.setSelection(prefs.isMoveEnabled());
        moveEnabledCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));
        moveEnabledCheckbox.setToolTipText(MOVE_ENABLED_TOOLTIP);

        renameEnabledCheckbox = new Button(generalGroup, SWT.CHECK);
        renameEnabledCheckbox.setText("Rename Enabled [?]");
        renameEnabledCheckbox.setSelection(prefs.isRenameEnabled());
        renameEnabledCheckbox.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, true, 1, 1));
        renameEnabledCheckbox.setToolTipText("Whether the 'rename' functionality is enabled.\n"
                                             + "You can move a file into a folder based on its show\n"
                                             + "without actually renaming the file");

        Label destDirLabel = new Label(generalGroup, SWT.NONE);
        destDirLabel.setText(DEST_DIR_TEXT);
        destDirLabel.setToolTipText(DEST_DIR_TOOLTIP);

        destDirText = new Text(generalGroup, SWT.BORDER);
        destDirText.setText(prefs.getDestinationDirectoryName());
        destDirText.setTextLimit(99);
        destDirText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true));

        final Button destDirButton = new Button(generalGroup, SWT.PUSH);
        destDirButton.setText(DEST_DIR_BUTTON_TEXT);
        destDirButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                DirectoryDialog directoryDialog = new DirectoryDialog(preferencesShell);

                directoryDialog.setFilterPath(prefs.getDestinationDirectoryName());
                directoryDialog.setText(DIR_DIALOG_TEXT);

                String dir = directoryDialog.open();
                if (dir != null) {
                    destDirText.setText(dir);
                }
            }
        });

        Label seasonPrefixLabel = new Label(generalGroup, SWT.NONE);
        seasonPrefixLabel.setText(SEASON_PREFIX_TEXT);
        seasonPrefixLabel.setToolTipText(PREFIX_TOOLTIP);

        seasonPrefixText = new Text(generalGroup, SWT.BORDER);
        seasonPrefixText.setText(prefs.getSeasonPrefixForDisplay());
        seasonPrefixText.setTextLimit(99);
        seasonPrefixText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

        seasonPrefixLeadingZeroCheckbox = new Button(generalGroup, SWT.CHECK);
        seasonPrefixLeadingZeroCheckbox.setText(SEASON_PREFIX_ZERO_TEXT);
        seasonPrefixLeadingZeroCheckbox.setSelection(prefs.isSeasonPrefixLeadingZero());
        seasonPrefixLeadingZeroCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
                                                                   true, true, 3, 1));
        seasonPrefixLeadingZeroCheckbox.setToolTipText(SEASON_PREFIX_ZERO_TOOLTIP);

        destDirText.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.F1) {
                    System.out.println("F1HelpRequested");
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // no-op
            }
        });

        destDirText.addHelpListener(new HelpListener() {

            @Override
            public void helpRequested(HelpEvent e) {
                System.out.println("helpRequested");
            }
        });

        toggleEnableControls(moveEnabledCheckbox, destDirText, destDirButton, seasonPrefixText);

        moveEnabledCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                toggleEnableControls(moveEnabledCheckbox, destDirText, destDirButton, seasonPrefixText);
            }
        });

        Label ignoreLabel = new Label(generalGroup, SWT.NONE);
        ignoreLabel.setText(IGNORE_LABEL_TEXT);
        ignoreLabel.setToolTipText(IGNORE_LABEL_TOOLTIP);

        ignoreWordsText = new Text(generalGroup, SWT.BORDER);
        java.util.List<String> ignoreList = prefs.getIgnoreKeywords();
        String ignoreWords = "";
        for (String s : ignoreList) {
            ignoreWords += s;
            ignoreWords += ",";
        }
        ignoreWordsText.setText(ignoreWords);
        ignoreWordsText.setTextLimit(99);
        ignoreWordsText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true));

        recurseFoldersCheckbox = new Button(generalGroup, SWT.CHECK);
        recurseFoldersCheckbox.setText(RECURSE_FOLDERS_TEXT);
        recurseFoldersCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
        recurseFoldersCheckbox.setSelection(prefs.isRecursivelyAddFolders());

        checkForUpdatesCheckbox = new Button(generalGroup, SWT.CHECK);
        checkForUpdatesCheckbox.setText(CHECK_UPDATES_TEXT);
        checkForUpdatesCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
        checkForUpdatesCheckbox.setSelection(prefs.checkForUpdates());

        item.setControl(generalGroup);
    }

    private void createRenameTab() {
        TabItem item = new TabItem(tabFolder, SWT.NULL);
        item.setText(RENAMING_LABEL);

        Composite replacementGroup = new Composite(tabFolder, SWT.NONE);
        replacementGroup.setLayout(new GridLayout(3, false));
        replacementGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));

        Label renameTokensLabel = new Label(replacementGroup, SWT.NONE);
        renameTokensLabel.setText(RENAME_TOKEN_TEXT);
        renameTokensLabel.setToolTipText(RENAME_TOKEN_TOOLTIP);

        List renameTokensList = new List(replacementGroup, SWT.SINGLE);
        renameTokensList.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));
        renameTokensList.add(ReplacementToken.SHOW_NAME.toString());
        renameTokensList.add(ReplacementToken.SEASON_NUM.toString());
        renameTokensList.add(ReplacementToken.SEASON_NUM_LEADING_ZERO.toString());
        renameTokensList.add(ReplacementToken.EPISODE_NUM.toString());
        renameTokensList.add(ReplacementToken.EPISODE_NUM_LEADING_ZERO.toString());
        renameTokensList.add(ReplacementToken.EPISODE_TITLE.toString());
        renameTokensList.add(ReplacementToken.EPISODE_TITLE_NO_SPACES.toString());
        renameTokensList.add(ReplacementToken.EPISODE_RESOLUTION.toString());
        renameTokensList.add(ReplacementToken.DATE_DAY_NUM.toString());
        renameTokensList.add(ReplacementToken.DATE_DAY_NUMLZ.toString());
        renameTokensList.add(ReplacementToken.DATE_MONTH_NUM.toString());
        renameTokensList.add(ReplacementToken.DATE_MONTH_NUMLZ.toString());
        renameTokensList.add(ReplacementToken.DATE_YEAR_MIN.toString());
        renameTokensList.add(ReplacementToken.DATE_YEAR_FULL.toString());

        Label episodeTitleLabel = new Label(replacementGroup, SWT.NONE);
        episodeTitleLabel.setText(RENAME_FORMAT_TEXT);
        episodeTitleLabel.setToolTipText(RENAME_FORMAT_TOOLTIP);

        replacementStringText = new Text(replacementGroup, SWT.BORDER);
        replacementStringText.setText(prefs.getRenameReplacementString());
        replacementStringText.setTextLimit(99);
        replacementStringText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

        createDragSource(renameTokensList);
        createDropTarget(replacementStringText);

        item.setControl(replacementGroup);
    }

    private static void createDragSource(final List sourceList) {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        DragSource dragSource = new DragSource(sourceList, DND_OPERATIONS);
        dragSource.setTransfer(types);
        dragSource.addDragListener(new DragSourceListener() {

            @Override
            public void dragStart(DragSourceEvent event) {
                if (sourceList.getSelectionIndex() != 0) {
                    event.doit = true;
                }
            }

            @Override
            public void dragSetData(DragSourceEvent event) {
                String listEntry = sourceList.getItem(sourceList.getSelectionIndex());
                String token;

                Matcher tokenMatcher =  Pattern.compile(REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX).matcher(listEntry);
                if (tokenMatcher.matches()) {
                    token = tokenMatcher.group(1);
                    event.data = token;
                }
            }

            @Override
            public void dragFinished(DragSourceEvent event) {
                // no-op
            }
        });
    }

    private static void createDropTarget(final Text targetText) {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        DropTarget dropTarget = new DropTarget(targetText, DND_OPERATIONS);
        dropTarget.setTransfer(types);

        dropTarget.addDropListener(new DropTargetListener() {

            @Override
            public void dragEnter(DropTargetEvent event) {
                // no-op
            }

            @Override
            public void dragLeave(DropTargetEvent event) {
                // no-op
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event) {
                // no-op
            }

            @Override
            public void dragOver(DropTargetEvent event) {
                // no-op
            }

            @Override
            public void drop(DropTargetEvent event) {
                String data = (String) event.data;
                //  TODO: This currently adds the dropped text onto the end, not where we dropped it
                targetText.append(data);
            }

            @Override
            public void dropAccept(DropTargetEvent event) {
                // no-op
            }
        });
    }

    private void createProxyTab() {
        ProxySettings proxy = prefs.getProxy();

        TabItem item = new TabItem(tabFolder, SWT.NULL);
        item.setText("Proxy");

        Composite proxyGroup = new Composite(tabFolder, SWT.NONE);
        proxyGroup.setLayout(new GridLayout(3, false));
        proxyGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
        proxyGroup.setToolTipText("If you connect to the internet via a proxy server, enable and set the properties");

        proxyEnabledCheckbox = new Button(proxyGroup, SWT.CHECK);
        proxyEnabledCheckbox.setText("Proxy Enabled");
        proxyEnabledCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
        proxyEnabledCheckbox.setSelection(proxy.isEnabled());

        Label proxyHostLabel = new Label(proxyGroup, SWT.NONE);
        proxyHostLabel.setText("Proxy Host [?]");
        proxyHostLabel.setToolTipText("The hostname or IP address of your proxy");

        proxyHostText = new Text(proxyGroup, SWT.BORDER);
        proxyHostText.setText(proxy.getHostname());
        proxyHostText.setTextLimit(99);
        proxyHostText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

        Label proxyPortLabel = new Label(proxyGroup, SWT.NONE);
        proxyPortLabel.setText("Proxy Port [?]");
        proxyPortLabel.setToolTipText("The port of your proxy");

        proxyPortText = new Text(proxyGroup, SWT.BORDER);
        proxyPortText.setText(proxy.getPort());
        proxyPortText.setTextLimit(99);
        proxyPortText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

        proxyAuthenticationRequiredCheckbox = new Button(proxyGroup, SWT.CHECK);
        proxyAuthenticationRequiredCheckbox.setText("Proxy Authentication Required");
        proxyAuthenticationRequiredCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
                                                                       true, true, 3, 1));
        proxyAuthenticationRequiredCheckbox.setSelection(proxy.isAuthenticationRequired());

        Label proxyUsernameLabel = new Label(proxyGroup, SWT.NONE);
        proxyUsernameLabel.setText("Proxy Username [?]");
        proxyUsernameLabel.setToolTipText("If you connect to a windows domain enter domain\\username");

        proxyUsernameText = new Text(proxyGroup, SWT.BORDER);
        proxyUsernameText.setText(proxy.getUsername());
        proxyUsernameText.setTextLimit(99);
        proxyUsernameText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

        Label proxyPasswordLabel = new Label(proxyGroup, SWT.NONE);
        proxyPasswordLabel.setText("Proxy Password");

        proxyPasswordText = new Text(proxyGroup, SWT.BORDER);
        proxyPasswordText.setEchoChar('*');
        proxyPasswordText.setText(proxy.getDecryptedPassword());
        proxyPasswordText.setTextLimit(99);
        proxyPasswordText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

        // Setup initial enabling controls
        toggleEnableControls(proxyEnabledCheckbox, proxyHostText, proxyPortText, proxyAuthenticationRequiredCheckbox);
        toggleEnableControls(proxyAuthenticationRequiredCheckbox, proxyUsernameText, proxyPasswordText);

        // Setup listeners
        proxyEnabledCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (proxyEnabledCheckbox.getSelection()) {
                    toggleEnableControls(proxyEnabledCheckbox, proxyHostText, proxyPortText, proxyAuthenticationRequiredCheckbox);
                } else {
                    toggleEnableControls(proxyEnabledCheckbox, proxyHostText, proxyPortText, proxyAuthenticationRequiredCheckbox, proxyUsernameText, proxyPasswordText);
                }
            }
        });

        proxyAuthenticationRequiredCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                toggleEnableControls(proxyAuthenticationRequiredCheckbox, proxyUsernameText, proxyPasswordText);
            }
        });

        item.setControl(proxyGroup);
    }

    private void createActionButtonGroup() {
        Composite bottomButtonsComposite = new Composite(preferencesShell, SWT.FILL);
        bottomButtonsComposite.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true, 0, 1));
        bottomButtonsComposite.setLayout(new GridLayout(2, false));
        GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        Button cancelButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData cancelButtonGridData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        cancelButtonGridData.minimumWidth = 150;
        cancelButtonGridData.widthHint = 150;
        cancelButton.setLayoutData(cancelButtonGridData);
        cancelButton.setText(CANCEL_LABEL);

        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                preferencesShell.close();
            }
        });

        Button saveButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData saveButtonGridData = new GridData(GridData.END, GridData.CENTER, true, true);
        saveButtonGridData.minimumWidth = 150;
        saveButtonGridData.widthHint = 150;
        saveButton.setLayoutData(saveButtonGridData);
        saveButton.setText(SAVE_LABEL);
        saveButton.setFocus();

        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                savePreferences();
                preferencesShell.close();
            }
        });

        // Set the OK button as the default, so
        // user can press Enter to save
        preferencesShell.setDefaultButton(saveButton);
    }

    /**
     * Save the preferences to the xml file
     */
    private void savePreferences() {
        // Update the preferences object from the UI control values
        prefs.setMoveEnabled(moveEnabledCheckbox.getSelection());
        prefs.setSeasonPrefix(seasonPrefixText.getText());
        prefs.setSeasonPrefixLeadingZero(seasonPrefixLeadingZeroCheckbox.getSelection());
        prefs.setRenameReplacementString(replacementStringText.getText());
        prefs.setIgnoreKeywords(Arrays.asList(ignoreWordsText.getText().split(IGNORE_WORDS_SPLIT_REGEX)));
        prefs.setRenameEnabled(renameEnabledCheckbox.getSelection());

        ProxySettings proxySettings = new ProxySettings();
        proxySettings.setEnabled(proxyEnabledCheckbox.getSelection());

        proxySettings.setHostname(proxyHostText.getText());
        proxySettings.setPort(proxyPortText.getText());

        proxySettings.setAuthenticationRequired(proxyAuthenticationRequiredCheckbox.getSelection());
        proxySettings.setUsername(proxyUsernameText.getText());
        proxySettings.setPlainTextPassword(proxyPasswordText.getText());

        prefs.setProxy(proxySettings);

        prefs.setCheckForUpdates(checkForUpdatesCheckbox.getSelection());
        prefs.setRecursivelyAddFolders(recurseFoldersCheckbox.getSelection());

        try {
            prefs.setDestinationDirectory(destDirText.getText());
        } catch (TVRenamerIOException e) {
            UIUtils.showMessageBox(SWTMessageBoxType.ERROR, ERROR_LABEL, CANT_CREATE_DEST + ": "
                + destDirText.getText());
            logger.log(Level.WARNING, CANT_CREATE_DEST, e);
        }
        UserPreferences.store(prefs);
    }

    /**
     * Toggle whether the or not the listed {@link Control}s are enabled, based off the of
     * the selection value of the checkbox
     * @param decidingCheckbox the checkbox the enable flag is taken off
     * @param controls the list of controls to update
     */
    private void toggleEnableControls(Button decidingCheckbox, Control... controls) {
        for (Control control : controls) {
            control.setEnabled(decidingCheckbox.getSelection());
        }
        preferencesShell.redraw();
    }
}
