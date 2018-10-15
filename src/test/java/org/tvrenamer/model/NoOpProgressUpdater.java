package org.tvrenamer.model;

public abstract class NoOpProgressUpdater implements ProgressUpdater {
    public void setProgress(final int totalNumFiles, final int nRemaining) {
        // no-op
    }

    public abstract void finish();
}
