package org.tvrenamer.model;

import java.net.SocketTimeoutException;

public class FailedShow extends ShowOption {

    private final TVRenamerIOException err;
    private final boolean didTimeout;

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
        }
        this.didTimeout = timeout;
    }

    public boolean isTimeout() {
        return didTimeout;
    }

    @Override
    public String toString() {
        return name + " (" + err + ") [FailedShow]";
    }
}
