package org.tvrenamer.model;

import java.time.LocalDate;

public class Episode {
    private final String title;
    private final LocalDate airDate;

    public Episode(String title, LocalDate airDate) {
        this.title = title;
        this.airDate = airDate;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getAirDate() {
        return airDate;
    }

    @Override
    public String toString() {
        return "Episode [title=" + title + ", airDate=" + airDate + "]";
    }
}
