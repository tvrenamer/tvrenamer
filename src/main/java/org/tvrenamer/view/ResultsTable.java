package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.Fields.*;
import static org.tvrenamer.view.ItemState.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;

import org.tvrenamer.controller.AddEpisodeListener;
import org.tvrenamer.controller.FileMover;
import org.tvrenamer.controller.MoveRunner;
import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.ShowListingsListener;
import org.tvrenamer.controller.UpdateChecker;
import org.tvrenamer.controller.UrlLauncher;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.EpisodeDb;
import org.tvrenamer.model.FailedShow;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;

import java.text.Collator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.logging.Logger;

public final class ResultsTable implements Observer, AddEpisodeListener {
    private static final Logger logger = Logger.getLogger(ResultsTable.class.getName());
    // load preferences
    private static final UserPreferences prefs = UserPreferences.getInstance();
    private static final Collator COLLATOR = Collator.getInstance(Locale.getDefault());

    private static final int ITEM_NOT_IN_TABLE = -1;

    private static final int WIDTH_CHECKED = 30;
    private static final int WIDTH_CURRENT_FILE = 550;
    private static final int WIDTH_NEW_FILENAME = 550;
    private static final int WIDTH_STATUS = 60;

    private final UIStarter ui;
    private final Shell shell;
    private final Display display;
    private final Table swtTable;
    private final EpisodeDb episodeMap = new EpisodeDb();

    private Button actionButton;
    private ProgressBar totalProgressBar;
    private TaskItem taskItem = null;

    private boolean apiDeprecated = false;

