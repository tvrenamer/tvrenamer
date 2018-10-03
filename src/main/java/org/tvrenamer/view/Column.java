package org.tvrenamer.view;

import static org.eclipse.swt.SWT.*;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import java.util.logging.Logger;

final class Column {
    private static final Logger logger = Logger.getLogger(Column.class.getName());

    final TableColumn swtColumn;
    final Field field;
    final int id;
    @SuppressWarnings("CanBeFinal")
    private Integer position = null;

    private int sortDirection = UP;

    /**
     * Create a new Column object, from a TableColumn that's already been created.
     * Private, to maintain the coupling between a Column and a TableColumn.  We
     * only use TableColumns that were created by this class, via the createColumn
     * static method, below.
     *
     * @param swtColumn
     *    the actual TableColumn from the Table that this Column represents
     * @param field
     *    the Field that this Column is meant to display
     * @param id
     *    the order where the column was added to the table; also presumed to
     *    be the initial position
     */
    private Column(TableColumn swtColumn, Field field, int id) {
        this.swtColumn = swtColumn;
        this.field = field;
        this.id = id;
        position = id;
    }

    /**
     * Is this Column currently visible in the table?
     *
     * @return true if the Column is currently visible, false otherwise
     */
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isVisible() {
        if (swtColumn.isDisposed()) {
            return false;
        }
        return (swtColumn.getWidth() > 0);
    }

    /**
     * Get the Column from a TableColumn.
     *
     * @param tcol the TableColumn instance whose Column we want
     * @return the Column that wraps the given TableColumn, or null if
     *   something goes wrong
     */
    public static Column getColumnWrapper(final TableColumn tcol) {
        Object columnObj = tcol.getData();
        if (columnObj instanceof Column) {
            return (Column) columnObj;
        }
        logger.warning("TableColumn's data is not a Column: " + columnObj);
        return null;
    }

    /**
     * Represent this Column as a String, displaying the field and position.
     *
     * @return a String representation of the Column
     */
    @Override
    public String toString() {
        return "Column [" + field + ", position=" + position + "]";
    }

    /**
     * Creates a Column in the given table.  The only way to create a Column,
     * as the constructor is private (by design).<p>
     *
     * Five arguments is kind of pushing it, but note they are five different
     * types, so it wouldn't be possible to accidentally get them in the wrong
     * order.<p>
     *
     * This method enforces the restriction that a CheckboxField may only be used
     * for the first (id 0), column of the Table, and will be immovable if present.
     * Please see comments in {@link CheckboxField}.java for an explanation.<p>
     *
     * There are no restrictions on Columns with other types of Fields.<p>
     *
     * This creates the TableColumn, and creates its behavior: when the user
     * clicks on the header, the table is sorted by that column.  If the user
     * clicks two or more times in a row on the same column, the effect is to
     * reverse the search with each click.  A click on a different column always
     * causes the new sort to be "up".<p>
     *
     * @param resultsTable
     *     the {@link ResultsTable} that this Column belongs to
     * @param swtTable
     *     the SWT Table that this Column belongs to
     * @param field
     *     the {@link Field} that this Column displays
     * @param label
     *     the text to display in the header of this Column
     * @param width
     *     the initial width for this Column
     * @return
     *     a new Column created per the parameters given
     */
    public static Column createColumn(final ResultsTable resultsTable,
                                      final Table swtTable,
                                      final Field field,
                                      final String label,
                                      final int width)
    {
        final TableColumn tcol = new TableColumn(swtTable, NONE);

        final int id = swtTable.indexOf(tcol);
        if (id < 0) {
            logger.severe("unable to locate column in table: " + tcol);
            throw new IllegalStateException("could not locate column after adding it");
        }
        if (field.type == Field.Type.CHECKBOX) {
            if (id > 0) {
                throw new IllegalStateException("checkbox field can only be column zero");
            }
        }

        final Column col = new Column(tcol, field, id);

        tcol.setData(col);
        tcol.setText(label);
        tcol.setWidth(width);
        tcol.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final boolean sameColumn = (tcol == swtTable.getSortColumn());
                if (sameColumn) {
                    // If the last time we sorted, it was on THIS column,
                    // then we reverse the direction of the previous sort.
                    col.sortDirection = (col.sortDirection == DOWN) ? UP : DOWN;
                } else {
                    // If the last sort was on any other column, then we
                    // sort UP.  We always sort UP on a "new" sort.
                    col.sortDirection = UP;
                }
                resultsTable.sortTable(col, col.sortDirection);
            }
        });

        return col;
    }
}
