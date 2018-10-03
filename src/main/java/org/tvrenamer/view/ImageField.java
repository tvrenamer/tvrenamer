package org.tvrenamer.view;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TableItem;

import org.tvrenamer.model.util.Constants;

public class ImageField extends Field {

    @SuppressWarnings("SameParameterValue")
    ImageField(final String name, final String label) {
        super(Field.Type.IMAGE, name, label);
    }

    /**
     * Sets the image to display in the given cell.
     *
     * @param item
     *   the item to set the image of for this field
     * @param newImage
     *   the Image to display in this field, of the given item
     */
    public void setCellImage(final TableItem item, final Image newImage) {
        if (column.isVisible() && (!item.isDisposed())) {
            item.setImage(column.id, newImage);
        }
    }

    /**
     * Sets the image in the given cell to be the image that represents the given status.
     *
     * @param item
     *   the item to set the image of for this field
     * @param newStatus
     *   the status of the item, from which we will obtain the Image to display
     *   in this field, of the given item
     */
    public void setCellImage(final TableItem item, final ItemState newStatus) {
        if (column.isVisible() && (!item.isDisposed())) {
            item.setImage(column.id, newStatus.getIcon());
        }
    }

    /**
     * Gets the Image currently being displayed in the given cell.
     *
     * @param item
     *   the item to get the text of for this field
     * @return
     *   the Image displayed this field, of the given item
     */
    public Image getCellImage(final TableItem item) {
        if (column.isVisible()) {
            return item.getImage(column.id);
        }
        return null;
    }

    /**
     * Gets the "text" of the given cell, for sorting.
     *
     * In the case of a ImageField, we are currently assuming that the Image
     * represents a status, and is provided by {@link ItemState}.  We send
     * the Image object to ItemState, and ask it to map it to a String.
     *
     * @param item
     *   the item to get the "text" of for this field
     * @return
     *   a text representing the status displayed this field, of the given item
     */
    @Override
    public String getItemTextValue(final TableItem item) {
        if (column.isVisible()) {
            return ItemState.getImagePriority(item.getImage(column.id));
        }
        // Field not currently displayed in table
        return Constants.EMPTY_STRING;
    }

}