    private synchronized void checkDestinationDirectory() {
        boolean success = prefs.ensureDestDir();
        if (!success) {
            logger.warning(CANT_CREATE_DEST);
            ui.showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL, CANT_CREATE_DEST + ": '"
                              + prefs.getDestinationDirectoryName() + "'. "
                              + MOVE_NOT_POSSIBLE);
        }
    }

    void ready() {
        prefs.addObserver(this);
        swtTable.setFocus();

        checkDestinationDirectory();

        // Load the preload folder into the episode map, which will call
        // us back with the list of files once they've been loaded.
        episodeMap.subscribe(this);
        episodeMap.preload();
    }

    Display getDisplay() {
        return display;
    }

    ProgressBar getProgressBar() {
        return totalProgressBar;
    }

    TaskItem getTaskItem() {
        return taskItem;
    }

    private Combo newComboBox() {
        if (swtTable.isDisposed()) {
            return null;
        }
        return new Combo(swtTable, SWT.DROP_DOWN | SWT.READ_ONLY);
    }

    private TableItem newTableItem() {
        return new TableItem(swtTable, SWT.NONE);
    }

    private void setComboBoxProposedDest(final TableItem item, final FileEpisode ep) {
        if (swtTable.isDisposed() || item.isDisposed()) {
            return;
        }
        final List<String> options = ep.getReplacementOptions();
        final int chosen = ep.getChosenEpisode();
        final String defaultOption = options.get(chosen);
        NEW_FILENAME_FIELD.setCellText(item, defaultOption);

        final Combo combo = newComboBox();
        if (combo == null) {
            return;
        }
        options.forEach(combo::add);
        combo.setText(defaultOption);
        combo.addModifyListener(e -> ep.setChosenEpisode(combo.getSelectionIndex()));
        item.setData(combo);

        final TableEditor editor = new TableEditor(swtTable);
        editor.grabHorizontal = true;
        NEW_FILENAME_FIELD.setEditor(item, editor, combo);
    }

    private void deleteItemCombo(final TableItem item) {
        final Object itemData = item.getData();
        if (itemData != null) {
            final Control oldCombo = (Control) itemData;
            if (!oldCombo.isDisposed()) {
                oldCombo.dispose();
            }
        }
    }

    /**
     * Fill in the value for the "Proposed File" column of the given row, with the text
     * we get from the given episode.  This is the only method that should ever set
     * this text, to ensure that the text of each row is ALWAYS the value returned by
     * getReplacementText() on the associated episode.
     *
     * @param item
     *    the row in the table to set the text of the "Proposed File" column
     * @param ep
     *    the FileEpisode to use to obtain the text
     */
    private void setProposedDestColumn(final TableItem item, final FileEpisode ep) {
        if (swtTable.isDisposed() || item.isDisposed()) {
            return;
        }
        deleteItemCombo(item);

        int nOptions = ep.optionCount();
        if (nOptions > 1) {
            setComboBoxProposedDest(item, ep);
        } else if (nOptions == 1) {
            NEW_FILENAME_FIELD.setCellText(item, ep.getReplacementText());
        } else {
            NEW_FILENAME_FIELD.setCellText(item, ep.getReplacementText());
            item.setChecked(false);
        }
    }

    private void failTableItem(final TableItem item) {
        STATUS_FIELD.setCellImage(item, FAIL);
        item.setChecked(false);
    }

    private void setTableItemStatus(final TableItem item, final int epsFound) {
        if (epsFound > 1) {
            STATUS_FIELD.setCellImage(item, OPTIONS);
            item.setChecked(true);
        } else if (epsFound == 1) {
            STATUS_FIELD.setCellImage(item, SUCCESS);
            item.setChecked(true);
        } else {
            failTableItem(item);
        }
    }

    private int getTableItemIndex(final TableItem item) {
        try {
            return swtTable.indexOf(item);
        } catch (IllegalArgumentException | SWTException ignored) {
            // We'll just fall through and return the sentinel.
        }
        return ITEM_NOT_IN_TABLE;
    }

    private boolean tableContainsTableItem(final TableItem item) {
        return (ITEM_NOT_IN_TABLE != getTableItemIndex(item));
    }

    private void listingsDownloaded(final TableItem item, final FileEpisode episode) {
        int epsFound = episode.listingsComplete();
        display.asyncExec(() -> {
            if (tableContainsTableItem(item)) {
                setProposedDestColumn(item, episode);
                setTableItemStatus(item, epsFound);
            }
        });
    }

    private void listingsFailed(final TableItem item, final FileEpisode episode, final Exception err) {
        episode.listingsFailed(err);
        display.asyncExec(() -> {
            if (tableContainsTableItem(item)) {
                setProposedDestColumn(item, episode);
                failTableItem(item);
            }
        });
    }

    private void getSeriesListings(final Series series, final TableItem item,
                                   final FileEpisode episode)
    {
        series.addListingsListener(new ShowListingsListener() {
            @Override
            public void listingsDownloadComplete() {
                listingsDownloaded(item, episode);
            }

            @Override
            public void listingsDownloadFailed(Exception err) {
                listingsFailed(item, episode, err);
            }
        });
    }

    private void tableItemFailed(final TableItem item, final FileEpisode episode) {
        display.asyncExec(() -> {
            if (tableContainsTableItem(item)) {
                setProposedDestColumn(item, episode);
                failTableItem(item);
            }
        });
    }

    private synchronized void noteApiFailure() {
        boolean showDialogBox = !apiDeprecated;
        apiDeprecated = true;
        if (showDialogBox) {
            boolean updateIsAvailable = UpdateChecker.isUpdateAvailable();
            ui.showMessageBox(SWTMessageBoxType.DLG_ERR, ERROR_LABEL,
                              updateIsAvailable ? GET_UPDATE_MESSAGE : NEED_UPDATE);
        }
    }

    private TableItem createTableItem(final FileEpisode episode) {
        TableItem item = newTableItem();

        // Initially we add items to the table unchecked.  When we successfully obtain enough
        // information about the episode to determine how to rename it, the check box will
        // automatically be activated.
        item.setChecked(false);
        CURRENT_FILE_FIELD.setCellText(item, episode.getFilepath());
        setProposedDestColumn(item, episode);
        STATUS_FIELD.setCellImage(item, DOWNLOADING);
        return item;
    }

    @Override
    public void addEpisodes(final Queue<FileEpisode> episodes) {
        for (final FileEpisode episode : episodes) {
            final TableItem item = createTableItem(episode);
            if (!episode.wasParsed()) {
                failTableItem(item);
                continue;
            }
            synchronized (this) {
                if (apiDeprecated) {
                    tableItemFailed(item, episode);
                    continue;
                }
            }

            final String showName = episode.getFilenameShow();
            if (StringUtils.isBlank(showName)) {
                logger.fine("no show name found for " + episode);
                continue;
            }
            ShowStore.mapStringToShow(showName, new ShowInformationListener() {
                    @Override
                    public void downloadSucceeded(Show show) {
                        episode.setEpisodeShow(show);
                        display.asyncExec(() -> {
                            if (tableContainsTableItem(item)) {
                                setProposedDestColumn(item, episode);
                                STATUS_FIELD.setCellImage(item, ADDED);
                            }
                        });
                        if (show.isValidSeries()) {
                            getSeriesListings(show.asSeries(), item, episode);
                        }
                    }

                    @Override
                    public void downloadFailed(FailedShow failedShow) {
                        episode.setFailedShow(failedShow);
                        tableItemFailed(item, episode);
                    }

                    @Override
                    public void apiHasBeenDeprecated() {
                        noteApiFailure();
                        episode.setApiDiscontinued();
                        tableItemFailed(item, episode);
                    }
                });
        }
    }

    /**
     * Returns (and, really, creates) a progress label for the given item.
     * This is used to display progress while the item's file is being copied.
     * (We don't actually support "copying" the file, only moving it, but when
     * the user chooses to "move" it across filesystems, that becomes a copy-
     * and-delete operation.)
     *
     * @param item
     *    the item to create a progress label for
     * @return
     *    a Label which is set as an editor for the status field of the given item
     */
    public Label getProgressLabel(final TableItem item) {
        Label progressLabel = new Label(swtTable, SWT.SHADOW_NONE | SWT.CENTER);
        TableEditor editor = new TableEditor(swtTable);
        editor.grabHorizontal = true;
        STATUS_FIELD.setEditor(item, editor, progressLabel);

        return progressLabel;
    }

    private void renameFiles() {
        if (!prefs.isMoveEnabled() && !prefs.isRenameSelected()) {
            logger.info("move and rename both disabled, nothing to be done.");
            return;
        }

        final List<FileMover> pendingMoves = new LinkedList<>();
        for (final TableItem item : swtTable.getItems()) {
            if (item.getChecked()) {
                String fileName = CURRENT_FILE_FIELD.getCellText(item);
                final FileEpisode episode = episodeMap.get(fileName);
                // Skip files not successfully downloaded and ready to be moved
                if (episode.optionCount() == 0) {
                    logger.info("checked but not ready: " + episode.getFilepath());
                    continue;
                }
                FileMover pendingMove = new FileMover(episode);
                pendingMove.addObserver(new FileCopyMonitor(this, item));
                pendingMoves.add(pendingMove);
            }
        }

        MoveRunner mover = new MoveRunner(pendingMoves);
        mover.setUpdater(new ProgressBarUpdater(this));
        mover.runThread();
        swtTable.setFocus();
    }

    /**
     * Insert a copy of the row at the given position, and then delete the original row.
     * Note that insertion does not overwrite the row that is already there.  It pushes
     * the row, and every row below it, down one slot.
     *
     * @param oldItem
     *   the TableItem to copy
     * @param positionToInsert
     *   the position where we should insert the row
     */
    private void setSortedItem(final TableItem oldItem, final int positionToInsert) {
        boolean wasChecked = oldItem.getChecked();

        TableItem item = new TableItem(swtTable, SWT.NONE, positionToInsert);
        item.setChecked(wasChecked);
        CURRENT_FILE_FIELD.setCellText(item, CURRENT_FILE_FIELD.getCellText(oldItem));
        NEW_FILENAME_FIELD.setCellText(item, NEW_FILENAME_FIELD.getCellText(oldItem));
        STATUS_FIELD.setCellImage(item, STATUS_FIELD.getCellImage(oldItem));

        final Object itemData = oldItem.getData();

        // Although the name suggests dispose() is primarily about reclaiming system
        // resources, it also deletes the item from the Table.
        oldItem.dispose();
        if (itemData != null) {
            final TableEditor newEditor = new TableEditor(swtTable);
            newEditor.grabHorizontal = true;
            NEW_FILENAME_FIELD.setEditor(item, newEditor, (Control) itemData);
            item.setData(itemData);
        }
    }

    /**
     * Sort the table by the given column in the given direction.
     *
     * @param column
     *    the Column to sort by
     * @param sortDirection
     *    the direction to sort by; SWT.UP means sort A-Z, while SWT.DOWN is Z-A
     */
    void sortTable(final Column column, final int sortDirection) {
        Field field = column.field;

        // Get the items
        TableItem[] items = swtTable.getItems();

        // Go through the item list and bubble rows up to the top as appropriate
        for (int i = 1; i < items.length; i++) {
            String value1 = field.getItemTextValue(items[i]);
            for (int j = 0; j < i; j++) {
                String value2 = field.getItemTextValue(items[j]);
                // Compare the two values and order accordingly
                int comparison = COLLATOR.compare(value1, value2);
                if (((comparison < 0) && (sortDirection == SWT.UP))
                    || (comparison > 0) && (sortDirection == SWT.DOWN))
                {
                    // Insert a copy of row i at position j, and then delete
                    // row i.  Then fetch the list of items anew, since we
                    // just modified it.
                    setSortedItem(items[i], j);
                    items = swtTable.getItems();
                    break;
                }
            }
        }
        swtTable.setSortDirection(sortDirection);
        swtTable.setSortColumn(column.swtColumn);
    }

    /**
     * Refreshes the "destination" and "status" field of all items in the table.
     *
     * This is intended to be called after something happens which changes what the
     * proposed destination would be.  The destination is determined partly by how
     * we parse the filename, of course, but also based on numerous fields that the
     * user sets in the Preferences Dialog.  When the user closes the dialog and
     * saves the changes, we want to immediately update the table for the new choices
     * specified.  This method iterates over each item, makes sure the model is
     * updated ({@link FileEpisode}), and then updates the relevant fields.
     *
     * (Doesn't bother updating other fields, because we know nothing in the
     * Preferences Dialog can cause them to need to be changed.)
     */
    public void refreshDestinations() {
        logger.info("Refreshing destinations");
        for (TableItem item : swtTable.getItems()) {
            String fileName = CURRENT_FILE_FIELD.getCellText(item);
            String newFileName = episodeMap.currentLocationOf(fileName);
            if (newFileName == null) {
                // Not expected, but could happen, primarily if some other,
                // unrelated program moves the file out from under us.
                deleteTableItem(item);
                return;
            }
            FileEpisode episode = episodeMap.get(newFileName);
            episode.refreshReplacement();
            setProposedDestColumn(item, episode);
            setTableItemStatus(item, episode.optionCount());
        }
    }

    private void setActionButtonText(final Button b) {
        String label = JUST_MOVE_LABEL;
        if (prefs.isRenameSelected()) {
            if (prefs.isMoveSelected()) {
                label = RENAME_AND_MOVE;
            } else {
                label = RENAME_LABEL;
            }
            // In the unlikely and erroneous case where neither is selected,
            // we'll still stick with JUST_MOVE_LABEL for the label.
        }
        b.setText(label);

        // Enable the button, in case it had been disabled before.  But we may
        // disable it again, below.
        b.setEnabled(true);

        String tooltip = RENAME_TOOLTIP;
        if (prefs.isMoveSelected()) {
            if (prefs.isMoveEnabled()) {
                tooltip = INTRO_MOVE_DIR + prefs.getDestinationDirectoryName()
                    + FINISH_MOVE_DIR;
                if (prefs.isRenameSelected()) {
                    tooltip = MOVE_INTRO + AND_RENAME + tooltip;
                } else {
                    tooltip = MOVE_INTRO + tooltip;
                }
            } else {
                b.setEnabled(false);
                tooltip = CANT_CREATE_DEST + ". " + MOVE_NOT_POSSIBLE;
            }
        } else if (!prefs.isRenameSelected()) {
            // This setting, "do not move and do not rename", really makes no sense.
            // But for now, we're not taking the effort to explicitly disable it.
            b.setEnabled(false);
            tooltip = NO_ACTION_TOOLTIP;
        }
        b.setToolTipText(tooltip);

        shell.changed(new Control[] {b});
        shell.layout(false, true);
    }

    private void setColumnDestText() {
        final TableColumn destinationColumn = NEW_FILENAME_FIELD.getTableColumn();
        if (destinationColumn == null) {
            logger.warning("could not get destination column");
        } else if (prefs.isMoveSelected()) {
            destinationColumn.setText(MOVE_HEADER);
        } else {
            destinationColumn.setText(RENAME_HEADER);
        }
    }

    private void deleteTableItem(final TableItem item) {
        deleteItemCombo(item);
        episodeMap.remove(CURRENT_FILE_FIELD.getCellText(item));
        item.dispose();
    }

    private void deleteSelectedTableItems() {
        for (final TableItem item : swtTable.getSelection()) {
            int index = getTableItemIndex(item);
            deleteTableItem(item);

            if (ITEM_NOT_IN_TABLE == index) {
                logger.info("error: somehow selected item not found in table");
            }
        }
        swtTable.deselectAll();
    }

    private void updateUserPreferences(final UserPreference userPref) {
        logger.info("Preference change event: " + userPref);

        switch (userPref) {
            case RENAME_SELECTED:
            case MOVE_SELECTED:
            case DEST_DIR:
                checkDestinationDirectory();
                setColumnDestText();
                setActionButtonText(actionButton);
                // Note: NO break!  We WANT to fall through.
            case REPLACEMENT_MASK:
            case SEASON_PREFIX:
            case LEADING_ZERO:
                refreshDestinations();
            // Also note, no default case.  We know there are other types of
            // UserPreference events that we might be notified of.  We're
            // just not interested.
        }
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(final Observable observable, final Object value) {
        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            updateUserPreferences((UserPreference) value);
        }
    }

    void finishAllMoves() {
        ui.setAppIcon();
    }

    /*
     * The table displays various data; a lot of it changes during the course of the
     * program.  As we get information from the provider, we automatically update the
     * status, the proposed destination, even whether the row is checked or not.
     *
     * The one thing we don't automatically update is the location.  That's something
     * that doesn't change, no matter how much information comes flowing in.  EXCEPT...
     * that's kind of the whole point of the program, to move files.  So when we actually
     * do move a file, we need to update things in some way.
     *
     * The program now has the "deleteRowAfterMove" option, which I recommend.  But if
     * we do not delete the row, then we need to update it.
     *
     * We also need to update the internal model we have of which files we're working with.
     *
     * So, here's what we do:
     *  1) find the text that is CURRENTLY being displayed as the file's location
     *  2) ask EpisodeDb to look up that file, figure out where it now resides, update its
     *     own internal model, and then return to us the current location
     *  3) assuming the file was found, check to see if it was really moved
     *  4) if it actually was moved, update the row with the most current information
     *
     * We do all this only after checking the row is still valid, and then we do it
     * with the item locked, so it can't change out from under us.
     *
     */
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void updateTableItemAfterMove(final TableItem item) {
        synchronized (item) {
            if (item.isDisposed()) {
                return;
            }
            String fileName = CURRENT_FILE_FIELD.getCellText(item);
            String newLocation = episodeMap.currentLocationOf(fileName);
            if (newLocation == null) {
                // Not expected, but could happen, primarily if some other,
                // unrelated program moves the file out from under us.
                deleteTableItem(item);
                return;
            }
            if (!fileName.equals(newLocation)) {
                CURRENT_FILE_FIELD.setCellText(item, newLocation);
            }
        }
    }

    /**
     * A callback that indicates that the {@link FileMover} has finished trying
     * to move a file, the one displayed in the given item.  We want to take
     * an action when the move has been finished.
     *
     * The specific action depends on the user preference, "deleteRowAfterMove".
     * As its name suggests, when it's true, and we successfully move the file,
     * we delete the TableItem from the table.
     *
     * If "deleteRowAfterMove" is false, then the moved file remains in the
     * table.  There's no reason why its proposed destination should change;
     * nothing that is used to create the proposed destination has changed.
     * But one thing that has changed is the file's current location.  We call
     * helper method updateTableItemAfterMove to update the table.
     *
     * If the move actually did not succeed, we log a message in development,
     * but currently don't do anything to make it obvious to the user that the
     * move failed.  Perhaps we should do more...
     *
     * @param item
     *   the item representing the file that we've just finished trying to move
     * @param success
     *   whether or not we actually succeeded in moving the file
     */
    public void finishMove(final TableItem item, final boolean success) {
        if (success) {
            if (prefs.isDeleteRowAfterMove()) {
                deleteTableItem(item);
            } else {
                updateTableItemAfterMove(item);
            }
        } else {
            // Should we do anything else, visible to the user?  Uncheck the row?
            // We don't really have a good option, right now.  TODO.
            logger.info("failed to move item: " + item);
        }
    }

    private void setupUpdateStuff(final Composite parentComposite) {
        Link updatesAvailableLink = new Link(parentComposite, SWT.VERTICAL);
        // updatesAvailableLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        updatesAvailableLink.setVisible(false);
        updatesAvailableLink.setText(UPDATE_AVAILABLE);
        updatesAvailableLink.addSelectionListener(new UrlLauncher(TVRENAMER_DOWNLOAD_URL));

        // Show the label if updates are available (in a new thread)
        UpdateChecker.notifyOfUpdate(updateIsAvailable -> {
            if (updateIsAvailable) {
                display.asyncExec(() -> updatesAvailableLink.setVisible(true));
            }
        });
    }

    private void setupTopButtons() {
        final Composite topButtonsComposite = new Composite(shell, SWT.FILL);
        topButtonsComposite.setLayout(new RowLayout());

        final FileDialog fd = new FileDialog(shell, SWT.MULTI);
        final Button addFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        addFilesButton.setText("Add files");
        addFilesButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String pathPrefix = fd.open();
                if (pathPrefix != null) {
                    episodeMap.addFilesToQueue(pathPrefix, fd.getFileNames());
                }
            }
        });

        final DirectoryDialog dd = new DirectoryDialog(shell, SWT.SINGLE);
        final Button addFolderButton = new Button(topButtonsComposite, SWT.PUSH);
        addFolderButton.setText("Add Folder");
        addFolderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String directory = dd.open();
                if (directory != null) {
                    // load all of the files in the dir
                    episodeMap.addFolderToQueue(directory);
                }
            }

        });

        final Button clearFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        clearFilesButton.setText("Clear List");
        clearFilesButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                for (final TableItem item : swtTable.getItems()) {
                    deleteTableItem(item);
                }
            }
        });

        setupUpdateStuff(topButtonsComposite);
    }

    private void setupBottomComposite() {
        Composite bottomButtonsComposite = new Composite(shell, SWT.FILL);
        bottomButtonsComposite.setLayout(new GridLayout(3, false));

        GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        final Button quitButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData quitButtonGridData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        quitButtonGridData.minimumWidth = 70;
        quitButtonGridData.widthHint = 70;
        quitButton.setLayoutData(quitButtonGridData);
        quitButton.setText(QUIT_LABEL);
        quitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ui.quit();
            }
        });

        totalProgressBar = new ProgressBar(bottomButtonsComposite, SWT.SMOOTH);
        totalProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        actionButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData actionButtonGridData = new GridData(GridData.END, GridData.CENTER, false, false);
        actionButton.setLayoutData(actionButtonGridData);
        setActionButtonText(actionButton);
        actionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                renameFiles();
            }
        });
    }

    private void setupTableDragDrop() {
        DropTarget dt = new DropTarget(swtTable, DND.DROP_DEFAULT | DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dt.addDropListener(new DropTargetAdapter() {

            @Override
            public void drop(DropTargetEvent e) {
                FileTransfer ft = FileTransfer.getInstance();
                if (ft.isSupportedType(e.currentDataType)) {
                    String[] fileList = (String[]) e.data;
                    episodeMap.addArrayOfStringsToQueue(fileList);
                }
            }
        });
    }

    private void setupSelectionListener() {
        swtTable.addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK) {
                TableItem eventItem = (TableItem) event.item;
                // This assumes that the current status of the TableItem
                // already reflects its toggled state, which appears to
                // be the case.
                boolean checked = eventItem.getChecked();
                boolean isSelected = false;

                for (final TableItem item : swtTable.getSelection()) {
                    if (item == eventItem) {
                        isSelected = true;
                        break;
                    }
                }
                if (isSelected) {
                    for (final TableItem item : swtTable.getSelection()) {
                        item.setChecked(checked);
                    }
                } else {
                    swtTable.deselectAll();
                }
            }
            // else, it's a SELECTED event, which we just don't care about
        });
    }

    private synchronized void createColumns() {
        CHECKBOX_FIELD.createColumn(this, swtTable, WIDTH_CHECKED);
        CURRENT_FILE_FIELD.createColumn(this, swtTable, WIDTH_CURRENT_FILE);
        NEW_FILENAME_FIELD.createColumn(this, swtTable, WIDTH_NEW_FILENAME);
        STATUS_FIELD.createColumn(this, swtTable, WIDTH_STATUS);
    }

    private void setSortColumn() {
        TableColumn sortColumn = CURRENT_FILE_FIELD.getTableColumn();
        if (sortColumn == null) {
            logger.warning("could not find preferred sort column");
        } else {
            swtTable.setSortColumn(sortColumn);
            swtTable.setSortDirection(SWT.UP);
        }
    }

    private void setupResultsTable() {
        swtTable.setHeaderVisible(true);
        swtTable.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        // gridData.widthHint = 780;
        gridData.heightHint = 350;
        gridData.horizontalSpan = 3;
        swtTable.setLayoutData(gridData);

        createColumns();
        setColumnDestText();
        setSortColumn();

        // Allow deleting of elements
        swtTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if ((e.keyCode == '\u0008') // backspace
                    || (e.keyCode == '\u007F')) // delete
                {
                    deleteSelectedTableItems();
                }
            }
        });

        // editable table
        final TableEditor editor = new TableEditor(swtTable);
        editor.horizontalAlignment = SWT.CENTER;
        editor.grabHorizontal = true;

        setupSelectionListener();
    }

    private void setupMainWindow() {
        setupResultsTable();
        setupTableDragDrop();
        setupBottomComposite();

        TaskBar taskBar = display.getSystemTaskBar();
        if (taskBar != null) {
            taskItem = taskBar.getItem(shell);
            if (taskItem == null) {
                taskItem = taskBar.getItem(null);
            }
        }
    }

    ResultsTable(final UIStarter ui) {
        this.ui = ui;
        shell = ui.shell;
        display = ui.display;

        setupTopButtons();
        swtTable = new Table(shell, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        setupMainWindow();
    }
}
