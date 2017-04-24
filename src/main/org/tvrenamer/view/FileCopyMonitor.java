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

    public FileCopyMonitor(Display display, Label label) {
        this.display = display;
        this.label = label;
        setValue(0);
        format.setMaximumFractionDigits(1);
    }

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

    @Override
    public void setMaximum(final long value) {
        maximum = value;
    }

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
