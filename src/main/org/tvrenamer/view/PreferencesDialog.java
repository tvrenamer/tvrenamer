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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import org.tvrenamer.model.ReplacementToken;
import org.tvrenamer.model.UserPreferences;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PreferencesDialog extends Dialog {
    private static final Logger logger = Logger.getLogger(PreferencesDialog.class.getName());

    private static final int DND_OPERATIONS = DND.DROP_MOVE;

    private static class PreferencesDragSourceListener implements DragSourceListener {

        private final List sourceList;

        public PreferencesDragSourceListener(List sourceList) {
            this.sourceList = sourceList;
        }

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

            Pattern patt = Pattern.compile(REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX);
            Matcher tokenMatcher = patt.matcher(listEntry);
            if (tokenMatcher.matches()) {
                token = tokenMatcher.group(1);
                event.data = token;
            }
        }

        @Override
        public void dragFinished(DragSourceEvent event) {
            // no-op
        }
    }

    private static class PreferencesDropTargetListener implements DropTargetListener {

        private final Text targetText;

        public PreferencesDropTargetListener(Text targetText) {
            this.targetText = targetText;
        }

        @Override
        public void drop(DropTargetEvent event) {
            String data = (String) event.data;
            // TODO: This currently adds the dropped text onto the end, not where we dropped it
            targetText.append(data);
        }

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
        public void dropAccept(DropTargetEvent event) {
            // no-op
        }
    }

    private final UserPreferences prefs;

    // The controls to save
    private Button moveEnabledCheckbox;
    private Button renameEnabledCheckbox;
    private Text destDirText;
    private Button destDirButton;
    private Text seasonPrefixText;
    private Button seasonPrefixLeadingZeroCheckbox;
    private Text replacementStringText;
    private Text ignoreWordsText;
    private Button checkForUpdatesCheckbox;
    private Button recurseFoldersCheckbox;
    private Shell preferencesShell;

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
        helpLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true,
                                             shellGridLayout.numColumns, 1));

        TabFolder tabFolder = new TabFolder(preferencesShell, getStyle());
        tabFolder.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true,
                                             shellGridLayout.numColumns, 1));

        createGeneralTab(tabFolder);
        createRenameTab(tabFolder);

        createActionButtonGroup();
    }

    /**
     * Toggle whether the or not the listed {@link Control}s are enabled, based off the of
     * the given state value.
     *
     * @param state the boolean to set the other controls to
     * @param controls the list of controls to update
     */
    private void toggleEnableControls(boolean state, Control... controls) {
        for (Control control : controls) {
            control.setEnabled(state);
        }
        preferencesShell.redraw();
    }

    private void handleMoveCheckbox(final boolean moveEnabled) {
        // The terminology here gets confusing.  These are checkboxes which enable or disable
        // specific functionality.  But we also can enable or disable the checkboxes themselves!
        // That's what we're doing here.  Because it does not make sense to have "move" and
        // "rename" both disabled, when one functionality is disabled, we disable the other
        // check box, so that the user cannot disable the other functionality.
        if (moveEnabled) {
            renameEnabledCheckbox.setEnabled(true);
        } else {
            // If we are here, the user has disabled move functionality, but unchecking the
            // "move" checkbox.  Therefore, we need to make sure "rename' stays selected,
            // unless or until "move" is re-enabled.  The very fact that we got here means,
            // if things are working properly, that rename is *already* selected.  If it
            // weren't, the user shouldn't have been able to uncheck "move".  So, the first
            // line here should be redundant.  But, do it anyway, just in case somehow
            // there's a bug.
            renameEnabledCheckbox.setSelection(true);
            renameEnabledCheckbox.setEnabled(false);
        }
        toggleEnableControls(moveEnabled, destDirText, destDirButton, seasonPrefixText);
    }

    private void handleRenameCheckbox(final boolean renameEnabled) {
        // See comments in handleMoveCheckbox; all the same logic applies here, as well.
        if (renameEnabled) {
            moveEnabledCheckbox.setEnabled(true);
        } else {
            moveEnabledCheckbox.setSelection(true);
            moveEnabledCheckbox.setEnabled(false);
        }
    }

    private void createLabel(final String label, final String tooltip, final Composite group) {
        final Label labelObj = new Label(group, SWT.NONE);
        labelObj.setText(label);
        labelObj.setToolTipText(tooltip);

        // we don't need to return the object
    }

    private Text createText(final String text, final Composite group, boolean setSize) {
        final Text textObj = new Text(group, SWT.BORDER);
        textObj.setText(text);
        textObj.setTextLimit(99);
        GridData layout;
        if (setSize) {
            layout = new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1);
        } else {
            layout = new GridData(GridData.FILL, GridData.CENTER, true, true);
        }
        textObj.setLayoutData(layout);

        return textObj;
    }

    private Button createCheckbox(final String text, final String tooltip,
                                  final boolean isChecked, final Composite group,
                                  final int alignment, final int span)
    {
        final Button box = new Button(group, SWT.CHECK);
        box.setText(text);
        box.setSelection(isChecked);
        box.setLayoutData(new GridData(alignment, GridData.CENTER, true, true, span, 1));
        box.setToolTipText(tooltip);

        return box;
    }

    private Button createDestDirButton(Composite group) {
        final Button button = new Button(group, SWT.PUSH);
        button.setText(DEST_DIR_BUTTON_TEXT);
        button.addListener(SWT.Selection, event -> {
            DirectoryDialog directoryDialog = new DirectoryDialog(preferencesShell);

            directoryDialog.setFilterPath(prefs.getDestinationDirectoryName());
            directoryDialog.setText(DIR_DIALOG_TEXT);

            String dir = directoryDialog.open();
            if (dir != null) {
                destDirText.setText(dir);
            }
        });

        return button;
    }

    private void populateGeneralTab(final Composite generalGroup) {
        final boolean moveIsEnabled = prefs.isMoveEnabled();
        boolean renameIsEnabled = prefs.isRenameEnabled();
        if (!moveIsEnabled && !renameIsEnabled) {
            renameIsEnabled = true;
            prefs.setRenameEnabled(true);
        }
        moveEnabledCheckbox = createCheckbox(MOVE_ENABLED_TEXT, MOVE_ENABLED_TOOLTIP,
                                             moveIsEnabled, generalGroup, GridData.BEGINNING, 2);
        renameEnabledCheckbox = createCheckbox(RENAME_ENABLED_TEXT, RENAME_ENABLED_TOOLTIP,
                                               renameIsEnabled, generalGroup, GridData.END, 1);

        createLabel(DEST_DIR_TEXT, DEST_DIR_TOOLTIP, generalGroup);
        destDirText = createText(prefs.getDestinationDirectoryName(), generalGroup, false);
        destDirButton = createDestDirButton(generalGroup);

        createLabel(SEASON_PREFIX_TEXT, PREFIX_TOOLTIP, generalGroup);
        seasonPrefixText = createText(prefs.getSeasonPrefixForDisplay(), generalGroup, true);
        seasonPrefixLeadingZeroCheckbox = createCheckbox(SEASON_PREFIX_ZERO_TEXT, SEASON_PREFIX_ZERO_TOOLTIP,
                                                         prefs.isSeasonPrefixLeadingZero(),
                                                         generalGroup, GridData.BEGINNING, 3);

        createLabel(IGNORE_LABEL_TEXT, IGNORE_LABEL_TOOLTIP, generalGroup);
        ignoreWordsText = createText(prefs.getIgnoredKeywordsString(), generalGroup, false);

        recurseFoldersCheckbox = createCheckbox(RECURSE_FOLDERS_TEXT, RECURSE_FOLDERS_TOOLTIP,
                                                prefs.isRecursivelyAddFolders(), generalGroup,
                                                GridData.BEGINNING, 3);
        checkForUpdatesCheckbox = createCheckbox(CHECK_UPDATES_TEXT, CHECK_UPDATES_TOOLTIP,
                                                 prefs.checkForUpdates(), generalGroup,
                                                 GridData.BEGINNING, 3);

        handleMoveCheckbox(moveIsEnabled);
        handleRenameCheckbox(renameIsEnabled);
        moveEnabledCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleMoveCheckbox(moveEnabledCheckbox.getSelection());
            }
        });
        renameEnabledCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRenameCheckbox(renameEnabledCheckbox.getSelection());
            }
        });
    }

    private void createGeneralTab(final TabFolder tabFolder) {
        final TabItem item = new TabItem(tabFolder, SWT.NULL);
        item.setText(GENERAL_LABEL);

        final Composite generalGroup = new Composite(tabFolder, SWT.NONE);
        generalGroup.setLayout(new GridLayout(3, false));
        generalGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
        generalGroup.setToolTipText(GENERAL_TOOLTIP);

        populateGeneralTab(generalGroup);

        item.setControl(generalGroup);
    }

    private void createRenameTab(TabFolder tabFolder) {
        TabItem item = new TabItem(tabFolder, SWT.NULL);
        item.setText(RENAMING_LABEL);

        Composite replacementGroup = new Composite(tabFolder, SWT.NONE);
        replacementGroup.setLayout(new GridLayout(3, false));
        replacementGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));

        Label renameTokensLabel = new Label(replacementGroup, SWT.NONE);
        renameTokensLabel.setText(RENAME_TOKEN_TEXT);
        renameTokensLabel.setToolTipText(RENAME_TOKEN_TOOLTIP);

        List renameTokensList = new List(replacementGroup, SWT.SINGLE);
        renameTokensList.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER,
                                                    true, true, 2, 1));
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

        replacementStringText = createText(prefs.getRenameReplacementString(), replacementGroup, true);

        createDragSource(renameTokensList);
        createDropTarget(replacementStringText);

        item.setControl(replacementGroup);
    }

    private void createDragSource(final List sourceList) {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        DragSource dragSource = new DragSource(sourceList, DND_OPERATIONS);
        dragSource.setTransfer(types);
        dragSource.addDragListener(new PreferencesDragSourceListener(sourceList));
    }

    private void createDropTarget(final Text targetText) {
        Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
        DropTarget dropTarget = new DropTarget(targetText, DND_OPERATIONS);
        dropTarget.setTransfer(types);
        dropTarget.addDropListener(new PreferencesDropTargetListener(targetText));
    }

    private void createActionButtonGroup() {
        Composite bottomButtonsComposite = new Composite(preferencesShell, SWT.FILL);
        bottomButtonsComposite.setLayoutData(new GridData(SWT.END, SWT.CENTER,
                                                          true, true, 0, 1));
        bottomButtonsComposite.setLayout(new GridLayout(2, false));
        GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER,
                                                               true, true, 2, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        Button cancelButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData cancelButtonGridData = new GridData(GridData.BEGINNING, GridData.CENTER,
                                                     false, false);
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
        prefs.setSeasonPrefix(seasonPrefixText.getText());
        prefs.setSeasonPrefixLeadingZero(seasonPrefixLeadingZeroCheckbox.getSelection());
        prefs.setRenameReplacementString(replacementStringText.getText());
        prefs.setIgnoreKeywords(ignoreWordsText.getText());
        prefs.setCheckForUpdates(checkForUpdatesCheckbox.getSelection());
        prefs.setRecursivelyAddFolders(recurseFoldersCheckbox.getSelection());
        prefs.setDestinationDirectory(destDirText.getText());

        boolean isRenameEnabled = renameEnabledCheckbox.getSelection();
        // Note, it is important to do the "enable" (true) before the "disable" (false),
        // because we cannot disable both even momentarily.
        if (moveEnabledCheckbox.getSelection()) {
            prefs.setMoveEnabled(true);
            prefs.setRenameEnabled(isRenameEnabled);
        } else {
            // Since move is disabled, rename needs to be enabled.  We're going to
            // enable rename no matter what.  If that's not what the UI says to do,
            // that's an error.
            if (!isRenameEnabled) {
                // The UI is supposed to prevent this situation from happening.
                logger.severe("internal error! should not be able to disable "
                              + "both move and rename! enabling rename");
            }
            prefs.setRenameEnabled(true);
            prefs.setMoveEnabled(false);
        }

        UIUtils.checkDestinationDirectory(prefs);

        UserPreferences.store(prefs);
    }
}
