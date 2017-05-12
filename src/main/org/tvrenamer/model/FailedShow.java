package org.tvrenamer.model;

public class FailedShow extends LocalShow {

    private final TVRenamerIOException err;

    public TVRenamerIOException getError() {
        return err;
    }

    public FailedShow(String name, TVRenamerIOException err) {
        super(name);
        this.err = err;
    }
}
