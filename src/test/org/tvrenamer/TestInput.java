package org.tvrenamer;

/**
 * Created by lsrom on 11/2/16.
 */
public class TestInput {
    public final String input;
    public final String show;
    public final String season;
    public final String episode;

    public final String episodeTitle;
    public final String episodeResolution;

    public TestInput(String input, String show, String season, String episode) {
        this(input, show, season, episode, null, null);
    }

    public TestInput(String input, String show, String season, String episode, String episodeTitle, String episodeResolution) {
        this.input = input;
        this.show = show.toLowerCase();
        this.season = season;
        this.episode = episode;

        this.episodeTitle = episodeTitle;
        this.episodeResolution = episodeResolution;
    }
}
