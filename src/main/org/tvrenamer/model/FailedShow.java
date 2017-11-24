package org.tvrenamer.model;

public class FailedShow extends ShowOption {

    private final TVRenamerIOException err;

    @SuppressWarnings("unused")
    public TVRenamerIOException getError() {
        return err;
    }

    public FailedShow(String name, TVRenamerIOException err) {
        super(null, name);
        this.err = err;
    }

    @Override
    public String toString() {
        return name + " (" + err + ") [FailedShow]";
    }
}
