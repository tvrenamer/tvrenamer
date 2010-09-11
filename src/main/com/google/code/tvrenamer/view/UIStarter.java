package com.google.code.tvrenamer.view;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.controller.XMLPersistence;
import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.FileEpisode;
import com.google.code.tvrenamer.model.NotFoundException;
import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.ShowStore;
import com.google.code.tvrenamer.model.UserPreferences;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;

public class UIStarter {
	private static Logger     logger     = Logger.getLogger(UIStarter.class.getName());
	private UserPreferences   prefs      = null;
	private ExecutorService   executor   = Executors.newSingleThreadExecutor();
	private ExecutorService   threadPool = Executors.newCachedThreadPool();

	private Display           display;
	private static Shell      shell;

	private Button            btnBrowse;
	private Button            btnRenameSelected;

	private Table             tblResults;

//	private Label             lblStatus;

	private ProgressBar       progressBarIndividual;
	private ProgressBar       progressBarTotal;

	private List<FileEpisode> files;

	public static void main(String[] args) {
		UIStarter ui = new UIStarter();
		ui.init();
		ui.launch();
	}

	private void init() {
		// Set up environment
		loadPreferences();

		GridLayout gridLayout = new GridLayout(4, false);
		Display.setAppName(Constants.APPLICATION_NAME);
		display = new Display();

		shell = new Shell(display);
		shell.setText(Constants.APPLICATION_NAME);
		shell.setLayout(gridLayout);

		setupBrowseDialog();
		setupResultsTable();
		setupTableDragDrop();
		setupMenuBar();
		setupMainWindow();

		setApplicationIcon();
	}

	private void setupMainWindow() {
		final Button btnQuit = new Button(shell, SWT.PUSH);
		btnQuit.setText("Quit");
		btnQuit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));

