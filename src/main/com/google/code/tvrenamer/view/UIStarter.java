package com.google.code.tvrenamer.view;

import static com.google.code.tvrenamer.view.UIUtils.isMac;
import static com.google.code.tvrenamer.view.UIUtils.showMessageBox;

import java.io.File;
import java.io.InputStream;
import java.text.Collator;
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
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
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

import com.google.code.tvrenamer.controller.FileMover;
import com.google.code.tvrenamer.controller.ShowInformationListener;
import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.EpisodeStatus;
import com.google.code.tvrenamer.model.FileEpisode;
import com.google.code.tvrenamer.model.FileMoveIcon;
import com.google.code.tvrenamer.model.NotFoundException;
import com.google.code.tvrenamer.model.SWTMessageBoxType;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.ShowStore;
import com.google.code.tvrenamer.model.UserPreferences;
import com.google.code.tvrenamer.model.util.Constants;

public class UIStarter {
	private static final int CURRENT_FILE_COLUMN = 0;
	private static final int NEW_FILENAME_COLUMN = 1;
	private static final int FILE_STATUS_COLUMN = 2;

	private static Logger logger = Logger.getLogger(UIStarter.class.getName());

	private static Shell shell;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private static UserPreferences prefs = new UserPreferences();
	private Display display;

	private Button btnBrowse;
	private Button btnRefresh;
	private static Button btnRenameSelected;

	private Table tblResults;

	private ProgressBar progressBarTotal;

	private Map<String, FileEpisode> files = new HashMap<String, FileEpisode>();

	public static void main(String[] args) {
		UIStarter ui = new UIStarter();
		ui.init();
		ui.launch();
	}

	private void init() {
		// load preferences
		prefs = prefs.load();

		// Setup display and shell
		GridLayout gridLayout = new GridLayout(3, false);
		Display.setAppName(Constants.APPLICATION_NAME);
		display = new Display();

		shell = new Shell(display);

		shell.setText(Constants.APPLICATION_NAME);
		shell.setLayout(gridLayout);

		// Setup the util class
		new UIUtils(shell);

		// Add controls to main shell
		setupMainWindow();
		setupBrowseDialog();
		setupMenuBar();

		setupIcons();

		shell.pack(true);
	}

