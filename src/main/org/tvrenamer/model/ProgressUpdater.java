package org.tvrenamer.model;

/**
 * Interface used to monitor things that can progress.
 */
public interface ProgressUpdater {
    /**
     * Updates the progress bar and the task item
     *
     * @param totalNumFiles
     *            the total number of files to be moved during the duration
     *            of this progress bar
     * @param nRemaining
     *            the number of files left to be moved
     */
    void setProgress(final int totalNumFiles, final int nRemaining);

    /**
     * Operation is finished
     *
     */
    void finish();
}