//		lblStatus = new Label(shell, SWT.NONE);
//		lblStatus.setText("");
//		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		Composite bars = new Composite(shell, SWT.NONE);
		bars.setLayout(fillLayout);
		progressBarIndividual = new ProgressBar(bars, SWT.SMOOTH);
		progressBarTotal = new ProgressBar(bars, SWT.SMOOTH);

		bars.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnRenameSelected = new Button(shell, SWT.PUSH);
		if (prefs != null && prefs.isMovedEnabled()) {
			setupMoveButton(btnRenameSelected);
		} else {
			setupRenameButton(btnRenameSelected);
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
		executor.shutdown();
		threadPool.shutdown();
		shell.dispose();
		display.dispose();
	}

	private void setupMoveButton(Button button) {
		button.setText("Move Selected");
		button
		    .setToolTipText("Clicking this button will rename and move the selected files to the directory set in preferences (currently "
		        + prefs.getDestinationDirectory().getAbsolutePath() + ").");
	}

	private void setupRenameButton(Button button) {
		button.setText("Rename Selected");
		button.setToolTipText("Clicking this button will rename the selected files but leave them where they are.");
	}

	private void setupMenuBar() {
		Menu menuBar = new Menu(shell, SWT.BAR);
		MenuItem fileMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		fileMenuHeader.setText("&File");

		Menu fileMenu = new Menu(shell, SWT.DROP_DOWN);
		fileMenuHeader.setMenu(fileMenu);

		MenuItem filePerferencesItem = new MenuItem(fileMenu, SWT.PUSH);
		filePerferencesItem.setText("&Perferences");

		MenuItem fileExitItem = new MenuItem(fileMenu, SWT.PUSH);
		fileExitItem.setText("E&xit");

		MenuItem helpMenuHeader = new MenuItem(menuBar, SWT.CASCADE);
		helpMenuHeader.setText("&Help");

		Menu helpMenu = new Menu(shell, SWT.DROP_DOWN);
		helpMenuHeader.setMenu(helpMenu);

		MenuItem helpGetHelpItem = new MenuItem(helpMenu, SWT.PUSH);
		helpGetHelpItem.setText("&Get Help");

		MenuItem helpAboutItem = new MenuItem(helpMenu, SWT.PUSH);
		helpAboutItem.setText("&About");

		filePerferencesItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				showPreferencesPane();
			}
		});

		fileExitItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				doCleanup();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				doCleanup();
			}
		});

		helpAboutItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				showAboutPane();
			}
		});

		shell.setMenuBar(menuBar);

		// fileSaveItem.addSelectionListener(new fileSaveItemListener());
		// helpGetHelpItem.addSelectionListener(new helpGetHelpItemListener());
	}

	private void setupBrowseDialog() {
		final FileDialog fd = new FileDialog(shell, SWT.MULTI);
		btnBrowse = new Button(shell, SWT.PUSH);
		btnBrowse.setText("Browse files...");

		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				String pathPrefix = fd.open();
				if (pathPrefix != null) {
					File file = new File(pathPrefix);
					pathPrefix = file.getParent();

					String[] fileNames = fd.getFileNames();
					for (int i = 0; i < fileNames.length; i++) {
						fileNames[i] = pathPrefix + Constants.FILE_SEPARATOR + fileNames[i];
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
		gridData.heightHint = 200;
		gridData.horizontalSpan = 4;
		tblResults.setLayoutData(gridData);

		final TableColumn colIdx = new TableColumn(tblResults, SWT.LEFT);
		//colIdx.setText("Index");
		colIdx.setWidth(0);
		colIdx.setResizable(false);

		final TableColumn colSrc = new TableColumn(tblResults, SWT.LEFT);
		colSrc.setText("Current Name");
		colSrc.setWidth(350);

		final TableColumn colDest = new TableColumn(tblResults, SWT.LEFT);
		colDest.setText("Proposed Name");
		colDest.setWidth(350);

		// editable table
		final TableEditor editor = new TableEditor(tblResults);
		editor.horizontalAlignment = SWT.CENTER;
		editor.grabHorizontal = true;

//		colIdx.addSelectionListener(new SelectionAdapter() {
//			@Override
//			public void widgetSelected(SelectionEvent e) {
//				tblResults.setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP : SWT.DOWN);
//				sortTable(colIdx, 1);
//				tblResults.setSortColumn(colIdx);
//			}
//		});

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

	private void setApplicationIcon() {
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

	private void loadPreferences() {
		File prefsFile = new File(System.getProperty("user.dir") + Constants.FILE_SEPARATOR
		    + Constants.PREFERENCES_FILE);
		try {
			prefs = XMLPersistence.retrieve(prefsFile);
		} catch (IOException e) {
			// failed to load, revert to in-memory
			try {
				prefs = new UserPreferences();
				XMLPersistence.persist(prefs, prefsFile);
			} catch (IOException e1) {
				// either failed to create (no moving),
				// or failed to save (in-memory prefs only)
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		if (prefs != null) {
			logger.info("Initialised: " + prefs.toString());
		}
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
			// showMessageBox(Constants.ERROR, message);
			// display.dispose();
			System.out.println(argumentException.getMessage() + " exception: " + message);
			argumentException.printStackTrace();
			JOptionPane.showMessageDialog(null, message);
			// launch();
			System.exit(1);
		} catch (Exception exception) {
			String message = "An error occoured, please check your internet connection, java version or run from the command line to show errors";
			showMessageBox(SWTMessageBoxType.ERROR, message);
			exception.printStackTrace();
		}
	}

	private void initiateRenamer(final String[] fileNames) {
		final Queue<Future<Boolean>> fetchers = new LinkedList<Future<Boolean>>();
		files = new ArrayList<FileEpisode>();

		final Set<String> showNames = new HashSet<String>();
		for (String fileName : fileNames) {
			final FileEpisode episode = TVRenamer.parseFilename(fileName);
			if (episode == null) {
				System.err.println("Couldn't parse file: " + fileName);
			} else {
				final String showName = episode.getShowName();
				if (!showNames.contains(showName)) {
					showNames.add(showName);
					
					Callable<Boolean> showFetcher = new Callable<Boolean>() {
						public Boolean call() throws Exception {
							ShowStore.addShow(showName);
							return true;
						}
					};
					fetchers.add(threadPool.submit(showFetcher));
				}
				files.add(episode);
			}
		}

//		lblStatus.setText("Please wait ...");

		Thread thread = new Thread() {
			@Override
			public void run() {
				final int totalNumShows = showNames.size();
				while (true) {
					if (display.isDisposed()) {
						return;
					}

					final int size = fetchers.size();
					display.asyncExec(new Runnable() {
						public void run() {
							if (progressBarTotal.isDisposed()) {
								return;
							}
							progressBarTotal.setSelection((int) Math
							    .round(((((double) (totalNumShows - size)) / totalNumShows) * progressBarTotal
							        .getMaximum())));
						}
					});

					if (size == 0) {
						break;
					}

					try {
						Future<Boolean> fetcher = fetchers.remove();
						fetcher.get(); // will block until callable has completed
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}

				// all threads should have finished by now
				display.asyncExec(new Runnable() {
					public void run() {
						populateTable();
//						lblStatus.setText("");
					}
				});
			}
		};
		thread.start();
	}

	private void renameFiles() {
		final Queue<Future<Boolean>> futures = new LinkedList<Future<Boolean>>();
		int count = 0;

		for (final TableItem item : tblResults.getItems()) {
			if (item.getChecked()) {
				count++;
				int index = Integer.parseInt(item.getText(0)) - 1;
				final FileEpisode episode = files.get(index);
				final File currentFile = episode.getFile();
				String currentName = currentFile.getName();
				String newName = item.getText(2);
				String newFilePath = currentFile.getParent() + Constants.FILE_SEPARATOR + newName;

				if (prefs != null) {
					newFilePath = episode.getDestinationDirectory(prefs) + Constants.FILE_SEPARATOR + newName;
				}

				final File newFile = new File(newFilePath);
				logger.info("Going to move '" + currentFile.getAbsolutePath() + "' to '" + newFile.getAbsolutePath()
				    + "'");

				if (newFile.exists() && !newName.equals(currentName)) {
					String message = "File " + newFile + " already exists.\n" + currentFile + " was not renamed!";
					showMessageBox(SWTMessageBoxType.QUESTION, message);
				} else {
//					final long length = currentFile.length();
//					
//					long newLength = newFile.length();
//					System.out.println("(" + newLength + " / " + length + ") = " + ((double) newLength) / length);

					Callable<Boolean> moveCallable = new Callable<Boolean>() {
						public Boolean call() {
							try {
								if (newFile.getParentFile().exists() || newFile.getParentFile().mkdirs()) {
									// FileUtils.copyFile(currentFile, newFile);
									FileUtils.moveFile(currentFile, newFile);
									logger.info("Moved " + currentFile.getAbsolutePath() + " to "
									    + newFile.getAbsolutePath());

									episode.setFile(newFile);
									return true;
								}
							} catch (IOException e) {
								e.printStackTrace();
							}
							return false;
						}
					};

					futures.add(executor.submit(moveCallable));
				}
			}
		}

		final int totalNumFiles = count;

		Thread progressThread = new Thread(new Runnable() {
			public void run() {
				while (true) {
					if (display.isDisposed()) {
						return;
					}

					final int size = futures.size();
					display.asyncExec(new Runnable() {
						public void run() {
							if (progressBarTotal.isDisposed()) {
								return;
							}
							progressBarTotal.setSelection((int) Math
							    .round(((((double) (totalNumFiles - size)) / totalNumFiles) * progressBarTotal.getMaximum())));
						}
					});

					if (size == 0) {
						return;
					}

					try {
						Future<Boolean> future = futures.remove();
						logger.info("future returned: " + future.get());
					} catch (InterruptedException e) {
						e.printStackTrace();
					} catch (ExecutionException e) {
						e.printStackTrace();
					}
				}
			}
		});
		progressThread.start();

//		if (renamedFiles > 0) {
//			if (renamedFiles == 1) {
//				lblStatus.setText(renamedFiles + " file successfully renamed.");
//			} else {
//				lblStatus.setText(renamedFiles + " files successfully renamed.");
//			}
//
//			lblStatus.pack(true);
			populateTable();
//		}

	}

	private void populateTable() {
		if (files == null) {
			return;
		}
		// Clear the table for new use
		tblResults.removeAll();
		for (int i = 0; i < files.size(); i++) {
			FileEpisode episode = files.get(i);
			TableItem item = new TableItem(tblResults, SWT.NONE);
			String newFilename = episode.getFile().getName();
			try {
				newFilename = getNewFilename(episode);
				item.setChecked(true);
			} catch (NotFoundException e) {
				newFilename = e.getMessage();
				item.setChecked(false);
				item.setForeground(display.getSystemColor(SWT.COLOR_RED));
			}
			item.setText(new String[] { String.valueOf(i + 1), episode.getFile().getName(), newFilename });
		}
	}

	private String getNewFilename(FileEpisode episode) {
		String showName = "Show not found";
		String seasonNum = "Season not found";
		String titleString = "Episode not found";

		Show show = ShowStore.getShow(episode.getShowName().toLowerCase());
		showName = show.getName();

		Season season = show.getSeason(episode.getSeasonNumber());
		seasonNum = String.valueOf(season.getNumber());

		String title = season.getTitle(episode.getEpisodeNumber());
		titleString = StringUtils.sanitiseTitle(title);

		String newFilename = Constants.DEFAULT_FORMAT_STRING;
		newFilename = newFilename.replaceAll("%S", showName);
		newFilename = newFilename.replaceAll("%s", seasonNum);
		newFilename = newFilename.replaceAll("%e", new DecimalFormat("00").format(episode.getEpisodeNumber()));
		newFilename = newFilename.replaceAll("%t", titleString);

		return newFilename + "." + StringUtils.getExtension(episode.getFile().getName());
	}

	private void setSortedItem(int i, int j) {
		TableItem oldItem = tblResults.getItem(i);
		boolean wasChecked = oldItem.getChecked();
		int oldStyle = oldItem.getStyle();
		String[] values = { oldItem.getText(0), oldItem.getText(1), oldItem.getText(2) };
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

	private void showPreferencesPane() {
		MessageBox msgSuccess = new MessageBox(shell, SWT.OK);
		msgSuccess.setMessage("Preferences Pane placeholder");
		msgSuccess.open();
	}

	private void showAboutPane() {
		MessageBox msgSuccess = new MessageBox(shell, SWT.OK);
		msgSuccess.setMessage("TVRenamer is a Java GUI utility to rename tv episodes from tv listings");
		msgSuccess.open();
	}

	public static void showMessageBox(SWTMessageBoxType type, String message) {
		int swtIconValue = -1;

		switch (type) {
			case QUESTION:
				swtIconValue = SWT.ICON_QUESTION;
				break;
			case MESSAGE:
				swtIconValue = SWT.ICON_INFORMATION;
				break;
			case WARNING:
				swtIconValue = SWT.ICON_WARNING;
				break;
			case ERROR:
				swtIconValue = SWT.ICON_ERROR;
				break;
			case OK:
				// Intentional missing break
			default:
				swtIconValue = SWT.OK;
		}

		logger.info("swtIconValue: " + swtIconValue);
		MessageBox msgSuccess = new MessageBox(shell, swtIconValue);
		msgSuccess.setMessage(message);
		msgSuccess.open();
	}
}
