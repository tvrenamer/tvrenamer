package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
import static org.tvrenamer.view.Columns.*;
import static org.tvrenamer.view.FileMoveIcon.Status.*;
import static org.tvrenamer.view.UIUtils.showMessageBox;

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
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
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
import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UserPreference;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.model.util.Environment;

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

    private final UIStarter ui;
    private final Shell shell;
    private final Display display;
    private final Table swtTable;
    private final Image appIcon;
    private final EpisodeDb episodeMap = new EpisodeDb();

    private Button actionButton;
    private ProgressBar totalProgressBar;
    private TaskItem taskItem = null;

    private boolean apiDeprecated = false;

    void ready() {
        prefs.addObserver(this);
        swtTable.setFocus();

        // Load the preload folder into the episode map, which will call
        // us back with the list of files once they've been loaded.
        episodeMap.subscribe(this);
        episodeMap.preload();
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

    private void quit() {
        shell.dispose();
    }

    private int getTableItemIndex(final TableItem item) {
        try {
            return swtTable.indexOf(item);
        } catch (IllegalArgumentException | SWTException ignored) {
            // We'll just fall through and return the sentinel.
        }
        return ITEM_NOT_IN_TABLE;
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

    private void deleteTableItem(final TableItem item) {
        deleteItemCombo(item);
        episodeMap.remove(getCellText(item, CURRENT_FILE_COLUMN));
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
                quit();
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

    private void setAppIcon() {
        if (appIcon == null) {
            logger.warning("unable to get application icon");
        } else {
            shell.setImage(appIcon);
        }
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

        setAppIcon();
    }

    private void makeMenuItem(final Menu parent, final String text,
                              final Listener listener, final char shortcut)
    {
        MenuItem newItem = new MenuItem(parent, SWT.PUSH);
        newItem.setText(text + "\tCtrl+" + shortcut);
        newItem.addListener(SWT.Selection, listener);
        newItem.setAccelerator(SWT.CONTROL | shortcut);
    }

    private void setupMenuBar() {
        Menu menuBarMenu = new Menu(shell, SWT.BAR);
        Menu helpMenu;

        Listener preferencesListener = e -> {
            PreferencesDialog preferencesDialog = new PreferencesDialog(shell);
            preferencesDialog.open();
        };
        Listener aboutListener = e -> {
            AboutDialog aboutDialog = new AboutDialog(ui);
            aboutDialog.open();
        };
        Listener quitListener = e -> quit();

        if (Environment.IS_MAC_OSX) {
            // Add the special Mac OSX Preferences, About and Quit menus.
            CocoaUIEnhancer enhancer = new CocoaUIEnhancer(APPLICATION_NAME);
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

    public void finishMove(final TableItem item, final boolean success) {
        if (success) {
            if (prefs.isDeleteRowAfterMove()) {
                deleteTableItem(item);
            }
        } else {
            logger.info("failed to move item: " + item);
        }
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

    private void setupResultsTable() {
        swtTable.setHeaderVisible(true);
        swtTable.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        // gridData.widthHint = 780;
        gridData.heightHint = 350;
        gridData.horizontalSpan = 3;
        swtTable.setLayoutData(gridData);

        Columns.createColumns(this, swtTable);
        setColumnDestText(swtTable.getColumn(NEW_FILENAME_COLUMN));
        swtTable.setSortColumn(swtTable.getColumn(CURRENT_FILE_COLUMN));
        swtTable.setSortDirection(SWT.UP);

        // Allow deleting of elements
        swtTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);

                switch (e.keyCode) {

                    // backspace
                    case '\u0008':
                    // delete
                    case '\u007F':
                        deleteSelectedTableItems();
                        break;

                    // Code analysis says have a default clause...
                    default:
                }

            }
        });

        // editable table
        final TableEditor editor = new TableEditor(swtTable);
        editor.horizontalAlignment = SWT.CENTER;
        editor.grabHorizontal = true;

        setupSelectionListener();
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

    Display getDisplay() {
        return display;
    }

    ProgressBar getProgressBar() {
        return totalProgressBar;
    }

    TaskItem getTaskItem() {
        return taskItem;
    }

    private static String getCellStatusString(final TableItem item, final int columnId) {
        return FileMoveIcon.getImagePriority(item.getImage(columnId));
    }

    private static Image getCellImage(final TableItem item, final int columnId) {
        return item.getImage(columnId);
    }

    private static void setCellImage(final TableItem item,
                                     final int columnId,
                                     final Image newImage)
    {
        item.setImage(columnId, newImage);
    }

    private static void setCellImage(final TableItem item,
                                     final int columnId,
                                     final FileMoveIcon.Status newStatus)
    {
        item.setImage(columnId, FileMoveIcon.getIcon(newStatus));
    }

    private static String getCellText(final TableItem item, final int columnId) {
        return item.getText(columnId);
    }

    private static void setCellText(final TableItem item,
                                    final int columnId,
                                    final String newText)
    {
        item.setText(columnId, newText);
    }

    private static void setEditor(final TableItem item,
                                  final int columnId,
                                  final TableEditor editor,
                                  final Control control)
    {
        editor.setEditor(control, item, columnId);
    }

    private void setComboBoxProposedDest(final TableItem item, final FileEpisode ep) {
        final List<String> options = ep.getReplacementOptions();
        final int chosen = ep.getChosenEpisode();
        final String defaultOption = options.get(chosen);
        setCellText(item, NEW_FILENAME_COLUMN, defaultOption);

        final Combo combo = new Combo(swtTable, SWT.DROP_DOWN | SWT.READ_ONLY);
        options.forEach(combo::add);
        combo.setText(defaultOption);
        combo.addModifyListener(e -> ep.setChosenEpisode(combo.getSelectionIndex()));
        item.setData(combo);

        final TableEditor editor = new TableEditor(swtTable);
        editor.grabHorizontal = true;
        setEditor(item, NEW_FILENAME_COLUMN, editor, combo);
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
        deleteItemCombo(item);

        int nOptions = ep.optionCount();
        if (nOptions > 1) {
            setComboBoxProposedDest(item, ep);
        } else if (nOptions == 1) {
            setCellText(item, NEW_FILENAME_COLUMN, ep.getReplacementText());
        } else {
            setCellText(item, NEW_FILENAME_COLUMN, ep.getReplacementText());
            item.setChecked(false);
        }
    }

    private void failTableItem(final TableItem item) {
        setCellImage(item, STATUS_COLUMN, FAIL);
        item.setChecked(false);
    }

    private void setTableItemStatus(final TableItem item, final int epsFound) {
        if (epsFound > 1) {
            setCellImage(item, STATUS_COLUMN, OPTIONS);
            item.setChecked(true);
        } else if (epsFound == 1) {
            setCellImage(item, STATUS_COLUMN, SUCCESS);
            item.setChecked(true);
        } else {
            failTableItem(item);
        }
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
            showMessageBox(SWTMessageBoxType.ERROR, ERROR_LABEL,
                           updateIsAvailable ? GET_UPDATE_MESSAGE : NEED_UPDATE);
        }
    }

    @Override
    public void addEpisodes(final Queue<FileEpisode> episodes) {
        for (final FileEpisode episode : episodes) {
            final String fileName = episode.getFilepath();
            final TableItem item = createTableItem(swtTable, fileName, episode);
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
            ShowStore.getShow(showName, new ShowInformationListener() {
                    @Override
                    public void downloadSucceeded(Show show) {
                        episode.setEpisodeShow(show);
                        display.asyncExec(() -> {
                            if (tableContainsTableItem(item)) {
                                setProposedDestColumn(item, episode);
                                setCellImage(item, STATUS_COLUMN, ADDED);
                            }
                        });
                        if (show.isValidSeries()) {
                            getSeriesListings(show.asSeries(), item, episode);
                        }
                    }

                    @Override
                    public void downloadFailed(FailedShow failedShow) {
                        // We don't send a FailedShow to the FileEpisode
                        episode.setEpisodeShow(null);
                        tableItemFailed(item, episode);
                    }

                    @Override
                    public void apiHasBeenDeprecated() {
                        noteApiFailure();
                        tableItemFailed(item, episode);
                    }
                });
        }
    }

    private boolean tableContainsTableItem(final TableItem item) {
        return (ITEM_NOT_IN_TABLE != getTableItemIndex(item));
    }

    public Label getProgressLabel(final TableItem item) {
        Label progressLabel = new Label(swtTable, SWT.SHADOW_NONE | SWT.CENTER);
        TableEditor editor = new TableEditor(swtTable);
        editor.grabHorizontal = true;
        setEditor(item, STATUS_COLUMN, editor, progressLabel);

        return progressLabel;
    }

    private void renameFiles() {
        if (!prefs.isMoveEnabled() && !prefs.isRenameEnabled()) {
            logger.info("move and rename both disabled, nothing to be done.");
            return;
        }

        final List<FileMover> pendingMoves = new LinkedList<>();
        for (final TableItem item : swtTable.getItems()) {
            if (item.getChecked()) {
                String fileName = getCellText(item, CURRENT_FILE_COLUMN);
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

    private TableItem createTableItem(final Table tblResults, final String fileName,
                                      final FileEpisode episode)
    {
        TableItem item = new TableItem(tblResults, SWT.NONE);

        // Initially we add items to the table unchecked.  When we successfully obtain enough
        // information about the episode to determine how to rename it, the check box will
        // automatically be activated.
        item.setChecked(false);
        setCellText(item, CURRENT_FILE_COLUMN, fileName);
        setProposedDestColumn(item, episode);
        setCellImage(item, STATUS_COLUMN, DOWNLOADING);
        return item;
    }

    private static String itemDestDisplayedText(final TableItem item) {
        synchronized (item) {
            final Object data = item.getData();
            if (data == null) {
                return getCellText(item, NEW_FILENAME_COLUMN);
            }
            final Combo combo = (Combo) data;
            final int selected = combo.getSelectionIndex();
            final String[] options = combo.getItems();
            return options[selected];
        }
    }

    private static String getItemTextValue(final TableItem item, final int column) {
        switch (column) {
            case CHECKBOX_COLUMN:
                return (item.getChecked()) ? "0" : "1";
            case STATUS_COLUMN:
                return getCellStatusString(item, column);
            case NEW_FILENAME_COLUMN:
                return itemDestDisplayedText(item);
            default:
                return getCellText(item, column);
        }
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
        int oldStyle = oldItem.getStyle();

        TableItem item = new TableItem(swtTable, oldStyle, positionToInsert);
        item.setChecked(wasChecked);
        setCellText(item, CURRENT_FILE_COLUMN, getCellText(oldItem, CURRENT_FILE_COLUMN));
        setCellText(item, NEW_FILENAME_COLUMN, getCellText(oldItem, NEW_FILENAME_COLUMN));
        setCellImage(item, STATUS_COLUMN, getCellImage(oldItem, STATUS_COLUMN));

        final Object itemData = oldItem.getData();

        // Although the name suggests dispose() is primarily about reclaiming system
        // resources, it also deletes the item from the Table.
        oldItem.dispose();
        if (itemData != null) {
            final TableEditor newEditor = new TableEditor(swtTable);
            newEditor.grabHorizontal = true;
            setEditor(item, NEW_FILENAME_COLUMN, newEditor, (Control) itemData);
            item.setData(itemData);
        }
    }

    /**
     * Sort the table by the given column in the given direction.
     *
     * @param column
     *    the TableColumn to sort by
     * @param columnNum
     *    the position of the TableColumn in the Table
     * @param sortDirection
     *    the direction to sort by; SWT.UP means sort A-Z, while SWT.DOWN is Z-A
     */
    private void sortTable(final TableColumn column, final int columnNum,
                           final int sortDirection)
    {
        // Get the items
        TableItem[] items = swtTable.getItems();

        // Go through the item list and bubble rows up to the top as appropriate
        for (int i = 1; i < items.length; i++) {
            String value1 = getItemTextValue(items[i], columnNum);
            for (int j = 0; j < i; j++) {
                String value2 = getItemTextValue(items[j], columnNum);
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
        swtTable.setSortColumn(column);
    }

    /**
     * Sort the table by the given column.
     *
     * If the column to sort by is the same column that the table is already
     * sorted by, then the effect is to reverse the ordering of the sort.
     *
     * @param column
     *    the TableColumn to sort by
     */
    void sortTable(final TableColumn column) {
        final int columnNum = swtTable.indexOf(column);
        if (ITEM_NOT_IN_TABLE == columnNum) {
            logger.severe("unable to locate column in table: " + column);
            return;
        }
        int sortDirection = SWT.UP;
        TableColumn previousSort = swtTable.getSortColumn();
        if (column.equals(previousSort)) {
            sortDirection = swtTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN;
        }
        sortTable(column, columnNum, sortDirection);
    }

    public void refreshAll() {
        logger.info("Refreshing table");
        for (TableItem item : swtTable.getItems()) {
            String fileName = getCellText(item, CURRENT_FILE_COLUMN);
            FileEpisode episode = episodeMap.remove(fileName);
            episode.refreshReplacement();
            String newFileName = episode.getFilepath();
            episodeMap.put(newFileName, episode);
            setCellText(item, CURRENT_FILE_COLUMN, newFileName);
            setProposedDestColumn(item, episode);
            setTableItemStatus(item, episode.optionCount());
        }
    }

    void finishAllMoves() {
        setAppIcon();
        refreshAll();
    }

    private void setActionButtonText(final Button b) {
        String label = RENAME_LABEL;
        String tooltip = RENAME_TOOLTIP;

        if (prefs.isMoveEnabled()) {
            tooltip = INTRO_MOVE_DIR + prefs.getDestinationDirectoryName()
                + FINISH_MOVE_DIR;
            if (prefs.isRenameEnabled()) {
                label = RENAME_AND_MOVE;
                tooltip = MOVE_INTRO + AND_RENAME + tooltip;
            } else {
                label = JUST_MOVE_LABEL;
                tooltip = MOVE_INTRO + tooltip;
            }
        } else if (!prefs.isRenameEnabled()) {
            // This setting, "do not move and do not rename", really makes no sense.
            // But for now, we're not taking the effort to explicitly disable it.
            tooltip = NO_ACTION_TOOLTIP;
        }

        b.setText(label);
        b.setToolTipText(tooltip);
        shell.changed(new Control[] {b});
        shell.layout(false, true);
    }

    private void setColumnDestText(final TableColumn destinationColumn) {
        if (prefs.isMoveEnabled()) {
            destinationColumn.setText(MOVE_HEADER);
        } else {
            destinationColumn.setText(RENAME_HEADER);
        }
    }

    private void updateUserPreferences(final UserPreferences observed,
                                       final UserPreference userPref)
    {
        logger.info("Preference change event: " + userPref);

        if ((userPref == UserPreference.MOVE_ENABLED)
            || (userPref == UserPreference.RENAME_ENABLED))
        {
            setColumnDestText(swtTable.getColumn(NEW_FILENAME_COLUMN));
            setActionButtonText(actionButton);
        }
        if ((userPref == UserPreference.REPLACEMENT_MASK)
            || (userPref == UserPreference.MOVE_ENABLED)
            || (userPref == UserPreference.RENAME_ENABLED)
            || (userPref == UserPreference.DEST_DIR)
            || (userPref == UserPreference.SEASON_PREFIX)
            || (userPref == UserPreference.LEADING_ZERO))
        {
            refreshAll();
        }

        if (userPref == UserPreference.DEST_DIR) {
            UIUtils.checkDestinationDirectory();
        }
    }

    /* (non-Javadoc)
     * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @Override
    public void update(final Observable observable, final Object value) {
        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            updateUserPreferences((UserPreferences) observable,
                                  (UserPreference) value);
        }
    }

    ResultsTable(final UIStarter ui) {
        this.ui = ui;
        shell = ui.shell;
        display = ui.display;
        appIcon = UIUtils.readImageFromPath(TVRENAMER_ICON_PATH);

        setupTopButtons();
        swtTable = new Table(shell, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        setupMainWindow();
        setupMenuBar();
    }
}
