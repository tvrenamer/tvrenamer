/*
 * ProgressObserver.java - Progression monitor
 *
 * Based on org.gjt.sp.util.ProgressObserver, though we add a finish method
 */

package org.tvrenamer.model;

/**
 * Interface used to monitor things that can progress.
 *
 * It should not be assumed that a ProgressObserver is ready to go upon construction,
 * or that it will clean up after itself once you stop using it.  It is required to
 * call initializeProgress() before anything else, and finishProgress() when you're done.
 */
public interface ProgressObserver {
    /**
     * Initialize with the maximum value.
     *
     * @param max
     *    the new maximum value
     */
    void initializeProgress(long max);

    /**
     * Update the progress value.
     *
     * @param value the new value
     */
    void setProgressValue(long value);

    /**
     * Update the status label.
     *
     * @param status the new status label
     */
    void setProgressStatus(String status);

    /**
     * Finish the activity
     *
     * @param succeeded
     *    whether the activity completed successfully
     */
    void finishProgress(boolean succeeded);
}
