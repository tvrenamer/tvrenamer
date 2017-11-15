package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

public class EpisodePlacement {
    public final int season;
    public final int episode;

    public EpisodePlacement(int season, int episode) {
        this.season = season;
        this.episode = episode;
    }

    @Override
    public String toString() {
        return "S" + StringUtils.zeroPadTwoDigits(season)
            + "E" + StringUtils.zeroPadTwoDigits(episode);
    }
}
