/*
 * ProgressObserver.java - Progression monitor
 *
 * Based on org.gjt.sp.util.ProgressObserver, though we add a cleanUp method
 */

package org.tvrenamer.model;

/**
 * Interface used to monitor things that can progress.
 *
 * It should not be assumed that a ProgressObserver is ready to go upon construction,
 * or that it will clean up after itself once you stop using it.  It is required to
 * call initialize() before anything else, and cleanUp() when you're done.
 */
public interface ProgressObserver {
    /**
     * Initialize with the maximum value.
     *
     * @param max
     *    the new maximum value
     */
    void initialize(long max);

    /**
     * Update the progress value.
     *
     * @param value the new value
     */
    void setValue(long value);

    /**
     * Update the status label.
     *
     * @param status the new status label
     */
    void setStatus(String status);

    /**
     * Finish the activity
     *
     */
    void finishProgress(boolean succeeded);
}
