package org.tvrenamer.model;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a TV show's Season
 *
 */
public class Season {
    private static final Logger logger = Logger.getLogger(Season.class.getName());

    private final Show show;
    private final int seasonNum;
    private final int maxEpisodeNum;
    private final EpisodeOptions[] episodes;

    /**
     * Create a Season object.
     *
     * Note that some of the information provided to this contructor is only used for debugging.
     * In terms of the actual functionality, there's no reason a Season needs to know what show
     * it's part of, or which season number it represents.
     *
     * @param show
     *           the Show to which this Season belongs
     * @param seasonNum
     *           the season number that this Season represents
     * @param tmp
     *           a mapping between episode number and the EpisodeOptions for that number
     */
    Season(Show show, int seasonNum, Map<Integer, EpisodeOptions> tmp) {
        this.show = show;
        this.seasonNum = seasonNum;

        maxEpisodeNum = tmp.keySet().stream().max((i1, i2) -> (i1 - i2)).orElse(0);

        episodes = new EpisodeOptions[maxEpisodeNum + 1];
        for (int i=0; i <= maxEpisodeNum; i++) {
            episodes[i] = tmp.remove(i);
        }
    }

    /**
     * Look up an episode in this season in the given ordering.
     *
     * @param preference
     *           whether the caller prefers the DVD ordering, or over-the-air ordering
     * @param episodeNum
     *           the episode number, within this Season, of the episode to return
     * @return the Episode that best matches the request criteria, or null if none does
     */
    public Episode get(EpisodeOptions.Ordering preference, int episodeNum) {
        if (episodeNum > maxEpisodeNum) {
            return null;
        }

        EpisodeOptions options = episodes[episodeNum];
        if (options == null) {
            return null;
        }

        Episode found = options.get(preference);
        logger.fine("for season " + seasonNum + ", episode " + episodeNum
                    + ", found " + found);
        return found;
    }

    /**
     * Standard object method to represent this Season as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "Season [" + seasonNum + "]";
    }
}
