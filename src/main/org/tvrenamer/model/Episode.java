package org.tvrenamer.model;

import java.util.Date;

public class Episode {
    private final String title;
    private final Date airDate;

    public Episode(String title, Date airDate) {
        this.title = title;
        this.airDate = airDate;
    }

    public String getTitle() {
        return title;
    }

    public Date getAirDate() {
        return airDate;
    }

    @Override
    public String toString() {
        return "Episode [title=" + title + ", airDate=" + airDate + "]";
    }
}
