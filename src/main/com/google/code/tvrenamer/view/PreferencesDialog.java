package com.google.code.tvrenamer.view;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.code.tvrenamer.model.ProxySettings;
import com.google.code.tvrenamer.model.ReplacementToken;
import com.google.code.tvrenamer.model.SWTMessageBoxType;
import com.google.code.tvrenamer.model.TVRenamerIOException;
import com.google.code.tvrenamer.model.UserPreferences;

/**
 * The Preferences Dialog box.
 */
public class PreferencesDialog extends Dialog {

	private static final String REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX = "(.*) :.*";
	private static Logger logger = Logger.getLogger(PreferencesDialog.class.getName());
	private static Shell preferencesShell;
	private static int DND_OPERATIONS = DND.DROP_MOVE;
	
	private final UserPreferences prefs;

	// The controls to save
	private Button moveEnabledCheckbox;
	private Text destDirText;
	private Text seasonPrefixText;
	private Text replacementStringText;
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
		preferencesShell.setText("Preferences");

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

	/**
	 * Creates the dialog's contents.
	 * 
	 * @param preferencesShell
	 *            the dialog window
	 */
	private void createContents() {
		GridLayout shellGridLayout = new GridLayout(3, false);
		preferencesShell.setLayout(shellGridLayout);

		Label helpLabel = new Label(preferencesShell, SWT.NONE);
		helpLabel.setText("Hover mouse over [?] to get help");
		helpLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, true, shellGridLayout.numColumns, 1));

		createMoveGroup();

		createRenameGroup();
		
		createProxyGroup();
		
		createCheckForUpdatesGroup();
		
		createAddFolderGroup();

		createActionButtonGroup();
	}

	private void createMoveGroup() {
		Group moveGroup = new Group(preferencesShell, SWT.NONE);
		moveGroup.setText("Move To TV Folder [?]");
		moveGroup.setLayout(new GridLayout(3, false));
		moveGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
		moveGroup
			.setToolTipText(" - TVRenamer will automatically move the files to your 'TV' folder if you want it to.  \n"
				+ " - It will move the file to <tv directory>/<show name>/<season prefix> #/ \n"
				+ " - Once enabled, set the location below.");

		moveEnabledCheckbox = new Button(moveGroup, SWT.CHECK);
		moveEnabledCheckbox.setText("Move Enabled [?]");
		moveEnabledCheckbox.setSelection(prefs.isMovedEnabled());
		moveEnabledCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
		moveEnabledCheckbox.setToolTipText("Whether the 'move to TV location' functionality is enabled");

		Label destDirLabel = new Label(moveGroup, SWT.NONE);
		destDirLabel.setText("TV Directory [?]");
		destDirLabel.setToolTipText("The location of your 'TV' folder");

		destDirText = new Text(moveGroup, SWT.BORDER);
		destDirText.setText(prefs.getDestinationDirectory().toString());
		destDirText.setTextLimit(99);
		destDirText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true));

		final Button destDirButton = new Button(moveGroup, SWT.PUSH);
		destDirButton.setText("Select directory");
		destDirButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog directoryDialog = new DirectoryDialog(preferencesShell);

				directoryDialog.setFilterPath(prefs.getDestinationDirectory().toString());
				directoryDialog.setText("Please select a directory and click OK");

				String dir = directoryDialog.open();
				if (dir != null) {
					destDirText.setText(dir);
				}
			}
		});
		
		Label seasonPrefixLabel = new Label(moveGroup, SWT.NONE);
		seasonPrefixLabel.setText("Season Prefix [?]");
		seasonPrefixLabel.setToolTipText(" - The prefix of the season when renaming and moving the file.  It is usually \"Season \" or \"s'\"." +
			"\n - If no value is entered (or \"\"), the season folder will not be created, putting all files in the show name folder" +
			"\n - The \" will not be included, just displayed here to show whitespace");

		seasonPrefixText = new Text(moveGroup, SWT.BORDER);
		seasonPrefixText.setText(prefs.getSeasonPrefixForDisplay());
		seasonPrefixText.setTextLimit(99);
		seasonPrefixText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

		destDirText.addKeyListener(new KeyListener() {

			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F1) {
					System.out.println("F1HelpRequested");
				}
			}

			public void keyReleased(KeyEvent e) {
				// no-op
			}
		});

		destDirText.addHelpListener(new HelpListener() {

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
	}

	private void createRenameGroup() {
		Group replacementGroup = new Group(preferencesShell, SWT.NONE);
		replacementGroup.setText("Rename Options");
		replacementGroup.setLayout(new GridLayout(3, false));
		replacementGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));

		Label renameTokensLabel = new Label(replacementGroup, SWT.NONE);
		renameTokensLabel.setText("Rename Tokens [?]");
		renameTokensLabel.setToolTipText(" - These are the possible tokens to make up the 'Rename Format' below." +
				"\n - You can drag and drop tokens to the 'Rename Format' text box below");

		List renameTokensList = new List(replacementGroup, SWT.SINGLE);
		renameTokensList.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));
		renameTokensList.add(ReplacementToken.SHOW_NAME.toString());
		renameTokensList.add(ReplacementToken.SEASON_NUM.toString());
		renameTokensList.add(ReplacementToken.SEASON_NUM_LEADING_ZERO.toString());
		renameTokensList.add(ReplacementToken.EPISODE_NUM.toString());
		renameTokensList.add(ReplacementToken.EPISODE_NUM_LEADING_ZERO.toString());
		renameTokensList.add(ReplacementToken.EPISODE_TITLE.toString());
		renameTokensList.add(ReplacementToken.EPISODE_TITLE_NO_SPACES.toString());
		renameTokensList.add(ReplacementToken.DATE_DAY_NUM.toString());
		renameTokensList.add(ReplacementToken.DATE_DAY_NUMLZ.toString());
		renameTokensList.add(ReplacementToken.DATE_MONTH_NUM.toString());
		renameTokensList.add(ReplacementToken.DATE_MONTH_NUMLZ.toString());
		renameTokensList.add(ReplacementToken.DATE_YEAR_MIN.toString());
		renameTokensList.add(ReplacementToken.DATE_YEAR_FULL.toString());

		Label episodeTitleLabel = new Label(replacementGroup, SWT.NONE);
		episodeTitleLabel.setText("Rename Format [?]");
		episodeTitleLabel.setToolTipText("The result of the rename, with the tokens being replaced by the meaning above");

		replacementStringText = new Text(replacementGroup, SWT.BORDER);
		replacementStringText.setText(prefs.getRenameReplacementString());
		replacementStringText.setTextLimit(99);
		replacementStringText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
		
		createDragSource(renameTokensList);
		createDropTarget(replacementStringText);
	}
	
	private static void createDragSource(final List sourceList) {
		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		DragSource dragSource = new DragSource(sourceList, DND_OPERATIONS);
		dragSource.setTransfer(types);
		dragSource.addDragListener(new DragSourceListener() {

			public void dragStart(DragSourceEvent event) {
				if (sourceList.getSelectionIndex() != 0) {
					event.doit = true;
				}
			}

			public void dragSetData(DragSourceEvent event) {
				String listEntry = sourceList.getItem(sourceList.getSelectionIndex());
				String token;
				
				Matcher tokenMatcher =  Pattern.compile(REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX).matcher(listEntry);
				if(tokenMatcher.matches()) {
					token = tokenMatcher.group(1);
					event.data = token;
				}
			}

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

			public void dragEnter(DropTargetEvent event) {
				// no-op
			}

			public void dragLeave(DropTargetEvent event) {
				// no-op
			}

			public void dragOperationChanged(DropTargetEvent event) {
				// no-op
			}

			public void dragOver(DropTargetEvent event) {
				// no-op
			}

			public void drop(DropTargetEvent event) {
				String data = (String) event.data;
				//  TODO: This currently adds the dropped text onto the end, not where we dropped it
				targetText.append(data);
			}

			public void dropAccept(DropTargetEvent event) {
				// no-op
			}
		});
	}
	
	private void createProxyGroup() {
		ProxySettings proxy = prefs.getProxy();
		
		Group proxyGroup = new Group(preferencesShell, SWT.NONE);
		proxyGroup.setText("Proxy Settings [?]");
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
		proxyAuthenticationRequiredCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
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
				if(proxyEnabledCheckbox.getSelection()) {
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
	}
	
	private void createCheckForUpdatesGroup() {
		Group checkForUpdateGroup = new Group(preferencesShell, SWT.FILL);
		checkForUpdateGroup.setText("Check for Updates");
		checkForUpdateGroup.setLayout(new GridLayout(3, false));
		checkForUpdateGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
		
		checkForUpdatesCheckbox = new Button(checkForUpdateGroup, SWT.CHECK);
		checkForUpdatesCheckbox.setText("Check for Updates at startup");
		checkForUpdatesCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
		checkForUpdatesCheckbox.setSelection(prefs.checkForUpdates());
	}
	
	private void createAddFolderGroup() {
		Group createAddFolderGroup = new Group( preferencesShell, SWT.FILL);
		createAddFolderGroup.setText("Adding Folders");
		createAddFolderGroup.setLayout(new GridLayout(3, false));
		createAddFolderGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
		
		recurseFoldersCheckbox = new Button(createAddFolderGroup, SWT.CHECK);
		recurseFoldersCheckbox.setText("Recursively add shows in subdirectories");
		recurseFoldersCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));
		recurseFoldersCheckbox.setSelection(prefs.isRecursivelyAddFolders());
		
	}

	private void createActionButtonGroup() {
		Composite bottomButtonsComposite = new Composite(preferencesShell, SWT.FILL);
		bottomButtonsComposite.setLayout(new GridLayout(2, false));
		GridData bottomButtonsCompositeGridData = new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1);
		bottomButtonsComposite.setLayoutData(bottomButtonsCompositeGridData);
		
		Button cancelButton = new Button(bottomButtonsComposite, SWT.PUSH);
		GridData cancelButtonGridData = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
		cancelButtonGridData.minimumWidth = 150;
		cancelButtonGridData.widthHint = 150;
		cancelButton.setLayoutData(cancelButtonGridData);
		cancelButton.setText("Cancel");
		
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
		saveButton.setText("Save");
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
		prefs.setMovedEnabled(moveEnabledCheckbox.getSelection());
		prefs.setSeasonPrefix(seasonPrefixText.getText());
		prefs.setRenameReplacementString(replacementStringText.getText());
		
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
			UIUtils.showMessageBox(SWTMessageBoxType.ERROR, "Error", "Unable to create the destination directory: "
				+ destDirText.getText());
			logger.log(Level.WARNING, "Unable to create the destination directory", e);
		}
		UserPreferences.store(prefs);
	}
	
	/**
	 * Toggle whether the or not the listed {@link Control}s are enabled, based off the of the selection value of the checkbox
	 * @param decidingCheckbox the checkbox the enable flag is taken off
	 * @param controls the list of controls to update
	 */
	private void toggleEnableControls(Button decidingCheckbox, Control... controls) {	
		for(Control control : controls) {
			control.setEnabled(decidingCheckbox.getSelection());
		}
		preferencesShell.redraw();
	}
}
