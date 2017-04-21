package org.tvrenamer.model;

public class FailedShow extends Show {

    @SuppressWarnings("unused")
    private final TVRenamerIOException err;

    public FailedShow(String id, String name, TVRenamerIOException err) {
        super(id, name);
        this.err = err;
    }

}
