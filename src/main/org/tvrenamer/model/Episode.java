package org.tvrenamer.model;

import java.time.LocalDate;

public class Episode {
    private final int seasonNum;
    private final int episodeNum;
    private final String title;
    private final LocalDate airDate;

    public Episode(int seasonNum, int episodeNum, String title, LocalDate airDate) {
        this.seasonNum = seasonNum;
        this.episodeNum = episodeNum;
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
        return "Episode S" + seasonNum
            + "E" + episodeNum
            + "[episodeName=" + title
            + ", firstAired=" + airDate + "]";
    }
}
