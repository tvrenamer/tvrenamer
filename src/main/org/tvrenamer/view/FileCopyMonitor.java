package org.tvrenamer.view;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.tvrenamer.model.ProgressObserver;

import java.text.NumberFormat;

public class FileCopyMonitor implements ProgressObserver {
    private final NumberFormat format = NumberFormat.getPercentInstance();
    private final Display display;
    private final Label label;
    private long maximum;
    private int loopCount = 0;

    /**
     * Creates the monitor, with the label and the display.
     *
     * @param display - where the label is running
     * @param label - the widget to update
     */
    public FileCopyMonitor(Display display, Label label) {
        this.display = display;
        this.label = label;
        setValue(0);
        format.setMaximumFractionDigits(1);
    }

    /**
     * Update the progress value.
     *
     * @param value the new value
     */
    @Override
    public void setValue(final long value) {
        if (loopCount++ % 500 == 0) {
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (label.isDisposed()) {
                        return;
                    }
                    label.setText(format.format((double) value / maximum));
                }
            });
        }
    }

    /**
     * Update the maximum value.
     *
     * @param value the new maximum value
     */
    @Override
    public void setMaximum(final long value) {
        maximum = value;
    }

    /**
     * Update the status label.
     *
     * @param status the new status label
     */
    @Override
    public void setStatus(final String status) {
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (label.isDisposed()) {
                    return;
                }
                label.setToolTipText(status);
            }
        });
    }

    /**
     * Dispose of the label.  We need to do this whether the label was used or not.
     */
    @Override
    public void cleanUp() {
        if (!display.isDisposed()) {
            display.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (label.isDisposed()) {
                            return;
                        }
                        label.dispose();
                    }
                });
        }
    }

}
