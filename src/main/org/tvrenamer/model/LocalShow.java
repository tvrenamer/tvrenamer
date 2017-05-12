package org.tvrenamer.model;

/**
 * A "local" show is one that is not found in the provider.  It's generally a
 * sort of a substitute, and can also be thought of as "fake" in some way.
 * It is assigned an ID meant to not conflict with any of the "real" shows
 * we get from the provider.
 */
@SuppressWarnings("WeakerAccess")
public class LocalShow extends Show {

    private static int fakeSeriesId = 0;

    private static synchronized String fakeId() {
        return String.valueOf(--fakeSeriesId);
    }

    public LocalShow(String name) {
        super(fakeId(), name, null);
    }
}
