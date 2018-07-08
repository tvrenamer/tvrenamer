package org.tvrenamer.view;

import org.eclipse.swt.widgets.TableItem;

/**
 * A field that is intended to hold a checkbox.<p>
 *
 * Note that the checkbox is NOT actually part of the field.  The checkbox is there if and only if
 * the Table that the TableItem is part of, was created with SWT.CHECK.  When that is the case, the
 * first column (id 0) of each TableItem will contain a checkbox.<p>
 *
 * If you make a "normal" column be your id-0 column, it will display the checkbox on the left, and
 * then whatever is the content of the cell.<p>
 *
 * In order to give the appearance of a "checkbox column", we simply use an empty column.  The only
 * thing interesting about this type of Field is how we get the "text value" for sorting: by looking
 * at the status of the checkbox, of the <i>item</i>.  Again, the column does not actually have the
 * checkbox; it's on the item.<p>
 *
 * In order for this to work, such a field can only be at position zero in the row (which, of course,
 * also implies that there can only be one such field).  It must be created at position zero, and it
 * must remain there.<p>
 *
 * This restriction is enforced in Column.createColumn().
 */
public class CheckboxField extends Field {

    @SuppressWarnings("SameParameterValue")
    CheckboxField(final String name, final String label) {
        super(Field.Type.CHECKBOX, name, label);
    }

    public void setCellChecked(final TableItem item, final boolean isChecked) {
        item.setChecked(isChecked);
    }

    @Override
    public String getItemTextValue(final TableItem item) {
        return (item.getChecked()) ? "0" : "1";
    }
}
