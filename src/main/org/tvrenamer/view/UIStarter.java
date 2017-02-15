package org.tvrenamer.view;

import static org.tvrenamer.view.UIUtils.getOSType;
import static org.tvrenamer.view.UIUtils.showMessageBox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
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
import org.eclipse.swt.widgets.Text;

import org.tvrenamer.controller.FileMover;
import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TVRenamer;
import org.tvrenamer.controller.UpdateChecker;
import org.tvrenamer.controller.UpdateCompleteHandler;
import org.tvrenamer.model.EpisodeStatus;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.FileMoveIcon;
import org.tvrenamer.model.NotFoundException;
import org.tvrenamer.model.SWTMessageBoxType;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.UserPreferences;
import org.tvrenamer.model.util.Constants;
import org.tvrenamer.model.util.Constants.OSType;

public class UIStarter {
    private static final String DOWNLOADING_FAILED_MESSAGE = "Downloading show listings failed.  Check internet connection";
    private static Logger logger = Logger.getLogger(UIStarter.class.getName());
    private static final int SELECTED_COLUMN = 0;
    private static final int CURRENT_FILE_COLUMN = 1;
    private static final int NEW_FILENAME_COLUMN = 2;
    private static final int STATUS_COLUMN = 3;
    private static final int ITEM_NOT_IN_TABLE = -1;
    private static final String TVRENAMER_DOWNLOAD_URL = AboutDialog.TVRENAMER_PROJECT_URL + "/downloads";

    private static Shell shell;
    private Display display;
    private static UserPreferences prefs;
    private static List<String> ignoreKeywords;

    private Button addFilesButton;
    private Button addFolderButton;
    private Button clearFilesButton;
    private Link updatesAvailableLink;
    private static Button renameSelectedButton;
    private static TableColumn destinationColumn;
    private Table resultsTable;
    private ProgressBar totalProgressBar;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private Map<String, FileEpisode> files = new HashMap<String, FileEpisode>();

    // Static initalisation block
    static {
        // Find logging.properties file inside jar
        InputStream loggingConfigStream = UIStarter.class.getResourceAsStream("/logging.properties");

        if (loggingConfigStream != null) {
            try {
                LogManager.getLogManager().readConfiguration(loggingConfigStream);
            } catch (IOException e) {
                System.err.println("Exception thrown while loading logging config");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        UIStarter ui = new UIStarter();
        ui.init();
        ui.launch();
    }

    private void init() {
        // load preferences
        prefs = UserPreferences.getInstance();

        // Setup display and shell
        GridLayout shellGridLayout = new GridLayout(3, false);
        Display.setAppName(Constants.APPLICATION_NAME);
        display = new Display();

        shell = new Shell(display);

        shell.setText(Constants.APPLICATION_NAME);
        shell.setLayout(shellGridLayout);

        // Setup the util class
        new UIUtils(shell);

        // Add controls to main shell
        setupMainWindow();
        setupAddFilesDialog();
        setupClearFilesButton();
        setupMenuBar();

        setupIcons();

        shell.pack(true);
    }

    private void setupMainWindow() {
        final Composite topButtonsComposite = new Composite(shell, SWT.FILL);
        topButtonsComposite.setLayout(new RowLayout());

        addFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        addFilesButton.setText("Add files");

        addFolderButton = new Button(topButtonsComposite, SWT.PUSH);
        addFolderButton.setText("Add Folder");

        clearFilesButton = new Button(topButtonsComposite, SWT.PUSH);
        clearFilesButton.setText("Clear List");

        updatesAvailableLink = new Link(topButtonsComposite, SWT.VERTICAL);
        //updatesAvailableLink.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, true));
        updatesAvailableLink.setVisible(false);
        updatesAvailableLink.setText("There is an update available. <a href=\"" + TVRENAMER_DOWNLOAD_URL
            + "\">Click here to download</a>");
        updatesAvailableLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                Program.launch(TVRENAMER_DOWNLOAD_URL);
            }
        });

