package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Logger;

public class StatusLabel {
    private static final Logger logger = Logger.getLogger(StatusLabel.class.getName());

    private final Deque<String> statusStack = new ArrayDeque<>();

    private Shell parentShell;
    private Label statusLabel;
    private boolean started = false;

    private void refreshStatusLabel() {
        if (statusStack.isEmpty()) {
            statusLabel.setText(EMPTY_STRING);
        } else {
            statusLabel.setText(statusStack.peekFirst());
        }
        parentShell.layout();
    }

    public void add(final String statusText) {
        if (statusText == null) {
            logger.info("cannot set null status");
            return;
        }
        statusStack.remove(statusText);
        statusStack.addFirst(statusText);
        if (!started) {
            // could potentially happen during startup?
            return;
        }
        refreshStatusLabel();
    }

    public void clear(final String statusText) {
        if (statusText == null) {
            // not used and not recommended; but provided, just in case
            statusStack.clear();
        } else {
            boolean found = statusStack.remove(statusText);
            if (!found) {
                logger.fine("did not find \"" + statusText + "\" in status stack");
            }
        }
        if (!started) {
            // could potentially happen during startup?
            return;
        }
        refreshStatusLabel();
    }

    /**
     * Start status label
     *
     * @param parent
     *            the parent {@link Shell}
     * @param numColumns
     *            how many columns the label should span
     */
    public void open(final Shell parent, final int numColumns) {
        parentShell = parent;
        statusLabel = new Label(parentShell, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, true, true,
                                               numColumns, 1));
        started = true;
        refreshStatusLabel();
    }
}
