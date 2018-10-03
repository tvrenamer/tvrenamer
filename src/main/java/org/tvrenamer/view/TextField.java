package org.tvrenamer.view;

import org.eclipse.swt.widgets.TableItem;

import org.tvrenamer.model.util.Constants;

public class TextField extends Field {

    TextField(final Type type, final String name, final String label) {
        super(type, name, label);
    }

    @SuppressWarnings("SameParameterValue")
    TextField(final String name, final String label) {
        this(Field.Type.TEXT, name, label);
    }

    /**
     * Sets the text to display in the given cell.
     *
     * @param item
     *   the item to set the text of for this field
     * @param newText
     *   the text to display in this field, of the given item
     */
    public void setCellText(final TableItem item, final String newText) {
        if (column.isVisible() && (!item.isDisposed())) {
            item.setText(column.id, newText);
        }
    }

    /**
     * Gets the text currently being displayed in the given cell.
     *
     * @param item
     *   the item to get the text of for this field
     * @return
     *   the text displayed this field, of the given item
     */
    public String getCellText(final TableItem item) {
        if (column.isVisible()) {
            return item.getText(column.id);
        }
        return Constants.EMPTY_STRING;
    }

    /**
     * Gets the "text" of the given cell, for sorting.
     *
     * In the case of a TextField, the "text" of the cell really is just that.
     * (For other types of cells, it's more complicated.)
     *
     * @param item
     *   the item to get the text of for this field
     * @return
     *   the text displayed this field, of the given item
     */
    @Override
    public String getItemTextValue(final TableItem item) {
        return getCellText(item);
    }
}
