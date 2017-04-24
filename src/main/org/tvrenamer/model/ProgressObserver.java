/*
 * ProgressObserver.java - Progression monitor
 *
 * Based on org.gjt.sp.util.ProgressObserver, though we add a cleanUp method
 */

package org.tvrenamer.model;

/**
 * Interface used to monitor things that can progress.
 */
public interface ProgressObserver {
    /**
     * Update the progress value.
     *
     * @param value the new value
     */
    void setValue(long value);

    /**
     * Update the maximum value.
     *
     * @param value the new maximum value
     */
    void setMaximum(long value);

    /**
     * Update the status label.
     *
     * @param status the new status label
     */
    void setStatus(String status);

    /**
     * Clean up after we're done
     *
     */
    void cleanUp();
}
