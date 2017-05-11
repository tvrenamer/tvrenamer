package org.tvrenamer.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.TaskItem;

import org.tvrenamer.model.FileMoveIcon;

import java.util.logging.Logger;

public class ProgressBarUpdater {
    private static Logger logger = Logger.getLogger(ProgressBarUpdater.class.getName());

    private final UIStarter ui;
    private final Display display;
    private final TaskItem taskItem;
    private final ProgressBar progressBar;
    private final int barSize;

    public ProgressBarUpdater(UIStarter ui) {
        this.ui = ui;
        this.display = ui.getDisplay();
        this.taskItem = ui.getTaskItem();
        this.progressBar = ui.getProgressBar();
        this.barSize = progressBar.getMaximum();

        if (taskItem != null) {
            taskItem.setProgressState(SWT.NORMAL);
            taskItem.setOverlayImage(FileMoveIcon.RENAMING.icon);
        }
    }

    /**
     * Cleans up the progress bar and the task item
     *
     */
    public void finish() {
        display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (taskItem != null) {
                        taskItem.setOverlayImage(null);
                        taskItem.setProgressState(SWT.DEFAULT);
                    }
                    ui.refreshTable();
                }
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
    public void setProgress(final int totalNumFiles, final int nRemaining) {
        if (display.isDisposed()) {
            return;
        }

        final float progress = (float) (totalNumFiles - nRemaining) / totalNumFiles;
        display.asyncExec(new Runnable() {
                @Override
                public void run() {
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
                }
            });
    }
}
