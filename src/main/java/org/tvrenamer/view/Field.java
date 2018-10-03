package org.tvrenamer.view;

import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public abstract class Field {
    private static final Logger logger = Logger.getLogger(Field.class.getName());

    public enum Type {
        CHECKBOX,
        IMAGE,
        TEXT,
        COMBO
    }

    public final String name;
    public final Type type;
    public final String label;
    protected Column column = null;

    /**
     * Constructs a new Field.
     *
     * @param type
     *   the {@link Type} of the field to construct
     * @param name
     *   a String used for the name of this instance; it is only used in toString().<p>
     *
     *   Instances of Field are constant and are intended to be used like an Enum, but
     *   Field needs too many features to actually use Enum.  A real Enum has the nice
     *   feature that it can be printed and displays its variable name.  For a class
     *   instance, we need to do this more explicitly to get it to work.
     * @param label
     *   the text to use in the header of a column that displays this Field
     */
    protected Field(final Type type, final String name, final String label) {
        this.type = type;
        this.name = name;
        this.label = label;
    }

    /**
     * Creates a new column associated with this Field.  This means both the
     * TableColumn widget in the SWT Table, as well as our "Column" data
     * structure that encapsulates it.  Does not return anything, but after
     * it returns, the TableColumn can be retrieved via {@link #getTableColumn}.
     *
     * @param resultsTable
     *    the instance of ResultsTable to create the column within
     * @param swtTable
     *    the SWT Table that the TableColumn will go into
     * @param initialWidth
     *    the width to create the column with
     */
    public void createColumn(final ResultsTable resultsTable,
                             final Table swtTable,
                             final int initialWidth)
    {
        if (this.column != null) {
            logger.warning("cannot re-set column " + this);
            throw new IllegalStateException("two columns created for " + this);
        }
        this.column = Column.createColumn(resultsTable, swtTable,
                                          this, label, initialWidth);
    }

    /**
     * Sets the editor on this Field of the given item, assuming that this Field is
     * currently being shown.  Returns without error or comment if this Field is not
     * currently displayed in the table.
     *
     * @param item
     *    the row of the table on which to set the editor for this field
     * @param editor
     *    the TableEditor instance that provides the glue between the control and the item
     * @param control
     *    the widget to use to edit the cell value
     */
    public void setEditor(final TableItem item,
                          final TableEditor editor,
                          final Control control)
    {
        if ((column.isVisible()) && (!item.isDisposed())) {
            editor.setEditor(control, item, column.id);
        }
    }

    /**
     * Retrieves the TableColumn associated with this Field, if there is one.
     *
     * @return the TableColumn associated with this Field, if there is one;
     *    null if this Field is not currently being displayed in the table.
     */
    public TableColumn getTableColumn() {
        if (column.isVisible()) {
            return column.swtColumn;
        }
        return null;
    }

    public abstract String getItemTextValue(final TableItem item);

    /**
     * Represent this Field as a String
     *
     * @return a String representation of the Field
     */
    @Override
    public String toString() {
        return name;
    }
}
