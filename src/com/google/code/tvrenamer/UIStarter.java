package com.google.code.tvrenamer;

import java.io.File;
import java.text.Collator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class UIStarter {
  private static final int RENAME = 1;
  private static final int LIST = 2;
  private static final String pathSeparator = System
      .getProperty("file.separator");
  private static Logger logger = Logger.getLogger(UIStarter.class);

  private Map<Integer, Episode> episodeListing;
  private Shell shell = null;
  private Table tblResults = null;

  public static void main(String[] args) {
    UIStarter ui = new UIStarter();
    ui.init();
    ui.launch();
  }

  private void init() {
    // Set up environment
    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    final Display display = new Display();
    shell = new Shell(display);
    shell.setText("TVRenamer");
    shell.setLayout(gridLayout);

    // File browsing
    final FileDialog fd = new FileDialog(shell, SWT.MULTI);
    Button btnBrowse = new Button(shell, SWT.PUSH);
    btnBrowse.setText("Browse files...");
    GridData gridData = new GridData();
    gridData.horizontalSpan = 3;
    btnBrowse.setLayoutData(gridData);

    // Results table
    tblResults = new Table(shell, SWT.CHECK);
    tblResults.setHeaderVisible(true);
    tblResults.setLinesVisible(true);
    gridData = new GridData(GridData.FILL_BOTH);
    gridData.widthHint = 780;
    gridData.heightHint = 160;
    gridData.horizontalSpan = 3;
    tblResults.setLayoutData(gridData);

    final TableColumn col1 = new TableColumn(tblResults, SWT.LEFT);
    col1.setText("Episode");
    col1.setWidth(80);

    final TableColumn col2 = new TableColumn(tblResults, SWT.LEFT);
    col2.setText("Old Name");
    col2.setWidth(350);

    final TableColumn col3 = new TableColumn(tblResults, SWT.LEFT);
    col3.setText("New Name");
    col3.setWidth(350);

    // editable table
    final TableEditor editor = new TableEditor(tblResults);
    editor.horizontalAlignment = SWT.CENTER;
    editor.grabHorizontal = true;
    final int EDITABLECOLUMN = 2;

    final Button btnRenameAll = new Button(shell, SWT.PUSH);
    btnRenameAll.setText("Rename All");

    final Button btnRenameSelected = new Button(shell, SWT.PUSH);
    btnRenameSelected.setText("Rename Selected");

    final Button btnQuit = new Button(shell, SWT.PUSH);
    btnQuit.setText("Quit");
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
    btnQuit.setLayoutData(gridData);

    btnBrowse.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        // Clear the table for new use
        tblResults.removeAll();

        String pathPrefix = fd.open();
        if (pathPrefix != null) {
          File file = new File(pathPrefix);
          pathPrefix = file.getParent();

          String[] fileNames = fd.getFileNames();

          for (int i = 0; i < fileNames.length; i++) {
            fileNames[i] = pathPrefix + pathSeparator + fileNames[i];
          }

          logger.debug(fileNames.length + " files successfully added to list");

          // Call tvRenamer which actually finds the ep names ...
          TVRenamer tv = new TVRenamer(fileNames);

          // ... and get the resuts in a map
          episodeListing = tv.getEpisodeListing();
          renameFiles(LIST);

          // Sort the list descending by Episode
          tblResults.setSortColumn(col1);
          tblResults.setSortDirection(SWT.DOWN);
          // sortTable(col1, 1);
        }
      }
    });

    btnRenameAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        renameFiles(RENAME);
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

    btnRenameSelected.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        // Remove the unticked entries from the map
        TableItem[] items = tblResults.getItems();
        for (int i = 0; i < items.length; i++) {
          if (!items[i].getChecked()) {
            episodeListing.remove(Integer.parseInt(items[i].getText()));
          }
        }
        renameFiles(RENAME);
      }
    });

    btnQuit.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        display.dispose();
      }
    });

    Listener tblEditListener = new Listener() {
      @Override
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
                public void handleEvent(final Event e) {
                  switch (e.type) {
                    case SWT.FocusOut:
                      item.setText(column, text.getText());
                      logger.debug("item height"
                          + item.getFont().getFontData()[0].getHeight());
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
              logger.debug("text height"
                  + text.getFont().getFontData()[0].getHeight());
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

  private void renameFiles(int action) {
    int renamedFiles = 0;
    for (Episode ep : episodeListing.values()) {
      File file = new File(ep.getPath());
      String parentFilename = file.getParent() + pathSeparator;
      String extension = getExtension(file);
      String newFilename = ep.getShow() + " [" + ep.getSeason() + "x"
          + ep.getNumber() + "] " + ep.getTitle() + "." + extension;

      if (action == LIST) {
        // Add entry to table
        TableItem item = new TableItem(tblResults, SWT.NONE);
        item
            .setText(new String[] { ep.getNumber(), file.getName(), newFilename });
        item.setChecked(true);
      }

      else {
        file.renameTo(new File(parentFilename + newFilename));
        logger.info("Sucessfully renamed \'" + file.getName() + "\' to \'"
            + newFilename + "\'");
        renamedFiles++;
      }
    }
    if (renamedFiles > 0) {
      MessageBox msgSuccess = new MessageBox(shell, SWT.OK
          | SWT.ICON_INFORMATION);
      msgSuccess.setMessage(renamedFiles + " files successfully renamed!");
      msgSuccess.open();
    }
  }

  private String getExtension(File file) {
    String fileName = file.getName();
    String regex = ".*\\.(.*)";

    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(fileName);

    String extn = "";
    if (matcher.matches()) {
      extn = matcher.group(1);
    }
    return extn;
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
            // the snippet replaces the items with the new items, we do the same
            items = tblResults.getItems();
            break;
          }
        } else {
          if (collator.compare(value1, value2) > 0) {
            setSortedItem(i, j);
            // the snippet replaces the items with the new items, we do the same
            items = tblResults.getItems();
            break;
          }
        }
      }
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
}