	private void setupMainWindow() {
		btnBrowse = new Button(shell, SWT.PUSH);
		btnBrowse.setText("Add files");

		btnRefresh = new Button(shell, SWT.PUSH);
		btnRefresh.setText("Refresh");
		GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gridData.horizontalSpan = 2;
		btnRefresh.setLayoutData(gridData);
		btnRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshTable();
			}
		});

		setupResultsTable();
		setupTableDragDrop();
		
		Composite bottomButtonsComposite = new Composite(shell, SWT.FILL);
		bottomButtonsComposite.setLayout(new GridLayout(3, false));
		GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1);
		// DH: Hack for GTK to stop the bottom of the window being cut off
		bottomButtonsCompositeGridData.minimumHeight = 65;
		bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);

		final Button btnQuit = new Button(bottomButtonsComposite, SWT.PUSH);
		GridData btnQuitGridData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		btnQuitGridData.minimumWidth = 70;
		btnQuitGridData.widthHint = 70;
		btnQuit.setLayoutData(btnQuitGridData);
		btnQuit.setText("Quit");

		progressBarTotal = new ProgressBar(bottomButtonsComposite, SWT.SMOOTH);
		GridData progressBarTotalGridData = new GridData(GridData.FILL, GridData.CENTER, true, true);
		progressBarTotal.setLayoutData(progressBarTotalGridData);

		btnRenameSelected = new Button(bottomButtonsComposite, SWT.PUSH);
		GridData btnRenameSelectedGridData = new GridData(GridData.END, GridData.CENTER, false, false);
		btnRenameSelectedGridData.minimumWidth = 160;
		btnRenameSelectedGridData.widthHint = 160;
		btnRenameSelected.setLayoutData(btnRenameSelectedGridData);
		
		if (prefs != null && prefs.isMovedEnabled()) {
			setupMoveButtonText();
		} else {
			setupRenameButtonText();
		}

		btnRenameSelected.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				renameFiles();
			}
		});

		btnQuit.addSelectionListener(new SelectionAdapter() {
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
		getRenameButtonText();
		btnRenameSelected
			.setToolTipText("Clicking this button will rename and move the selected files to the directory set in preferences (currently "
				+ prefs.getDestinationDirectory().getAbsolutePath() + ").");
	}

	private void setupRenameButtonText() {
		getRenameButtonText();
		btnRenameSelected.setToolTipText("Clicking this button will rename the selected files but leave them where they are.");
	}

	private void setupMenuBar() {
		Menu menuBar = new Menu(shell, SWT.BAR);
		Menu helpMenu;

		Listener preferencesListener = new Listener() {
			public void handleEvent(Event e) {
				showPreferencesPane();
			}
		};

		Listener aboutListener = new Listener() {
			public void handleEvent(Event e) {
				showAboutPane();
			}
		};

		Listener quitListener = new Listener() {
			public void handleEvent(Event e) {
				doCleanup();
			}
		};

		if (isMac()) {
			// Add the special Mac OSX Preferences, About and Quit menus.
			CocoaUIEnhancer enhancer = new CocoaUIEnhancer(Constants.APPLICATION_NAME);
			enhancer.hookApplicationMenu(shell.getDisplay(), quitListener, aboutListener, preferencesListener);

			setupHelpMenuBar(menuBar);
		} else {
			// Add the normal Preferences, About and Quit menus.
			MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
			fileMenuHeader.setText("File");

			Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
			fileMenuHeader.setMenu(fileMenu);

			MenuItem filePreferencesItem = new MenuItem(fileMenu, SWT.PUSH);
			filePreferencesItem.setText("Preferences");
			filePreferencesItem.addListener(SWT.Selection, preferencesListener);

			MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
			fileExitItem.setText("Exit");
			fileExitItem.addListener(SWT.Selection, quitListener);

			helpMenu = setupHelpMenuBar(menuBar);

			// The About item is added to the OSX bar, so we need to add it manually here
			MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
			helpAboutItem.setText("About");
			helpAboutItem.addListener(SWT.Selection, aboutListener);
		}

		shell.setMenuBar(menuBar);
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

	private void setupBrowseDialog() {
		final FileDialog fd = new FileDialog(shell, SWT.MULTI);
		btnBrowse.addSelectionListener(new SelectionAdapter() {

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
	}

	private void setupResultsTable() {
		tblResults = new Table(shell, SWT.CHECK);
		tblResults.setHeaderVisible(true);
		tblResults.setLinesVisible(true);
		GridData gridData = new GridData(GridData.FILL_BOTH);
		// gridData.widthHint = 780;
		gridData.heightHint = 350;
		gridData.horizontalSpan = 3;
		tblResults.setLayoutData(gridData);

		final TableColumn colSrc = new TableColumn(tblResults, SWT.LEFT);
		colSrc.setText("Current File");
		colSrc.setWidth(550);

		final TableColumn colDest = new TableColumn(tblResults, SWT.LEFT);
		colDest.setText("Proposed Filename");
		colDest.setWidth(375);

		final TableColumn colStatus = new TableColumn(tblResults, SWT.LEFT);
		colStatus.setText("Status");
		colStatus.setWidth(75);

		// editable table
		final TableEditor editor = new TableEditor(tblResults);
		editor.horizontalAlignment = SWT.CENTER;
		editor.grabHorizontal = true;

		colSrc.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tblResults.setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
				sortTable(colSrc, 2);
				tblResults.setSortColumn(colSrc);
			}
		});

		colDest.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				tblResults.setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
				sortTable(colDest, 1);
				tblResults.setSortColumn(colDest);
			}
		});

		Listener tblEditListener = new Listener() {

			public void handleEvent(Event event) {
				Rectangle clientArea = tblResults.getClientArea();
				Point pt = new Point(event.x, event.y);
				int index = tblResults.getTopIndex();
				while (index < tblResults.getItemCount()) {
					boolean visible = false;
					final TableItem item = tblResults.getItem(index);
					for (int i = 0; i < tblResults.getColumnCount(); i++) {
						Rectangle rect = item.getBounds(i);
						if (rect.contains(pt)) {
							final int column = i;
							final Text text = new Text(tblResults, SWT.NONE);
							Listener textListener = new Listener() {

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
		tblResults.addListener(SWT.MouseDown, tblEditListener);
	}

	private void setupTableDragDrop() {
		DropTarget dt = new DropTarget(tblResults, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {

			@Override
			public void drop(DropTargetEvent e) {
				String fileList[] = null;
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
		TaskBar bar = display.getSystemTaskBar();
		if (bar != null) {
			taskItem = bar.getItem(shell);
			if (taskItem == null) {
				taskItem = bar.getItem(null);
			}
		}
		return taskItem;
	}

	private void launch() {
		Display display = null;
		try {
			// place the window in the centre of the primary monitor
			Monitor primary = Display.getCurrent().getPrimaryMonitor();
			Rectangle bounds = primary.getBounds();
			Rectangle rect = shell.getBounds();
			int x = bounds.x + (bounds.width - rect.width) / 2;
			int y = bounds.y + (bounds.height - rect.height) / 2;
			shell.setLocation(x, y);

			// Start the shell
			shell.pack();
			shell.open();

			display = shell.getDisplay();
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
			showMessageBox(SWTMessageBoxType.ERROR, "Error", message);
			logger.log(Level.SEVERE, message, exception);
		}
	}

	private void initiateRenamer(final String[] fileNames) {
		final List<String> files = new LinkedList<String>();
		for (final String fileName : fileNames) {
			File f = new File(fileName);
			new FileTraversal() {
				@Override
				public void onFile(File f) {
					files.add(f.getAbsolutePath());
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
		for (final String fileName : fileNames) {
			final FileEpisode episode = TVRenamer.parseFilename(fileName);
			if (episode == null) {
				logger.severe("Couldn't parse file: " + fileName);
			} else {
				String showName = episode.getShowName();

				files.put(fileName, episode);
				final TableItem item = createTableItem(tblResults, fileName, episode, prefs);

				ShowStore.getShow(showName, new ShowInformationListener() {
					public void downloaded(Show show) {
						episode.setStatus(EpisodeStatus.DOWNLOADED);
						display.asyncExec(new Runnable() {
							public void run() {
								item.setText(NEW_FILENAME_COLUMN, episode.getNewFilename(prefs));
								item.setImage(FILE_STATUS_COLUMN, FileMoveIcon.ADDED.icon);
							}
						});
					}
				});
			}
		}
	}

	private void renameFiles() {
		final Queue<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
		int count = 0;

		for (final TableItem item : tblResults.getItems()) {
			if (item.getChecked()) {
				count++;
				String fileName = item.getText(0);
				final File currentFile = new File(fileName);
				final FileEpisode episode = files.get(fileName);
				String currentName = currentFile.getName();
				String newName = item.getText(1);
				String newFilePath = currentFile.getParent() + File.separatorChar + newName;

				if (prefs != null && prefs.isMovedEnabled()) {
					newFilePath = episode.getDestinationDirectory(prefs).getAbsolutePath() + File.separatorChar
						+ newName;
				}

				final File newFile = new File(newFilePath);
				logger.info("Going to move '" + currentFile.getAbsolutePath() + "' to '" + newFile.getAbsolutePath()
					+ "'");

				if (newFile.exists() && !newName.equals(currentName)) {
					String message = "File " + newFile + " already exists.\n" + currentFile + " was not renamed!";
					showMessageBox(SWTMessageBoxType.QUESTION, "Question", message);
				} else {
					// progress label
					TableEditor editor = new TableEditor(tblResults);
					final Label progressLabel = new Label(tblResults, SWT.SHADOW_NONE | SWT.CENTER);
					editor.grabHorizontal = true;
					editor.setEditor(progressLabel, item, FILE_STATUS_COLUMN);

					Callable<Boolean> moveCallable = new FileMover(display, episode, newFile, item, progressLabel);
					futures.add(executor.submit(moveCallable));
				}
			}
		}
		
		final TaskItem taskItem = getTaskItem();
		taskItem.setProgressState(SWT.NORMAL);
		taskItem.setOverlayImage(FileMoveIcon.RENAMING.icon);

		Thread progressThread = new Thread(new ProgressBarUpdater(new ProgressProxy() {
			public void setProgress(final float progress) {
				if (display.isDisposed()) {
					return;
				}

				display.asyncExec(new Runnable() {
					public void run() {
						if (progressBarTotal.isDisposed()) {
							return;
						}
						progressBarTotal.setSelection((int) Math.round(progress * progressBarTotal.getMaximum()));
						if (taskItem.isDisposed()) {
							return;
						}
						taskItem.setProgress((int) Math.round(progress * 100));
					}
				});
			}
		}, count, futures, new UpdateCompleteHandler() {
			public void onUpdateComplete() {
				display.asyncExec(new Runnable() {
					public void run() {
						taskItem.setOverlayImage(null);
						taskItem.setProgressState(SWT.DEFAULT);
						refreshTable();
					}
				});
			}
		}));
		progressThread.setName("ProgressThread");
		progressThread.setDaemon(true);
		progressThread.start();
	}

	public static void setTableItemStatus(Display display, final TableItem item, final FileMoveIcon fmi) {
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(new Runnable() {
			public void run() {
				if (item.isDisposed()) {
					return;
				}
				item.setImage(FILE_STATUS_COLUMN, fmi.icon);
			}
		});
	}

	private static TableItem createTableItem(Table tblResults, String fileName, FileEpisode episode,
		UserPreferences prefs) {
		TableItem item = new TableItem(tblResults, SWT.NONE);
		String newFilename = fileName;
		try {
			newFilename = episode.getNewFilename(prefs);
			item.setChecked(true);
		} catch (NotFoundException e) {
			newFilename = e.getMessage();
			item.setChecked(false);
			item.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
		}
		item.setText(new String[] { fileName, newFilename });
		item.setImage(FILE_STATUS_COLUMN, FileMoveIcon.DOWNLOADING.icon);
		return item;
	}

	private void setSortedItem(int i, int j) {
		TableItem oldItem = tblResults.getItem(i);
		boolean wasChecked = oldItem.getChecked();
		int oldStyle = oldItem.getStyle();
		String[] values = { oldItem.getText(0), oldItem.getText(1) };
		oldItem.dispose();
		TableItem item = new TableItem(tblResults, oldStyle, j);
		item.setText(values);
		item.setChecked(wasChecked);
	}

	private void sortTable(TableColumn col, int position) {
		// Get the items
		TableItem[] items = tblResults.getItems();
		Collator collator = Collator.getInstance(Locale.getDefault());

		// Go through the item list and
		for (int i = 1; i < items.length; i++) {
			String value1 = items[i].getText(position);
			for (int j = 0; j < i; j++) {
				String value2 = items[j].getText(position);
				// Compare the two values and order accordingly
				if (tblResults.getSortDirection() == SWT.DOWN) {
					if (collator.compare(value1, value2) < 0) {
						setSortedItem(i, j);
						// the snippet replaces the items with the new items, we
						// do the same
						items = tblResults.getItems();
						break;
					}
				} else {
					if (collator.compare(value1, value2) > 0) {
						setSortedItem(i, j);
						// the snippet replaces the items with the new items, we
						// do the same
						items = tblResults.getItems();
						break;
					}
				}
			}
		}
	}

	private void refreshTable() {
		logger.info("Refreshing table");
		for (TableItem item : tblResults.getItems()) {
			String fileName = item.getText(CURRENT_FILE_COLUMN);
			FileEpisode episode = files.remove(fileName);
			String newFileName = episode.getFile().getAbsolutePath();
			files.put(newFileName, episode);
			item.setText(CURRENT_FILE_COLUMN, newFileName);
			item.setText(NEW_FILENAME_COLUMN, episode.getNewFilename(prefs));
		}
	}
	
	public static void getRenameButtonText() {
		if(prefs.isMovedEnabled()) {
			btnRenameSelected.setText("Rename && Move Selected");
		} else {
			btnRenameSelected.setText("Rename Selected");
		}
	}

	private void showPreferencesPane() {
		PreferencesDialog preferencesDialog = new PreferencesDialog(shell, prefs);
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
