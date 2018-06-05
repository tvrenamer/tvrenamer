package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
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
import org.eclipse.swt.widgets.Monitor;
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

import java.io.InputStream;
import java.text.Collator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class UIStarter implements Observer, AddEpisodeListener {
    private static final Logger logger = Logger.getLogger(UIStarter.class.getName());
    // load preferences
    private static final UserPreferences prefs = UserPreferences.getInstance();

    private static final int CHECKBOX_COLUMN = 0;
    private static final int CURRENT_FILE_COLUMN = 1;
    private static final int NEW_FILENAME_COLUMN = 2;
    private static final int STATUS_COLUMN = 3;
    private static final int ITEM_NOT_IN_TABLE = -1;

    private Shell shell;
    private Display display;

    private Button actionButton;
    private Table resultsTable;
    private ProgressBar totalProgressBar;
    private Image appIcon = null;
    private TaskItem taskItem = null;

    private boolean apiDeprecated = false;

    private final EpisodeDb episodeMap = new EpisodeDb();

    private void init() {
        prefs.addObserver(this);

        // Setup display and shell
        GridLayout shellGridLayout = new GridLayout(3, false);
        Display.setAppName(APPLICATION_NAME);
        display = new Display();

        shell = new Shell(display);

        shell.setText(APPLICATION_NAME);
        shell.setLayout(shellGridLayout);

        // Setup the util class
        UIUtils.setShell(shell);
        UIUtils.checkDestinationDirectory();

        // Add controls to main shell
        setupTopButtons();
        resultsTable = new Table(shell, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        setupMainWindow();
        setupMenuBar();

        setupIcons();

        shell.pack(true);
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
        display.dispose();
    }

    private void deleteTableItem(final TableItem item) {
        episodeMap.remove(item.getText(CURRENT_FILE_COLUMN));
        item.dispose();
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
                for (final TableItem item : resultsTable.getItems()) {
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
    }

    private void makeMenuItem(Menu parent, String text, Listener listener, char shortcut) {
        MenuItem newItem = new MenuItem(parent, SWT.PUSH);
        newItem.setText(text + "\tCtrl+" + shortcut);
        newItem.addListener(SWT.Selection, listener);
        newItem.setAccelerator(SWT.CONTROL | shortcut);
    }

    private void setupMenuBar() {
        Menu menuBarMenu = new Menu(shell, SWT.BAR);
        Menu helpMenu;

        Listener preferencesListener = e -> showPreferencesPane();
        Listener aboutListener = e -> showAboutPane();
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

    private Menu setupHelpMenuBar(Menu menuBar) {
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
        resultsTable.addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK) {
                TableItem eventItem = (TableItem) event.item;
                // This assumes that the current status of the TableItem
                // already reflects its toggled state, which appears to
                // be the case.
                boolean checked = eventItem.getChecked();
                boolean isSelected = false;

                for (final TableItem item : resultsTable.getSelection()) {
                    if (item == eventItem) {
                        isSelected = true;
                        break;
                    }
                }
                if (isSelected) {
                    for (final TableItem item : resultsTable.getSelection()) {
                        item.setChecked(checked);
                    }
                } else {
                    resultsTable.deselectAll();
                }
            }
            // else, it's a SELECTED event, which we just don't care about
        });
    }

    private void setupResultsTable() {
        resultsTable.setHeaderVisible(true);
        resultsTable.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        // gridData.widthHint = 780;
        gridData.heightHint = 350;
        gridData.horizontalSpan = 3;
        resultsTable.setLayoutData(gridData);

        final TableColumn checkboxColumn = new TableColumn(resultsTable, SWT.LEFT);
        checkboxColumn.setText(CHECKBOX_HEADER);
        checkboxColumn.setWidth(60);

        final TableColumn sourceColumn = new TableColumn(resultsTable, SWT.LEFT);
        sourceColumn.setText(SOURCE_HEADER);
        sourceColumn.setWidth(550);

        final TableColumn destinationColumn = new TableColumn(resultsTable, SWT.LEFT);
        setColumnDestText(destinationColumn);
        destinationColumn.setWidth(550);

        final TableColumn statusColumn = new TableColumn(resultsTable, SWT.LEFT);
        statusColumn.setText(STATUS_HEADER);
        statusColumn.setWidth(60);

        // Allow deleting of elements
        resultsTable.addKeyListener(new KeyAdapter() {
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

        checkboxColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int newDirection = resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN;
                resultsTable.setSortDirection(newDirection);
                sortTable(CHECKBOX_COLUMN);
                resultsTable.setSortColumn(checkboxColumn);
            }
        });

        sourceColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int newDirection = resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN;
                resultsTable.setSortDirection(newDirection);
                sortTable(CURRENT_FILE_COLUMN);
                resultsTable.setSortColumn(sourceColumn);
            }
        });

        destinationColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int newDirection = resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN;
                resultsTable.setSortDirection(newDirection);
                sortTable(NEW_FILENAME_COLUMN);
                resultsTable.setSortColumn(destinationColumn);
            }
        });

        statusColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int newDirection = resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN;
                resultsTable.setSortDirection(newDirection);
                sortTable(STATUS_COLUMN);
                resultsTable.setSortColumn(statusColumn);
            }
        });

        // editable table
        final TableEditor editor = new TableEditor(resultsTable);
        editor.horizontalAlignment = SWT.CENTER;
        editor.grabHorizontal = true;

        setupSelectionListener();
    }

    private void setupTableDragDrop() {
        DropTarget dt = new DropTarget(resultsTable, DND.DROP_DEFAULT | DND.DROP_MOVE);
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

    private void setupIcons() {
        try {
            InputStream icon = getClass().getResourceAsStream(TVRENAMER_ICON_PATH);
            if (icon != null) {
                appIcon = new Image(display, icon);
            } else {
                appIcon = new Image(display, TVRENAMER_ICON_DIRECT_PATH);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        setAppIcon();
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

    private int launch() {
        try {
            // place the window in the centre of the primary monitor
            Monitor primary = display.getPrimaryMonitor();
            Rectangle bounds = primary.getBounds();
            Rectangle rect = shell.getBounds();
            int x = bounds.x + (bounds.width - rect.width) / 2;
            int y = bounds.y + (bounds.height - rect.height) / 2;
            shell.setLocation(x, y);

            // Start the shell
            shell.pack();
            shell.open();
            resultsTable.setFocus();

            // Load the preload folder into the episode map, which will call
            // us back with the list of files once they've been loaded.
            episodeMap.subscribe(this);
            episodeMap.preload();

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
        quit();
        return rval;
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
        item.setText(NEW_FILENAME_COLUMN, ep.getReplacementText());
        if (ep.isReady()) {
            item.setChecked(true);
        } else {
            item.setChecked(false);
        }
    }

    private void failTableItem(final TableItem item) {
        item.setImage(STATUS_COLUMN, FileMoveIcon.getIcon(FAIL));
        item.setChecked(false);
    }

    private void listingsDownloaded(TableItem item, FileEpisode episode) {
        int epsFound = episode.listingsComplete();
        display.asyncExec(() -> {
            if (tableContainsTableItem(item)) {
                setProposedDestColumn(item, episode);
                if (epsFound >= 1) {
                    item.setImage(STATUS_COLUMN, FileMoveIcon.getIcon(SUCCESS));
                    item.setChecked(true);
                } else {
                    failTableItem(item);
                }
            }
        });
    }

    private void listingsFailed(TableItem item, FileEpisode episode, Exception err) {
        episode.listingsFailed(err);
        display.asyncExec(() -> {
            if (tableContainsTableItem(item)) {
                setProposedDestColumn(item, episode);
                failTableItem(item);
            }
        });
    }

    private void getSeriesListings(Series series, TableItem item, FileEpisode episode) {
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


    private void tableItemFailed(TableItem item, FileEpisode episode) {
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
    public void addEpisodes(Queue<FileEpisode> episodes) {
        for (final FileEpisode episode : episodes) {
            final String fileName = episode.getFilepath();
            final TableItem item = createTableItem(resultsTable, fileName, episode);
            synchronized (this) {
                if (apiDeprecated) {
                    tableItemFailed(item, episode);
                    continue;
                }
            }

            final String showName = episode.getFilenameShow();
            ShowStore.getShow(showName, new ShowInformationListener() {
                    @Override
                    public void downloadSucceeded(Show show) {
                        episode.setEpisodeShow(show);
                        display.asyncExec(() -> {
                            if (tableContainsTableItem(item)) {
                                setProposedDestColumn(item, episode);
                                item.setImage(STATUS_COLUMN, FileMoveIcon.getIcon(ADDED));
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

    private int getTableItemIndex(TableItem item) {
        try {
            return resultsTable.indexOf(item);
        } catch (IllegalArgumentException | SWTException ignored) {
            // We'll just fall through and return the sentinel.
        }
        return ITEM_NOT_IN_TABLE;
    }

    private boolean tableContainsTableItem(TableItem item) {
        return (ITEM_NOT_IN_TABLE != getTableItemIndex(item));
    }

    public Label getProgressLabel(TableItem item) {
        Label progressLabel = new Label(resultsTable, SWT.SHADOW_NONE | SWT.CENTER);
        TableEditor editor = new TableEditor(resultsTable);
        editor.grabHorizontal = true;
        editor.setEditor(progressLabel, item, STATUS_COLUMN);

        return progressLabel;
    }

    private void renameFiles() {
        if (!prefs.isMoveEnabled() && !prefs.isRenameEnabled()) {
            logger.info("move and rename both disabled, nothing to be done.");
            return;
        }

        final List<FileMover> pendingMoves = new LinkedList<>();
        for (final TableItem item : resultsTable.getItems()) {
            if (item.getChecked()) {
                String fileName = item.getText(CURRENT_FILE_COLUMN);
                final FileEpisode episode = episodeMap.get(fileName);
                // Skip files not successfully downloaded and ready to be moved
                if (!episode.isReady()) {
                    logger.info("selected but not ready: " + episode.getFilepath());
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
    }

    private TableItem createTableItem(Table tblResults, String fileName, FileEpisode episode) {
        TableItem item = new TableItem(tblResults, SWT.NONE);

        // Initially we add items to the table unchecked.  When we successfully obtain enough
        // information about the episode to determine how to rename it, the check box will
        // automatically be activated.
        item.setChecked(false);
        item.setText(CURRENT_FILE_COLUMN, fileName);
        setProposedDestColumn(item, episode);
        item.setImage(STATUS_COLUMN, FileMoveIcon.getIcon(DOWNLOADING));
        return item;
    }

    private void deleteSelectedTableItems() {
        for (final TableItem item : resultsTable.getSelection()) {
            int index = getTableItemIndex(item);
            deleteTableItem(item);

            if (ITEM_NOT_IN_TABLE == index) {
                logger.info("error: somehow selected item not found in table");
            }
        }
        resultsTable.deselectAll();
    }

    /**
     * Insert a copy of the row at the given position, and then delete the original row.
     * Note that insertion does not overwrite the row that is already there.  It pushes
     * the row, and every row below it, down one slot.
     *
     * @param i
     *   the index of the TableItem to copy
     * @param positionToInsert
     *   the position where we should insert the row
     */
    private void setSortedItem(final int i, final int positionToInsert) {
        TableItem oldItem = resultsTable.getItem(i);
        boolean wasChecked = oldItem.getChecked();
        int oldStyle = oldItem.getStyle();

        TableItem item = new TableItem(resultsTable, oldStyle, positionToInsert);
        item.setChecked(wasChecked);
        item.setText(CURRENT_FILE_COLUMN, oldItem.getText(CURRENT_FILE_COLUMN));
        item.setText(NEW_FILENAME_COLUMN, oldItem.getText(NEW_FILENAME_COLUMN));
        item.setImage(STATUS_COLUMN, oldItem.getImage(STATUS_COLUMN));

        oldItem.dispose();
    }

    private static String getResultsTableTextValue(TableItem[] items, int row, final int column) {
        switch (column) {
            case CHECKBOX_COLUMN:
                return (items[row].getChecked()) ? "0" : "1";
            case STATUS_COLUMN:
                return FileMoveIcon.getImagePriority(items[row].getImage(column));
            default:
                return items[row].getText(column);
        }
    }

    private void sortTable(final int columnNum) {
        int sortDirection = resultsTable.getSortDirection();
        // Get the items
        TableItem[] items = resultsTable.getItems();
        Collator collator = Collator.getInstance(Locale.getDefault());

        // Go through the item list and bubble rows up to the top as appropriate
        for (int i = 1; i < items.length; i++) {
            String value1 = getResultsTableTextValue(items, i, columnNum);
            for (int j = 0; j < i; j++) {
                String value2 = getResultsTableTextValue(items, j, columnNum);
                // Compare the two values and order accordingly
                int comparison = collator.compare(value1, value2);
                if (((comparison < 0) && (sortDirection == SWT.UP))
                    || (comparison > 0) && (sortDirection == SWT.DOWN))
                {
                    // Insert a copy of row i at position j, and then delete
                    // row i.  Then fetch the list of items anew, since we
                    // just modified it.
                    setSortedItem(i, j);
                    items = resultsTable.getItems();
                    break;
                }
            }
        }
    }

    public void refreshAll() {
        logger.info("Refreshing table");
        for (TableItem item : resultsTable.getItems()) {
            String fileName = item.getText(CURRENT_FILE_COLUMN);
            FileEpisode episode = episodeMap.remove(fileName);
            episode.refreshReplacement();
            String newFileName = episode.getFilepath();
            episodeMap.put(newFileName, episode);
            item.setText(CURRENT_FILE_COLUMN, newFileName);
            setProposedDestColumn(item, episode);
        }
    }

    void finishAllMoves() {
        setAppIcon();
        refreshAll();
    }

    private void setActionButtonText(Button b) {
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

    private void updateUserPreferences(UserPreferences observed, UserPreference userPref) {
        logger.info("Preference change event: " + userPref);

        if ((userPref == UserPreference.MOVE_ENABLED)
            || (userPref == UserPreference.RENAME_ENABLED))
        {
            setColumnDestText(resultsTable.getColumn(NEW_FILENAME_COLUMN));
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
    public void update(Observable observable, Object value) {
        if (observable instanceof UserPreferences && value instanceof UserPreference) {
            updateUserPreferences((UserPreferences) observable,
                                  (UserPreference) value);
        }
    }

    private void showPreferencesPane() {
        PreferencesDialog preferencesDialog = new PreferencesDialog(shell);
        preferencesDialog.open();
    }

    /**
     * Create the 'About' dialog.
     */
    private void showAboutPane() {
        AboutDialog aboutDialog = new AboutDialog(shell);
        aboutDialog.open();
    }
}
