package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.logging.Logger;

public final class Columns {
    private static final Logger logger = Logger.getLogger(Columns.class.getName());

    static final int CHECKBOX_COLUMN = 0;
    static final int CURRENT_FILE_COLUMN = 1;
    static final int NEW_FILENAME_COLUMN = 2;
    static final int STATUS_COLUMN = 3;

    public static synchronized void createColumns(ResultsTable resultsTable, Table swtTable) {
        final TableColumn checkboxColumn = new TableColumn(swtTable, SWT.LEFT, CHECKBOX_COLUMN);
        checkboxColumn.setText(CHECKBOX_HEADER);
        checkboxColumn.setWidth(60);
        checkboxColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.sortTable(checkboxColumn);
            }
        });

        final TableColumn sourceColumn = new TableColumn(swtTable, SWT.LEFT, CURRENT_FILE_COLUMN);
        sourceColumn.setText(SOURCE_HEADER);
        sourceColumn.setWidth(550);
        sourceColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.sortTable(sourceColumn);
            }
        });

        final TableColumn destinationColumn = new TableColumn(swtTable, SWT.LEFT, NEW_FILENAME_COLUMN);
        destinationColumn.setWidth(550);
        destinationColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.sortTable(destinationColumn);
            }
        });

        final TableColumn statusColumn = new TableColumn(swtTable, SWT.LEFT, STATUS_COLUMN);
        statusColumn.setText(STATUS_HEADER);
        statusColumn.setWidth(60);
        statusColumn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultsTable.sortTable(statusColumn);
            }
        });
    }
}
