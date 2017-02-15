package org.tvrenamer.model;

public class FailedShow extends Show {

    @SuppressWarnings("unused")
    private final TVRenamerIOException err;

    public FailedShow(String id, String name, String url, TVRenamerIOException err) {
        super(id, name, url);
        this.err = err;
    }

}
