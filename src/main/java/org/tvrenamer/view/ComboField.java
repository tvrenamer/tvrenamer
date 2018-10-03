package org.tvrenamer.view;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.TableItem;

public class ComboField extends TextField {

    @SuppressWarnings("SameParameterValue")
    ComboField(final String name, final String label) {
        super(Field.Type.COMBO, name, label);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private String itemDestDisplayedText(final TableItem item) {
        synchronized (item) {
            final Object data = item.getData();
            if (data == null) {
                return getCellText(item);
            }
            final Combo combo = (Combo) data;
            final int selected = combo.getSelectionIndex();
            final String[] options = combo.getItems();
            return options[selected];
        }
    }

    @Override
    public String getItemTextValue(final TableItem item) {
        return itemDestDisplayedText(item);
    }
}
