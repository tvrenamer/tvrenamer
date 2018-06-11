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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
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

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.ReplacementToken;
import org.tvrenamer.model.UserPreferences;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PreferencesDialog extends Dialog {
    private static final Logger logger = Logger.getLogger(PreferencesDialog.class.getName());
    private static final UserPreferences prefs = UserPreferences.getInstance();

    private static final int DND_OPERATIONS = DND.DROP_MOVE;
    private static final char DOUBLE_QUOTE = '"';

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

    // The controls to save
    private Button moveSelectedCheckbox;
    private Button renameSelectedCheckbox;
    private Text destDirText;
    private Button destDirButton;
    private Text seasonPrefixText;
    private Button seasonPrefixLeadingZeroCheckbox;
    private Text replacementStringText;
    private Text ignoreWordsText;
    private Button checkForUpdatesCheckbox;
    private Button recurseFoldersCheckbox;
    private Button rmdirEmptyCheckbox;
    private Button deleteRowsCheckbox;
    private Shell preferencesShell;

    private final StatusLabel statusLabel;

    private String seasonPrefixString;

    /**
     * PreferencesDialog constructor
     *
     * @param parent
     *            the parent {@link Shell}
     */
    public PreferencesDialog(Shell parent) {
        super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        statusLabel = new StatusLabel();
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
        GridLayout shellGridLayout = new GridLayout(4, false);
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

        statusLabel.open(preferencesShell, shellGridLayout.numColumns);

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

    /*
     * Return true if the parameters indicate a double-quote character
     * is being inserted as the first or last character of the text.
     *
     * The purpose of this method is that the double-quote is an illegal
     * character in file paths, but it is allowed in the text box where
     * the user enters the prefix -- but only as surrounding characters,
     * to show the limits of the text being entered (i.e., to help display
     * whitespace).
     *
     * Note this test is not sufficient on its own.  If the text is quoted
     * already, and then the user tries to add a double-quote in front of
     * the existing quote, that should not be allowed.  It's assumed that
     * situation is caught by other code; this method just detects if it's
     * the first or last character.
     */
    private boolean quoteAtBeginningOrEnd(final char c, final int pos,
                                          final int start, final int end,
                                          final int originalLength,
                                          final int insertLength)
    {
        // The user has entered a character that is not a double quote.
        if (c != DOUBLE_QUOTE) {
            return false;
        }
        // If start is 0, that means we're inserting at the beginning of the text box;
        // but this may be the result of a paste, so we may be inserting multiple
        // characters.  Checking (pos == 0) makes sure we're looking at the first
        // character of the text that's being inserted.
        if ((start == 0) && (pos == 0)) {
            return true;
        }
        // This is the same idea.  "end == originalLength" means we're inserting at
        // the end of the text box, but we only want to allow the double quote if
        // it's the LAST character of the text being inserted.
        if ((end == originalLength) && (pos == (insertLength - 1))) {
            return true;
        }
        // The user has tried to insert a double quote somewhere other than the first
        // or last character.
        return false;
    }

    /*
     * A sub-method to be called once it's been determined that the user has tried to insert
     * text into the "season prefix" text box, in a legal position (i.e., not before the
     * opening quote, and not after the closing quote.)  Not all edits are insertions; some
     * just delete text.
     *
     * Constructs a string with any illegal characters removed.  If the text is the same
     * length as what we got from the event, then all characters were legal.  If the new text
     * is zero length, then all characters were illegal, and we reject the insertion.  If the
     * length is neither zero, nor the full length of the inserted text, then the user has
     * pasted in some mix of legal and illegal characters.  We strip away the illegal ones,
     * and insert the legal ones, with a warning given to the user.
     */
    private void filterEnteredSeasonPrefixText(VerifyEvent e, final int previousTextLength) {
        String textToInsert = e.text;
        int insertLength = textToInsert.length();
        StringBuilder acceptedText = new StringBuilder(insertLength);
        for (int i = 0; i < insertLength; i++) {
            char c = textToInsert.charAt(i);
            boolean isLegal = StringUtils.isLegalFilenameCharacter(c)
                || quoteAtBeginningOrEnd(c, i, e.start, e.end,
                                         previousTextLength, insertLength);
            if (isLegal) {
                acceptedText.append(c);
            }
        }
        if (acceptedText.length() == insertLength) {
            statusLabel.clear(ILLEGAL_CHARACTERS_WARNING);
        } else {
            statusLabel.add(ILLEGAL_CHARACTERS_WARNING);
            if (acceptedText.length() == 0) {
                e.doit = false;
            } else {
                e.text = acceptedText.toString();
            }
        }
    }

    /*
     * The main verifier method for the "season prefix" text box.  The basic idea is
     * that we want to prohibit characters that are illegal in filenames.  But we
     * added a complication by deciding to display (and accept) the text with or without
     * surrounding double quotes.
     *
     * Double quotes are, of course, illegal in filenames (on the filesystems we care
     * about, anyway).  So they are generally prohibited.  And we put the surrounding
     * quotes up by default, so the user never needs to type them.  But it's very possible
     * they might delete the quotes, and then try to re-enter them, and that should be
     * supported.  And we also support them deleting the quotes and NOT reinstating them.
     *
     * A really stringent application might insist the quotes be either absent or balanced.
     * But that makes it impossible to delete a quote, unless you delete the entire text.
     * That's very annoying.  So we allow them to be unbalanced.  The StringUtils method
     * unquoteString will remove the quote from the front and from the back, whether they
     * are balanced or not.
     *
     * In order to avoid having the illegal quote character in the middle of the text, we
     * cannot allow the user to insert any text before the opening quote, or any text after
     * the closing quote.  Doing so would change them from delimiters to part of the text.
     *
     * Edits might not be inserting text at all.  They could be deleting text.  This method
     * checks that the user is trying to insert text, and that it's not before the opening
     * quote or after the closing quote.  If that's the case, it calls the next method,
     * filterEnteredSeasonPrefixText, to ensure no illegal characters are being entered.
     */
    private void verifySeasonPrefixText(VerifyEvent e) {
        if (e.text.length() > 0) {
            String previousText = seasonPrefixText.getText();
            int originalLength = previousText.length();

            if ((e.end < (originalLength - 1))
                && (previousText.charAt(e.end) == DOUBLE_QUOTE))
            {
                statusLabel.add(NO_TEXT_BEFORE_OPENING_QUOTE);
                e.doit = false;
            } else if ((e.start > 1)
                       && (previousText.charAt(e.start - 1) == DOUBLE_QUOTE))
            {
                statusLabel.add(NO_TEXT_AFTER_CLOSING_QUOTE);
                e.doit = false;
            } else {
                filterEnteredSeasonPrefixText(e, originalLength);
            }
        }
    }

    /*
     * Create the controls that regard the naming of the season prefix folder.
     * The text box gets both a verify listener and a modify listener.
     */
    private void createSeasonPrefixControls(final Composite generalGroup) {
        createLabel(SEASON_PREFIX_TEXT, PREFIX_TOOLTIP, generalGroup);
        seasonPrefixString = prefs.getSeasonPrefix();
        seasonPrefixText = createText(StringUtils.makeQuotedString(seasonPrefixString),
                                      generalGroup, true);
        seasonPrefixText.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                statusLabel.clear(NO_TEXT_BEFORE_OPENING_QUOTE);
                statusLabel.clear(NO_TEXT_AFTER_CLOSING_QUOTE);
                verifySeasonPrefixText(e);
            }
        });
        seasonPrefixText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                String prefixText = seasonPrefixText.getText();

                // Remove the surrounding double quotes, if present;
                // any other double quotes should not be removed.
                String unquoted = StringUtils.unquoteString(prefixText);
                // The verifier should have prevented any illegal characters from
                // being entered.  This is just to check.
                seasonPrefixString = StringUtils.replaceIllegalCharacters(unquoted);

                if (!seasonPrefixString.equals(unquoted)) {
                    // Somehow, illegal characters got through.
                    logger.severe("Illegal characters recognized in season prefix");
                    logger.severe("Instead of \"" + unquoted + "\", will use \""
                                 + seasonPrefixString + "\"");
                }
            }
        });
        seasonPrefixLeadingZeroCheckbox = createCheckbox(SEASON_PREFIX_ZERO_TEXT, SEASON_PREFIX_ZERO_TOOLTIP,
                                                         prefs.isSeasonPrefixLeadingZero(),
                                                         generalGroup, GridData.BEGINNING, 3);
    }

    private void populateGeneralTab(final Composite generalGroup) {
        final boolean moveIsSelected = prefs.isMoveEnabled();
        boolean renameIsSelected = prefs.isRenameEnabled();
        moveSelectedCheckbox = createCheckbox(MOVE_SELECTED_TEXT, MOVE_SELECTED_TOOLTIP,
                                              moveIsSelected, generalGroup, GridData.BEGINNING, 2);
        renameSelectedCheckbox = createCheckbox(RENAME_SELECTED_TEXT, RENAME_SELECTED_TOOLTIP,
                                                renameIsSelected, generalGroup, GridData.END, 1);

        createLabel(DEST_DIR_TEXT, DEST_DIR_TOOLTIP, generalGroup);
        destDirText = createText(prefs.getDestinationDirectoryName(), generalGroup, false);
        destDirButton = createDestDirButton(generalGroup);

        createSeasonPrefixControls(generalGroup);

        toggleEnableControls(moveIsSelected, destDirText, destDirButton,
                             seasonPrefixText, seasonPrefixLeadingZeroCheckbox);

        moveSelectedCheckbox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                toggleEnableControls(moveSelectedCheckbox.getSelection(), destDirText, destDirButton,
                                     seasonPrefixText, seasonPrefixLeadingZeroCheckbox);
            }
        });
        createLabel(IGNORE_LABEL_TEXT, IGNORE_LABEL_TOOLTIP, generalGroup);
        ignoreWordsText = createText(prefs.getIgnoredKeywordsString(), generalGroup, false);

        recurseFoldersCheckbox = createCheckbox(RECURSE_FOLDERS_TEXT, RECURSE_FOLDERS_TOOLTIP,
                                                prefs.isRecursivelyAddFolders(), generalGroup,
                                                GridData.BEGINNING, 3);
        rmdirEmptyCheckbox = createCheckbox(REMOVE_EMPTIED_TEXT, REMOVE_EMPTIED_TOOLTIP,
                                            prefs.isRemoveEmptiedDirectories(), generalGroup,
                                            GridData.BEGINNING, 3);
        deleteRowsCheckbox = createCheckbox(DELETE_ROWS_TEXT, DELETE_ROWS_TOOLTIP,
                                            prefs.isDeleteRowAfterMove(), generalGroup,
                                            GridData.BEGINNING, 3);
        checkForUpdatesCheckbox = createCheckbox(CHECK_UPDATES_TEXT, CHECK_UPDATES_TOOLTIP,
                                                 prefs.checkForUpdates(), generalGroup,
                                                 GridData.BEGINNING, 3);
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
        bottomButtonsComposite.setLayout(new GridLayout(2, false));
        bottomButtonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER,
                                                          true, true, 2, 1));

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
        prefs.setMoveEnabled(moveSelectedCheckbox.getSelection());
        prefs.setSeasonPrefix(seasonPrefixString);
        prefs.setSeasonPrefixLeadingZero(seasonPrefixLeadingZeroCheckbox.getSelection());
        prefs.setRenameReplacementString(replacementStringText.getText());
        prefs.setIgnoreKeywords(ignoreWordsText.getText());
        prefs.setRenameEnabled(renameSelectedCheckbox.getSelection());

        prefs.setCheckForUpdates(checkForUpdatesCheckbox.getSelection());
        prefs.setRecursivelyAddFolders(recurseFoldersCheckbox.getSelection());
        prefs.setRemoveEmptiedDirectories(rmdirEmptyCheckbox.getSelection());
        prefs.setDeleteRowAfterMove(deleteRowsCheckbox.getSelection());
        prefs.setDestinationDirectory(destDirText.getText());

        UserPreferences.store(prefs);
    }
}
