package org.tvrenamer.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TaskItem;

import org.tvrenamer.model.ProgressUpdater;

public class ProgressBarUpdater implements ProgressUpdater {

    private final ResultsTable ui;
    private final Display display;
    private final TaskItem taskItem;
    private final ProgressBar progressBar;
    private final int barSize;

    public ProgressBarUpdater(ResultsTable ui) {
        this.ui = ui;
        this.display = ui.getDisplay();
        this.taskItem = ui.getTaskItem();
        this.progressBar = ui.getProgressBar();
        this.barSize = progressBar.getMaximum();

        if (taskItem != null) {
            taskItem.setProgressState(SWT.NORMAL);
            taskItem.setOverlayImage(FileMoveIcon.getIcon(FileMoveIcon.Status.RENAMING));
        }
    }

    /**
     * Cleans up the progress bar and the task item
     *
     */
    @Override
    public void finish() {
        display.asyncExec(() -> {
            if (progressBar != null) {
                progressBar.setSelection(0);
            }
            if (taskItem != null) {
                taskItem.setOverlayImage(null);
                taskItem.setProgressState(SWT.DEFAULT);
            }
            ui.refreshAll();
        });
    }

    /**
     * Updates the progress bar and the task item
     *
     * @param totalNumFiles
     *            the total number of files to be moved during the duration
     *            of this progress bar
     * @param nRemaining
     *            the number of files left to be moved
     */
    @Override
    public void setProgress(final int totalNumFiles, final int nRemaining) {
        if (display.isDisposed()) {
            return;
        }

        final float progress = (float) (totalNumFiles - nRemaining) / totalNumFiles;
        display.asyncExec(() -> {
            if (progressBar.isDisposed()) {
                return;
            }
            progressBar.setSelection(Math.round(progress * barSize));
            if (taskItem != null) {
                if (taskItem.isDisposed()) {
                    return;
                }
                taskItem.setProgress(Math.round(progress * 100));
            }
        });
    }
}
