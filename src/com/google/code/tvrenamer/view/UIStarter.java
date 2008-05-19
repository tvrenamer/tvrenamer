package com.google.code.tvrenamer.view;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.Show;

public class UIStarter {
  private static final String pathSeparator = System
      .getProperty("file.separator");
  private static Logger logger = Logger.getLogger(UIStarter.class);

  public static void main(String[] args) {
    UIStarter ui = new UIStarter();
    ui.init();
    ui.launch();
  }

  private Shell shell;
  private Table tblResults;

  private Text textFormat;
  private Text textShowName;

  private ArrayList<Show> showList;
  private ArrayList<String> files;

  private TVRenamer tv;

  private void init() {
    // Set up environment
    GridLayout gridLayout = new GridLayout(3, false);
    final Display display = new Display();
    shell = new Shell(display);
    shell.setText("TVRenamer");
    shell.setLayout(gridLayout);

    // File browsing
    final FileDialog fd = new FileDialog(shell, SWT.MULTI);
    Button btnBrowse = new Button(shell, SWT.PUSH);
    btnBrowse.setText("Browse files...");
    // Drop down to select show
    final Combo showCombo = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY
        | SWT.SIMPLE);
    showCombo.setEnabled(false);

    final Composite formatParent = new Composite(shell, SWT.NONE);
    GridData formatData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    formatData.widthHint = 450;
    formatParent.setLayout(new GridLayout(6, false));
    formatParent.setLayoutData(formatData);

    final Label lblFormat = new Label(formatParent, SWT.NONE);
    lblFormat.setText("Format:");

