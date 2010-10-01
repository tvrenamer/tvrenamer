package com.google.code.tvrenamer.view;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
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
	private Combo moveEnabledCombo;
	private Text destDirText;
	private Text seasonPrefixText;
	private Text replacementMaskText;

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

		moveEnabledCombo = new Combo(moveGroup, SWT.READ_ONLY | SWT.BORDER);
		moveEnabledCombo.add("false", 0);
		moveEnabledCombo.add("true", 1);
		moveEnabledCombo.select((prefs.isMovedEnabled()) ? 1 : 0);
		moveEnabledCombo.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));

		Label destDirLabel = new Label(moveGroup, SWT.NONE);
		destDirLabel.setText("Destination directory [?]");
		destDirLabel.setToolTipText("The location of your 'TV' folder");

		destDirText = new Text(moveGroup, SWT.BORDER);
		destDirText.setText(prefs.getDestinationDirectory().toString());
		destDirText.setTextLimit(99);
		destDirText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true));

		final Button destDirButton = new Button(moveGroup, SWT.PUSH);
		destDirButton.setText("Select a directory");
		destDirButton.addListener(SWT.Selection, new Listener() {
			@Override
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

		moveEnabledCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDestDirControl(moveEnabledCombo, destDirText, destDirButton);
			}
		});

		handleDestDirControl(moveEnabledCombo, destDirText, destDirButton);
	}

	private void handleDestDirControl(Combo moveEnabledCombo, Text destDirText, Button destDirButton) {
		boolean enabled = Boolean.parseBoolean(moveEnabledCombo.getText());
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
			.setToolTipText("This is the prefix of the season when renaming and moving the file.  It is usually 'Season ' or 's'");

		seasonPrefixText = new Text(replacementGroup, SWT.BORDER);
		seasonPrefixText.setText("[" + prefs.getSeasonPrefix() + "]");
		seasonPrefixText.setTextLimit(99);
		seasonPrefixText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));

		Label replacementMaskLabel = new Label(replacementGroup, SWT.NONE);
		replacementMaskLabel.setText("Mask Options:");

		Table optionsTable = new Table(replacementGroup, SWT.NONE);
		optionsTable.setLinesVisible(true);
		optionsTable.setHeaderVisible(true);

		optionsTable.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1));

		TableColumn tokenColumn = new TableColumn(optionsTable, SWT.NONE, 0);
		tokenColumn.setText("Token");
		tokenColumn.setWidth(50);

		TableColumn valueColumn = new TableColumn(optionsTable, SWT.NONE, 1);
		valueColumn.setText("Value");
		valueColumn.setWidth(120);

		Map<String, String> optionsMap = new HashMap<String, String>();
		optionsMap.put("%S", "Show Name");
		optionsMap.put("%s", "Season Number");
		optionsMap.put("%e", "Episode Number");
		optionsMap.put("%t", "Episode Title");

		for (String option : optionsMap.keySet()) {
			TableItem item = new TableItem(optionsTable, SWT.NONE);
			item.setText(0, option);
			item.setText(1, optionsMap.get(option));
		}

		Label episodeTitleLabel = new Label(replacementGroup, SWT.NONE);
		episodeTitleLabel.setText("Replacement Mask [?]");
		episodeTitleLabel
			.setToolTipText("The result of the rename, with the tokens being replaced by the meaning above");

		replacementMaskText = new Text(replacementGroup, SWT.BORDER);
		replacementMaskText.setText(prefs.getRenameReplacementMask());
		replacementMaskText.setTextLimit(99);
		replacementMaskText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
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
		prefs.setMovedEnabled(Boolean.parseBoolean(moveEnabledCombo.getText()));
		prefs.setSeasonPrefix(seasonPrefixText.getText());
		prefs.setRenameReplacementMask(replacementMaskText.getText());

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