        // Show the label if updates are available (in a new thread)
        Thread updateCheckThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (prefs.checkForUpdates()) {
                    final boolean updatesAvailable = UpdateChecker.isUpdateAvailable();

                    if (updatesAvailable) {
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                updatesAvailableLink.setVisible(updatesAvailable);
                            }
                        });
                    }
                }
            }
        });
        updateCheckThread.start();

        setupResultsTable();
        setupTableDragDrop();

        Composite bottomButtonsComposite = new Composite(shell, SWT.FILL);
        bottomButtonsComposite.setLayout(new GridLayout(3, false));
        GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
        bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

        final Button quitButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData quitButtonGridData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
        quitButtonGridData.minimumWidth = 70;
        quitButtonGridData.widthHint = 70;
        quitButton.setLayoutData(quitButtonGridData);
        quitButton.setText("Quit");

        totalProgressBar = new ProgressBar(bottomButtonsComposite, SWT.SMOOTH);
        totalProgressBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

        renameSelectedButton = new Button(bottomButtonsComposite, SWT.PUSH);
        GridData renameSelectedButtonGridData = new GridData(GridData.END, GridData.CENTER, false, false);
        renameSelectedButton.setLayoutData(renameSelectedButtonGridData);

        if (prefs != null && prefs.isMovedEnabled()) {
            setupMoveButtonText();
        } else {
            setupRenameButtonText();
        }

        renameSelectedButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                renameFiles();
            }
        });

        quitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doCleanup();
            }
        });
    }

    private void doCleanup() {
        executor.shutdownNow();
        ShowStore.cleanUp();
        shell.dispose();
        display.dispose();
    }

    private void setupMoveButtonText() {
        setRenameButtonText();
        renameSelectedButton
            .setToolTipText("Clicking this button will rename and move the selected files to the directory set in preferences (currently "
                + prefs.getDestinationDirectory().getAbsolutePath() + ").");
    }

    private void setupRenameButtonText() {
        setRenameButtonText();
        renameSelectedButton
            .setToolTipText("Clicking this button will rename the selected files but leave them where they are.");
    }

    private void setupMenuBar() {
        Menu menuBarMenu = new Menu(shell, SWT.BAR);
        Menu helpMenu;

        Listener preferencesListener = new Listener() {
            @Override
            public void handleEvent(Event e) {
                showPreferencesPane();
            }
        };

        Listener aboutListener = new Listener() {
            @Override
            public void handleEvent(Event e) {
                showAboutPane();
            }
        };

        Listener quitListener = new Listener() {
            @Override
            public void handleEvent(Event e) {
                doCleanup();
            }
        };

        if (getOSType() == OSType.MAC) {
            // Add the special Mac OSX Preferences, About and Quit menus.
            CocoaUIEnhancer enhancer = new CocoaUIEnhancer(Constants.APPLICATION_NAME);
            enhancer.hookApplicationMenu(display, quitListener, aboutListener, preferencesListener);

            setupHelpMenuBar(menuBarMenu);
        } else {
            // Add the normal Preferences, About and Quit menus.
            MenuItem fileMenuItem = new MenuItem(menuBarMenu, SWT.CASCADE);
            fileMenuItem.setText("File");

            Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
            fileMenuItem.setMenu(fileMenu);

            MenuItem filePreferencesItem = new MenuItem(fileMenu, SWT.PUSH);
            filePreferencesItem.setText("Preferences");
            filePreferencesItem.addListener(SWT.Selection, preferencesListener);

            MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
            fileExitItem.setText("Exit");
            fileExitItem.addListener(SWT.Selection, quitListener);

            helpMenu = setupHelpMenuBar(menuBarMenu);

            // The About item is added to the OSX bar, so we need to add it manually here
            MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
            helpAboutItem.setText("About");
            helpAboutItem.addListener(SWT.Selection, aboutListener);
        }

        shell.setMenuBar(menuBarMenu);
    }

    private Menu setupHelpMenuBar(Menu menuBar) {
        MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
        helpMenuHeader.setText("Help");

        Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
        helpMenuHeader.setMenu(helpMenu);

        MenuItem helpHelpItem = new MenuItem(helpMenu, SWT.PUSH);
        helpHelpItem.setText("Help");

        MenuItem helpVisitWebpageItem = new MenuItem(helpMenu, SWT.PUSH);
        helpVisitWebpageItem.setText("Visit Webpage");
        helpVisitWebpageItem.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                Program.launch(AboutDialog.TVRENAMER_PROJECT_URL);
            }
        });

        return helpMenu;
    }

    private void setupAddFilesDialog() {
        final FileDialog fd = new FileDialog(shell, SWT.MULTI);
        addFilesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String pathPrefix = fd.open();
                if (pathPrefix != null) {
                    File file = new File(pathPrefix);
                    pathPrefix = file.getParent();

                    String[] fileNames = fd.getFileNames();
                    for (int i = 0; i < fileNames.length; i++) {
                        fileNames[i] = pathPrefix + File.separatorChar + fileNames[i];
                    }

                    initiateRenamer(fileNames);
                }
            }
        });

        final DirectoryDialog dd = new DirectoryDialog(shell, SWT.SINGLE);
        addFolderButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                String directory = dd.open();
                if (directory != null) {
                    // load all of the files in the dir
                    File file = new File(directory);
                    String[] fileNames = file.list();

                    if (fileNames != null) {

                        // Check we are not recursive
                        boolean includeDirs = prefs.isRecursivelyAddFolders();
                        List<String> subDirs = new ArrayList<String>();

                        for (int i = 0; i < fileNames.length; i++) {
                            String path = directory + File.separatorChar + fileNames[i];

                            // Store the list of directories
                            if (new File(path).isDirectory()) {
                                subDirs.add(path);
                            }

                            // update the fileName value
                            fileNames[i] = path;
                        }

                        if (!includeDirs) {
                            for (String subDir : subDirs) {
                                for (int i = 0; i < fileNames.length; i++) {
                                    if (fileNames[i].startsWith(subDir)) {
                                        // A safe way of removing the file name
                                        fileNames[i] = "";
                                    }
                                }
                            }
                        }

                        initiateRenamer(fileNames);
                    }

                }
            }

        });
    }

    private void setupClearFilesButton() {

        clearFilesButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                resultsTable.removeAll();
            }
        });
    }

    private void setupSelectionListener() {
        resultsTable.addListener(SWT.Selection,
            new Listener() {
                public void handleEvent(Event event) {
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
                }
            });
    }

    private void setupResultsTable() {
        resultsTable = new Table(shell, SWT.CHECK | SWT.FULL_SELECTION | SWT.MULTI);
        resultsTable.setHeaderVisible(true);
        resultsTable.setLinesVisible(true);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        // gridData.widthHint = 780;
        gridData.heightHint = 350;
        gridData.horizontalSpan = 3;
        resultsTable.setLayoutData(gridData);

        final TableColumn selectedColumn = new TableColumn(resultsTable, SWT.LEFT);
        selectedColumn.setText("Selected");
        selectedColumn.setWidth(60);

        final TableColumn sourceColum = new TableColumn(resultsTable, SWT.LEFT);
        sourceColum.setText("Current File");
        sourceColum.setWidth(550);

        destinationColumn = new TableColumn(resultsTable, SWT.LEFT);
        setColumnDestText();
        destinationColumn.setWidth(550);

        final TableColumn statusColumn = new TableColumn(resultsTable, SWT.LEFT);
        statusColumn.setText("Status");
        statusColumn.setWidth(60);

        // Allow deleting of elements
        resultsTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);

                switch (e.keyCode) {

                    // backspace
                    case '\u0008':
                        deleteSelectedTableItems();
                        break;

                    // delete
                    case '\u007F':
                        deleteSelectedTableItems();
                        break;
                }

            }
        });

        selectedColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.setSortDirection(resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
                sortTable(selectedColumn, SELECTED_COLUMN);
                resultsTable.setSortColumn(selectedColumn);
            }
        });

        sourceColum.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.setSortDirection(resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
                sortTable(sourceColum, CURRENT_FILE_COLUMN);
                resultsTable.setSortColumn(sourceColum);
            }
        });

        destinationColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.setSortDirection(resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
                sortTable(destinationColumn, NEW_FILENAME_COLUMN);
                resultsTable.setSortColumn(destinationColumn);
            }
        });

        statusColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.setSortDirection(resultsTable.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
                sortTable(statusColumn, STATUS_COLUMN);
                resultsTable.setSortColumn(statusColumn);
            }
        });

        // editable table
        final TableEditor editor = new TableEditor(resultsTable);
        editor.horizontalAlignment = SWT.CENTER;
        editor.grabHorizontal = true;

        @SuppressWarnings("unused")
        Listener tblEditListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                Rectangle clientArea = resultsTable.getClientArea();
                Point pt = new Point(event.x, event.y);
                int index = resultsTable.getTopIndex();
                while (index < resultsTable.getItemCount()) {
                    boolean visible = false;
                    final TableItem item = resultsTable.getItem(index);
                    for (int i = 0; i < resultsTable.getColumnCount(); i++) {
                        Rectangle rect = item.getBounds(i);
                        if (rect.contains(pt)) {
                            final int column = i;
                            final Text text = new Text(resultsTable, SWT.NONE);
                            Listener textListener = new Listener() {
                                @Override
                                @SuppressWarnings("fallthrough")
                                public void handleEvent(final Event e) {
                                    switch (e.type) {
                                        case SWT.FocusOut:
                                            item.setText(column, text.getText());
                                            text.dispose();
                                            break;
                                        case SWT.Traverse:
                                            switch (e.detail) {
                                                case SWT.TRAVERSE_RETURN:
                                                    item.setText(column, text.getText());
                                                    // fall through
                                                case SWT.TRAVERSE_ESCAPE:
                                                    text.dispose();
                                                    e.doit = false;
                                            }
                                            break;
                                    }
                                }
                            };
                            text.addListener(SWT.FocusOut, textListener);
                            text.addListener(SWT.FocusIn, textListener);
                            editor.setEditor(text, item, i);
                            text.setText(item.getText(i));
                            text.selectAll();
                            text.setFocus();
                            return;
                        }
                        if (!visible && rect.intersects(clientArea)) {
                            visible = true;
                        }
                    }
                    if (!visible) {
                        return;
                    }
                    index++;
                }
            }
        };
        //resultsTable.addListener(SWT.MouseDown, tblEditListener);
        setupSelectionListener();
    }

    private void setupTableDragDrop() {
        DropTarget dt = new DropTarget(resultsTable, DND.DROP_DEFAULT | DND.DROP_MOVE);
        dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dt.addDropListener(new DropTargetAdapter() {

            @Override
            public void drop(DropTargetEvent e) {
                String[] fileList = null;
                FileTransfer ft = FileTransfer.getInstance();
                if (ft.isSupportedType(e.currentDataType)) {
                    fileList = (String[]) e.data;
                    initiateRenamer(fileList);
                }
            }
        });
    }

    private void setupIcons() {
        try {
            InputStream icon = getClass().getResourceAsStream("/icons/tvrenamer.png");
            if (icon != null) {
                shell.setImage(new Image(display, icon));
            } else {
                shell.setImage(new Image(display, "res/icons/tvrenamer.png"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TaskItem getTaskItem() {
        TaskItem taskItem = null;
        TaskBar taskBar = display.getSystemTaskBar();
        if (taskBar != null) {
            taskItem = taskBar.getItem(shell);
            if (taskItem == null) {
                taskItem = taskBar.getItem(null);
            }
        }
        return taskItem;
    }

    private void launch() {
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

            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            doCleanup();
        } catch (IllegalArgumentException argumentException) {
            String message = "Drag and Drop is not currently supported on your operating system, please use the 'Browse Files' option above";
            logger.log(Level.SEVERE, message, argumentException);
            JOptionPane.showMessageDialog(null, message);
            System.exit(1);
        } catch (Exception exception) {
            String message = "An error occurred, please check your internet connection, java version or run from the command line to show errors";
            showMessageBox(SWTMessageBoxType.ERROR, "Error", message, exception);
            logger.log(Level.SEVERE, message, exception);
            System.exit(1);
        }
    }

    private void initiateRenamer(final String[] fileNames) {
        final List<String> files = new LinkedList<String>();
        for (final String fileName : fileNames) {
            File f = new File(fileName);
            new FileTraversal() {
                @Override
                public void onFile(File f) {
                    // Don't add hidden files - defect 38
                    if (!f.isHidden()) {
                        files.add(f.getAbsolutePath());
                    }
                }
            }.traverse(f);
        }
        addFiles(files);
    }

    // class adopted from http://vafer.org/blog/20071112204524
    public abstract class FileTraversal {
        public final void traverse(final File f) {
            if (f.isDirectory()) {
                onDirectory(f);
                final File[] children = f.listFiles();
                for (File child : children) {
                    traverse(child);
                }
                return;
            }
            onFile(f);
        }

        public void onDirectory(final File d) {

        }

        public void onFile(final File f) {

        }
    }

    private void addFiles(final List<String> fileNames) {
        // Update the list of ignored keywords
        ignoreKeywords = prefs.getIgnoreKeywords();

        for (final String fileName : fileNames) {
            final FileEpisode episode = TVRenamer.parseFilename(fileName);
            if (episode == null) {
                logger.severe("Couldn't parse file: " + fileName);
            } else {
                String showName = episode.getShowName();

                files.put(fileName, episode);
                final TableItem item = createTableItem(resultsTable, fileName, episode);

                ShowStore.getShow(showName, new ShowInformationListener() {
                    @Override
                    public void downloaded(Show show) {
                        episode.setStatus(EpisodeStatus.DOWNLOADED);
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if ( tableContainsTableItem(item) ) {
                                    item.setText(NEW_FILENAME_COLUMN, episode.getNewFilePath());
                                    item.setImage(STATUS_COLUMN, FileMoveIcon.ADDED.icon);
                                }
                            }
                        });
                    }
                    @Override
                    public void downloadFailed(Show show) {
                        episode.setStatus(EpisodeStatus.BROKEN);
                        display.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if ( tableContainsTableItem(item) ) {
                                    item.setText(NEW_FILENAME_COLUMN, DOWNLOADING_FAILED_MESSAGE);
                                    item.setImage(STATUS_COLUMN, FileMoveIcon.FAIL.icon);
                                    item.setChecked(false);
                                }
                            }
                        });
                    }
                });
            }
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

    private void renameFiles() {
        final Queue<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
        int count = 0;

        for (final TableItem item : resultsTable.getItems()) {
            if (item.getChecked()) {
                count++;
                String fileName = item.getText(CURRENT_FILE_COLUMN);
                final File currentFile = new File(fileName);
                final FileEpisode episode = files.get(fileName);
                String currentName = currentFile.getName();
                String newName = item.getText(NEW_FILENAME_COLUMN);

                // Skip files not successfully downloaded
                if (episode.getStatus() != EpisodeStatus.DOWNLOADED) {
                    continue;
                }

                File newFile = null;

                if (prefs != null && prefs.isMovedEnabled()) {
                    // If move is enabled, the full path is in the table already
                    newFile = new File(newName);
                } else {
                    // Else we need to build it
                    String newFilePath = currentFile.getParent() + File.separatorChar + newName;
                    newFile = new File(newFilePath);
                }

                logger.info("Going to move '" + currentFile.getAbsolutePath() + "' to '" + newFile.getAbsolutePath()
                    + "'");

                if (newFile.exists() && !newName.equals(currentName)) {
                    String message = "File " + newFile + " already exists.\n" + currentFile + " was not renamed!";
                    showMessageBox(SWTMessageBoxType.ERROR, "Rename Failed", message);
                } else {
                    // progress label
                    TableEditor editor = new TableEditor(resultsTable);
                    final Label progressLabel = new Label(resultsTable, SWT.SHADOW_NONE | SWT.CENTER);
                    editor.grabHorizontal = true;
                    editor.setEditor(progressLabel, item, STATUS_COLUMN);

                    Callable<Boolean> moveCallable = new FileMover(display, episode, newFile, item, progressLabel);
                    futures.add(executor.submit(moveCallable));
                    item.setChecked(false);
                }
            }
        }

        final TaskItem taskItem = getTaskItem();
        // There is no task bar on linux
        if (taskItem != null) {
            taskItem.setProgressState(SWT.NORMAL);
            taskItem.setOverlayImage(FileMoveIcon.RENAMING.icon);

            Thread progressThread = new Thread(new ProgressBarUpdater(new ProgressProxy() {
                @Override
                public void setProgress(final float progress) {
                    if (display.isDisposed()) {
                        return;
                    }

                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (totalProgressBar.isDisposed()) {
                                return;
                            }
                            totalProgressBar.setSelection(Math.round(progress * totalProgressBar.getMaximum()));
                            if (taskItem.isDisposed()) {
                                return;
                            }
                            taskItem.setProgress(Math.round(progress * 100));
                        }
                    });
                }
            }, count, futures, new UpdateCompleteHandler() {
                @Override
                public void onUpdateComplete() {
                    display.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            taskItem.setOverlayImage(null);
                            taskItem.setProgressState(SWT.DEFAULT);
                            refreshTable();
                        }
                    });
                }
            }));
            progressThread.setName("ProgressBarThread");
            progressThread.setDaemon(true);
            progressThread.start();
        }
    }

    public static void setTableItemStatus(Display display, final TableItem item, final FileMoveIcon fmi) {
        if (display.isDisposed()) {
            return;
        }
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (item.isDisposed()) {
                    return;
                }
                item.setImage(STATUS_COLUMN, fmi.icon);
            }
        });
    }

    private static TableItem createTableItem(Table tblResults, String fileName, FileEpisode episode) {
        TableItem item = new TableItem(tblResults, SWT.NONE);
        String newFilename = fileName;
        try {
            // Set if the item is checked or not according
            // to a list of banned keywords
            item.setChecked(!isNameIgnored(newFilename));

            newFilename = episode.getNewFilename();
        } catch (NotFoundException e) {
            newFilename = e.getMessage();
            item.setChecked(false);
            item.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
        }
        item.setText(CURRENT_FILE_COLUMN, fileName);
        item.setText(NEW_FILENAME_COLUMN, newFilename);
        item.setImage(STATUS_COLUMN, FileMoveIcon.DOWNLOADING.icon);
        return item;
    }

    private static boolean isNameIgnored(String fileName) {
        for (int i = 0; i < ignoreKeywords.size(); i++) {
            if (fileName.toLowerCase().contains(ignoreKeywords.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void deleteSelectedTableItems() {
        int index = ITEM_NOT_IN_TABLE;
        for (final TableItem item : resultsTable.getSelection()) {
            index = getTableItemIndex(item);
            if (ITEM_NOT_IN_TABLE == index) {
                logger.info("error: somehow selected item not found in table");
                continue;
            }

            String filename = item.getText(CURRENT_FILE_COLUMN);
            files.remove(filename);

            resultsTable.remove(index);
            item.dispose();
        }
        resultsTable.deselectAll();
    }

    private void setSortedItem(int i, int j) {
        TableItem oldItem = resultsTable.getItem(i);
        boolean wasChecked = oldItem.getChecked();
        int oldStyle = oldItem.getStyle();

        TableItem item = new TableItem(resultsTable, oldStyle, j);
        item.setChecked(wasChecked);
        item.setText(CURRENT_FILE_COLUMN, oldItem.getText(CURRENT_FILE_COLUMN));
        item.setText(NEW_FILENAME_COLUMN, oldItem.getText(NEW_FILENAME_COLUMN));
        item.setImage(STATUS_COLUMN, oldItem.getImage(STATUS_COLUMN));

        oldItem.dispose();
    }

    private void sortTable(TableColumn col, int position) {
        // Get the items
        TableItem[] items = resultsTable.getItems();
        Collator collator = Collator.getInstance(Locale.getDefault());

        // Go through the item list and
        for (int i = 1; i < items.length; i++) {
            String value1 = items[i].getText(position);
            for (int j = 0; j < i; j++) {
                String value2 = items[j].getText(position);
                // Compare the two values and order accordingly
                if (resultsTable.getSortDirection() == SWT.DOWN) {
                    if (collator.compare(value1, value2) < 0) {
                        setSortedItem(i, j);
                        // the snippet replaces the items with the new items, we
                        // do the same
                        items = resultsTable.getItems();
                        break;
                    }
                } else {
                    if (collator.compare(value1, value2) > 0) {
                        setSortedItem(i, j);
                        // the snippet replaces the items with the new items, we
                        // do the same
                        items = resultsTable.getItems();
                        break;
                    }
                }
            }
        }
    }

    private void refreshTable() {
        logger.info("Refreshing table");
        for (TableItem item : resultsTable.getItems()) {
            String fileName = item.getText(CURRENT_FILE_COLUMN);
            FileEpisode episode = files.remove(fileName);
            String newFileName = episode.getFile().getAbsolutePath();
            files.put(newFileName, episode);
            item.setText(CURRENT_FILE_COLUMN, newFileName);
            item.setText(NEW_FILENAME_COLUMN, episode.getNewFilePath());
        }
    }

    public static void setRenameButtonText() {
        if (prefs.isMovedEnabled()) {
            renameSelectedButton.setText("Rename && Move Selected");
            shell.changed(new Control[] {renameSelectedButton});
            shell.layout(false, true);
        } else {
            renameSelectedButton.setText("Rename Selected");
            shell.changed(new Control[] {renameSelectedButton});
            shell.layout(false, true);
        }
    }

    public static void setColumnDestText() {
        if (prefs.isMovedEnabled()) {
            destinationColumn.setText("Proposed File Path");
        } else {
            destinationColumn.setText("Proposed File Name");
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
