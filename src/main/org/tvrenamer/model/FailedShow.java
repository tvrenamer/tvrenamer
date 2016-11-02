package org.tvrenamer.model;

public class FailedShow extends Show {

    @SuppressWarnings("unused")
    private final TVRenamerIOException e;

    public FailedShow(String id, String name, String url, TVRenamerIOException e) {
        super(id, name, url);
        this.e = e;
    }

}