    textFormat = new Text(formatParent, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    textFormat.setText("%S [%sx%e] %t");
    textFormat.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Label lblShowName = new Label(formatParent, SWT.None);
    lblShowName.setText("Show:");

    textShowName = new Text(formatParent, SWT.LEFT | SWT.SINGLE | SWT.BORDER);
    textShowName.setEnabled(false);
    textShowName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    final Button btnFormat = new Button(formatParent, SWT.PUSH);
    btnFormat.setText("Apply");
    btnFormat.setEnabled(false);

    btnFormat.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        populateTable();
      }
    });

    final Button btnReset = new Button(formatParent, SWT.PUSH);
    btnReset.setText("Reset");
    btnReset.setEnabled(true);

    btnReset.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        textFormat.setText("%S [%sx%e] %t");
        if (tv != null)
          textShowName.setText(tv.getShowName(new File(files.get(0))));
        else
          textShowName.setText("");

        if (tv != null)
          populateTable();
      }
    });

    // Results table
    tblResults = new Table(shell, SWT.CHECK);
    tblResults.setHeaderVisible(true);
    tblResults.setLinesVisible(true);
    GridData gridData = new GridData(GridData.FILL_BOTH);
    // gridData.widthHint = 780;
    gridData.heightHint = 200;
    gridData.horizontalSpan = 3;
    tblResults.setLayoutData(gridData);

    final TableColumn col1 = new TableColumn(tblResults, SWT.LEFT);
    col1.setText("Index");
    col1.setWidth(40);

    final TableColumn col2 = new TableColumn(tblResults, SWT.LEFT);
    col2.setText("Current Name");
    col2.setWidth(380);

    final TableColumn col3 = new TableColumn(tblResults, SWT.LEFT);
    col3.setText("Proposed Name");
    col3.setWidth(380);

    // editable table
    final TableEditor editor = new TableEditor(tblResults);
    editor.horizontalAlignment = SWT.CENTER;
    editor.grabHorizontal = true;

    final Button btnRenameAll = new Button(shell, SWT.PUSH);
    btnRenameAll.setText("Rename All");

    final Button btnRenameSelected = new Button(shell, SWT.PUSH);
    btnRenameSelected.setText("Rename Selected");

    final Button btnQuit = new Button(shell, SWT.PUSH);
    btnQuit.setText("Quit");
    btnQuit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));

    btnBrowse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {

        String pathPrefix = fd.open();
        if (pathPrefix != null) {
          File file = new File(pathPrefix);
          pathPrefix = file.getParent();

          String[] fileNames = fd.getFileNames();
          for (int i = 0; i < fileNames.length; i++) {
            fileNames[i] = pathPrefix + pathSeparator + fileNames[i];
          }

          files = new ArrayList<String>(Arrays.asList(fileNames));

          // Call tvRenamer which actually finds the ep names ...
          tv = new TVRenamer();

          String showName = tv.getShowName(new File(fileNames[0]));

          textShowName.setText(showName);
          textShowName.setEnabled(true);

          showList = tv.downloadOptions(showName);

          if (showList.isEmpty())
            return;

          if (showList.size() > 1) {
            showCombo.setEnabled(true);
          }

          showCombo.removeAll();

          for (Show show : showList) {
            showCombo.add(show.getName());
          }

          showCombo.select(0);
          showCombo.pack(true);

          btnFormat.setEnabled(true);

          tv.setShow(showList.get(showCombo.getSelectionIndex()));

          tv.downloadListing();

          populateTable();

          // Sort the list descending by Episode
          tblResults.setSortColumn(col1);
          tblResults.setSortDirection(SWT.DOWN);
          // sortTable(col1, 1);
        }
      }
    });

    showCombo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        int showNum = ((Combo) e.getSource()).getSelectionIndex();
        tv.setShow(showList.get(showNum));
        tv.downloadListing();
        populateTable();
      }
    });

    btnRenameAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        renameFiles(true);
      }
    });

    btnRenameSelected.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        renameFiles(false);
      }
    });

    col1.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tblResults
            .setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP
                : SWT.DOWN);
        sortTable(col1, 1);
        tblResults.setSortColumn(col1);
      }
    });

    col2.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tblResults
            .setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP
                : SWT.DOWN);
        sortTable(col2, 2);
        tblResults.setSortColumn(col2);
      }
    });

    col3.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        tblResults
            .setSortDirection(tblResults.getSortDirection() == SWT.DOWN ? SWT.UP
                : SWT.DOWN);
        sortTable(col3, 1);
        tblResults.setSortColumn(col3);
      }
    });

    btnQuit.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        display.dispose();
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
          if (!visible)
            return;
          index++;
        }
      }
    };
    tblResults.addListener(SWT.MouseDown, tblEditListener);
  }

  private void launch() {
    // place the window in the center of the primary monitor
    Monitor primary = Display.getCurrent().getPrimaryMonitor();
    Rectangle bounds = primary.getBounds();
    Rectangle rect = shell.getBounds();
    int x = bounds.x + (bounds.width - rect.width) / 2;
    int y = bounds.y + (bounds.height - rect.height) / 2;
    shell.setLocation(x, y);

    // Start the shell
    shell.pack();
    shell.open();

    Display display = shell.getDisplay();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  private void renameFiles(boolean all) {
    int renamedFiles = 0;
    for (TableItem item : tblResults.getItems()) {
      if (all || item.getChecked()) {
        int index = Integer.parseInt(item.getText(0)) - 1;
        String currentName = files.get(index);
        File file = new File(currentName);
        File newFile = new File(file.getParent() + pathSeparator
            + item.getText(2));
        file.renameTo(newFile);
        logger.info("Renamed " + file.getAbsolutePath() + " to "
            + newFile.getAbsolutePath());
        renamedFiles++;
        files.set(index, newFile.getAbsolutePath());
      }
    }

    if (renamedFiles > 0) {
      MessageBox msgSuccess = new MessageBox(shell, SWT.OK
          | SWT.ICON_INFORMATION);
      msgSuccess.setMessage(renamedFiles + " files successfully renamed!");
      msgSuccess.open();
      populateTable();
    }

  }

  private void populateTable() {
    // Clear the table for new use
    tblResults.removeAll();
    for (int i = 0; i < files.size(); i++) {
      String fileName = files.get(i);
      String oldFilename = new File(fileName).getName();
      String newFilename = tv.parseFileName(new File(fileName), textShowName
          .getText(), textFormat.getText());
      TableItem item = new TableItem(tblResults, SWT.NONE);
      item.setText(new String[] { (i + 1) + "", oldFilename, newFilename });
      item.setChecked(true);
    }
  }

  private void setSortedItem(int i, int j) {
    TableItem oldItem = tblResults.getItem(i);
    boolean wasChecked = oldItem.getChecked();
    int oldStyle = oldItem.getStyle();
    String[] values = { oldItem.getText(0), oldItem.getText(1),
        oldItem.getText(2) };
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
}
