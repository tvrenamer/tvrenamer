package com.google.code.tvrenamer.view;

import java.util.HashMap;
import java.util.Map;
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

import com.google.code.tvrenamer.model.UserPreferences;

/**
 * The Preferences Dialog box.
 */
public class PreferencesDialog extends Dialog {

	private static Logger logger = Logger.getLogger(PreferencesDialog.class.getName());

	private static Shell preferencesShell;

	private final UserPreferences prefs = UserPreferences.getInstance();

	/**
	 * PreferencesDialog constructor
	 * 
	 * @param parent
	 *            the parent {@link Shell}
	 */
	public PreferencesDialog(Shell parent) {
		super(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
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

		createMoveGroup();

		createSeasonPrefix();

		createReplacementMask();

		Button saveButton = new Button(preferencesShell, SWT.PUSH);
		saveButton.setText("Save");

		GridData saveGridData = new GridData();
		saveGridData.horizontalSpan = shellGridLayout.numColumns;
		saveGridData.widthHint = 150;
		saveGridData.horizontalAlignment = GridData.END;
		saveButton.setLayoutData(saveGridData);
		saveButton.setFocus();

		saveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				preferencesShell.close();
			}
		});

		// Set the OK button as the default, so
		// user can press Enter to dismiss
		preferencesShell.setDefaultButton(saveButton);
	}

	private void createMoveGroup() {
		Group moveGroup = new Group(preferencesShell, SWT.NONE);
		moveGroup.setText("Move To Destination");

		moveGroup.setLayout(new GridLayout(3, false));

		moveGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, true, 3, 1));

		Label moveEnabledLabel = new Label(moveGroup, SWT.NONE);
		moveEnabledLabel.setText("Move Enabled");
		final Combo moveEnabledCombo = new Combo(moveGroup, SWT.READ_ONLY | SWT.BORDER);
		moveEnabledCombo.add("false", 0);
		moveEnabledCombo.add("true", 1);
		moveEnabledCombo.select((prefs.isMovedEnabled()) ? 1 : 0);
		GridData moveEnabledTextGridData = new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1);
		moveEnabledCombo.setLayoutData(moveEnabledTextGridData);

		Label destDirLabel = new Label(moveGroup, SWT.NONE);
		destDirLabel.setText("Destination directory");

		final Text destDirText = new Text(moveGroup, SWT.BORDER);
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
				disableDestDirControls(moveEnabledCombo, destDirText, destDirButton);
			}
		});

		disableDestDirControls(moveEnabledCombo, destDirText, destDirButton);
	}

	private void disableDestDirControls(Combo moveEnabledCombo, Text destDirText, Button destDirButton) {
		boolean enabled = Boolean.parseBoolean(moveEnabledCombo.getText());
		logger.finer("Updating destDir controls to : " + enabled);
		destDirText.setEnabled(enabled);
		destDirButton.setEnabled(enabled);

		preferencesShell.redraw();
	}

	private void createSeasonPrefix() {
		Label seasonPrefixLabel = new Label(preferencesShell, SWT.NONE);
		seasonPrefixLabel.setText("Season Prefix");
		Text seasonPrefixText = new Text(preferencesShell, SWT.BORDER);
		seasonPrefixText.setText(prefs.getSeasonPrefix());
		seasonPrefixText.setTextLimit(99);
		GridData seasonPrefixGridData = new GridData(GridData.BEGINNING, GridData.CENTER, true, true, 2, 1);
		seasonPrefixText.setLayoutData(seasonPrefixGridData);
	}

	private void createReplacementMask() {
		Group replacementGroup = new Group(preferencesShell, SWT.NONE);
		replacementGroup.setText("Replacement Mask");
		replacementGroup.setLayout(new GridLayout(3, false));
		replacementGroup.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, true, 3, 1));

		Label replacementMaskLabel = new Label(replacementGroup, SWT.NONE);
		replacementMaskLabel.setText("Options:");

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
		episodeTitleLabel.setText("Replacement Mask");
		Text replacementMaskText = new Text(replacementGroup, SWT.BORDER);
		replacementMaskText.setText(prefs.getRenameReplacementMask());
		replacementMaskText.setTextLimit(99);
		replacementMaskText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, true, 2, 1));
	}
}
