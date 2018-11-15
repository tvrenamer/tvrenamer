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
 * If a class accepts a MoveObserver is required to call finishProgress() at
 * the end of the attempted move, regardless of the type of move or its success
 * or failure.  If the class intends to provide updates, it is necessary to call
 * initializeProgress() before calling one of the "set progress" methods, but
 * if updates will not be provided, then initializeProgress() may be skipped.
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
     * Finish the file move
     *
     * @param episode
     *    the FileEpisode that this observer was associated with
     */
    void finishProgress(FileEpisode episode);
}
