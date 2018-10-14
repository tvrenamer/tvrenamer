package org.tvrenamer.model;

import java.net.SocketTimeoutException;

public class FailedShow extends ShowOption {

    private final TVRenamerIOException err;
    private final boolean didTimeout;

    /**
     * Create a FailedShow
     *
     * A FailedShow is a stand-in object that we use when we do not have any
     * better options.  It contains enough information to know what we were
     * looking for, and give us some idea what went wrong.
     *
     * @param name
     *   the name of the show we were looking for (and did not find)
     * @param err
     *   an exception, if one occurred while trying to look for the show;
     *   certainly may be null
     */
    public FailedShow(String name, TVRenamerIOException err) {
        super(null, name);
        this.err = err;
        boolean timeout = false;
        Throwable exception = err;
        while (exception != null) {
            if (exception instanceof SocketTimeoutException) {
                timeout = true;
                break;
            }
            exception = exception.getCause();
        }
        this.didTimeout = timeout;
    }

    /**
     * Return whether the failure to look up the show was due to a timeout
     * trying to download data from the provider.
     *
     * @return true if we timed out trying to look up the show; false otherwise
     */
    public boolean isTimeout() {
        return didTimeout;
    }

    @Override
    public String toString() {
        return name + " (" + err + ") [FailedShow]";
    }
}
