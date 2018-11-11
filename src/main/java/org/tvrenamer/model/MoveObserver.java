/*
 * MoveObserver.java - observer of a "file move" process
 *
 * Originally based on org.gjt.sp.util.ProgressObserver, but we've specialized
 * it to be specific to a "file move".
 */

package org.tvrenamer.model;

/**
 * Interface used to monitor a file move.
 *
 * It should not be assumed that a MoveObserver is ready to go
 * upon  construction,  or that it will clean up after itself
 * once you stop using it.  It is required to call initializeProgress()
 * before anything else, and finishProgress() when you're done.
 */
public interface MoveObserver {
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
