package com.google.code.tvrenamer.view;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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

import com.google.code.tvrenamer.model.TVRenamerIOException;
import com.google.code.tvrenamer.model.UserPreferences;
import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;

/**
 * The Preferences Dialog box.
 */
public class PreferencesDialog extends Dialog {

	private static Logger logger = Logger.getLogger(PreferencesDialog.class.getName());

	private static Shell preferencesShell;
	private final UserPreferences prefs;

	// The controls to save
	private Button moveEnabledCheckbox;
	private Text destDirText;
	private Text seasonPrefixText;
	private Text replacementStringText;

	/**
	 * PreferencesDialog constructor
	 * 
	 * @param parent
	 *            the parent {@link Shell}
	 */
	public PreferencesDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

		prefs = UserPreferences.load();
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

		createButtonGroup();
	}

	private void createMoveGroup() {
		Group moveGroup = new Group(preferencesShell, SWT.NONE);
		moveGroup.setText("Move To Destination [?]");
		moveGroup.setLayout(new GridLayout(3, false));
		moveGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, true, 3, 1));
		moveGroup
			.setToolTipText("TVRenamer will automatically move the files to your 'TV' folder if you want it to.  \n"
				+ "It will move the file to <tv directory>/Show Name/<season prefix> #/ \n"
				+ "Once enabled, set the location of the folder below.");

		Label moveEnabledLabel = new Label(moveGroup, SWT.NONE);
		moveEnabledLabel.setText("Move Enabled [?]");
		moveEnabledLabel.setToolTipText("Whether the 'move to TV location' functionality is enabled");

		moveEnabledCheckbox = new Button(moveGroup, SWT.CHECK);
		moveEnabledCheckbox.setText("Move Enabled");
		moveEnabledCheckbox.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 3, 1));

		Label destDirLabel = new Label(moveGroup, SWT.NONE);
		destDirLabel.setText("Destination directory [?]");
		destDirLabel.setToolTipText("The location of your 'TV' folder");

		destDirLabel.addMouseListener(new MouseListener() {

			public void mouseUp(MouseEvent e) {
				// no-op
			}

			public void mouseDown(MouseEvent e) {
				System.out.println("mouseDown");
			}

			public void mouseDoubleClick(MouseEvent e) {
				// no-op
			}
		});

		destDirText = new Text(moveGroup, SWT.BORDER);
		destDirText.setText(prefs.getDestinationDirectory().toString());
		destDirText.setTextLimit(99);
		destDirText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true));

		final Button destDirButton = new Button(moveGroup, SWT.PUSH);
		destDirButton.setText("Select a directory");
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

		handleDestDirControl(moveEnabledCheckbox, destDirText, destDirButton);

		moveEnabledCheckbox.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDestDirControl(moveEnabledCheckbox, destDirText, destDirButton);
			}
		});

	}

	private void handleDestDirControl(Button moveEnabledCheckbox, Text destDirText, Button destDirButton) {
		boolean enabled = moveEnabledCheckbox.getSelection();
		logger.finer("Updating destDir controls to : " + enabled);
		destDirText.setEnabled(enabled);
		destDirButton.setEnabled(enabled);

		preferencesShell.redraw();
	}

	private void createRenameGroup() {
		Group replacementGroup = new Group(preferencesShell, SWT.NONE);
		replacementGroup.setText("Rename Options");
		replacementGroup.setLayout(new GridLayout(3, false));
		replacementGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, true, 3, 1));

		Label seasonPrefixLabel = new Label(replacementGroup, SWT.NONE);
		seasonPrefixLabel.setText("Season Prefix [?]");
		seasonPrefixLabel
			.setToolTipText("This is the prefix of the season when renaming and moving the file.  It is usually \"Season \" or \"s'\".  The \" will not be included, just displayed here to show whitespace");

		seasonPrefixText = new Text(replacementGroup, SWT.BORDER);
		seasonPrefixText.setText(prefs.getSeasonPrefixForDisplay());
		seasonPrefixText.setTextLimit(99);
		seasonPrefixText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

		Label replacementOptionsLabel = new Label(replacementGroup, SWT.NONE);
		replacementOptionsLabel.setText("Rename Options:");

//		Table optionsTable = new Table(replacementGroup, SWT.NONE);
//		optionsTable.setLinesVisible(true);
//		optionsTable.setHeaderVisible(true);
//
//		optionsTable.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));
//
//		TableColumn tokenColumn = new TableColumn(optionsTable, SWT.NONE, 0);
//		tokenColumn.setText("Token");
//		tokenColumn.setWidth(50);
//
//		TableColumn valueColumn = new TableColumn(optionsTable, SWT.NONE, 1);
//		valueColumn.setText("Value");
//		valueColumn.setWidth(120);
//
//		Map<String, String> tokenMap = new LinkedHashMap<String, String>();
//		tokenMap.put("%S", "Show Name");
//		tokenMap.put("%s", "Season Number");
//		tokenMap.put("%e", "Episode Number");
//		tokenMap.put("%t", "Episode Title");
//		tokenMap.put("%T", "Episode Title (with spaces removed)");
//
//		for (String option : tokenMap.keySet()) {
//			TableItem item = new TableItem(optionsTable, SWT.NONE);
//			item.setText(0, option);
//			item.setText(1, tokenMap.get(option));
//
//		}

		List optionsList = new List(replacementGroup, SWT.SINGLE);
		optionsList.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));
		optionsList.add("%S : Show Name");
		optionsList.add("%s : Season Number");
		optionsList.add("%e : Episode Number");
		optionsList.add("%t : Episode Title");
		optionsList.add("%T : Episode Title (with spaces removed)");

		Label episodeTitleLabel = new Label(replacementGroup, SWT.NONE);
		episodeTitleLabel.setText("Replacement Tokens [?]");
		episodeTitleLabel
			.setToolTipText("The result of the rename, with the tokens being replaced by the meaning above");

		replacementStringText = new Text(replacementGroup, SWT.BORDER);
		replacementStringText.setText(prefs.getRenameReplacementString());
		replacementStringText.setTextLimit(99);
		replacementStringText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
	}

	private void createButtonGroup() {
		Button cancelButton = new Button(preferencesShell, SWT.PUSH);
		cancelButton.setText("Cancel");

		Button saveButton = new Button(preferencesShell, SWT.PUSH);
		saveButton.setText("Save");

		GridData saveGridData = new GridData();
		saveGridData.widthHint = 150;
		saveGridData.horizontalAlignment = GridData.END;
		saveButton.setLayoutData(saveGridData);
		saveButton.setFocus();

		cancelButton.setLayoutData(saveGridData);

		saveButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				savePreferences();
				preferencesShell.close();
			}
		});

		cancelButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
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

		try {
			prefs.setDestinationDirectory(destDirText.getText());
		} catch (TVRenamerIOException e) {
			UIUtils.showMessageBox(SWTMessageBoxType.ERROR, "Error", "Unable to create the destination directory: "
				+ destDirText.getText());
			logger.log(Level.WARNING, "Unable to create the destination directory", e);
		}
		prefs.store();
	}
}
