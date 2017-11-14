package org.tvrenamer.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents a TV show's Season
 */
class Season {
    private static final Logger logger = Logger.getLogger(Season.class.getName());

    private final Show show;
    private final int seasonNum;
    private final Map<Integer, EpisodeOptions> episodes = new ConcurrentHashMap<>();

    /**
     * Create a Season object.
     *
     * Note that the information provided to this constructor is only used for
     * debugging.  In terms of the actual functionality, there's no reason a
     * Season needs to know what show it's part of, or which season number
     * it represents.
     *
     * @param show
     *           the Show to which this Season belongs
     * @param seasonNum
     *           the season number that this Season represents
     */
    Season(Show show, int seasonNum) {
        this.show = show;
        this.seasonNum = seasonNum;
    }

    /**
     * Look up an episode in this season.
     *
     * Nothing in the Season class knows anything about Episodes.  The caller
     * decides where to place the Episode, and this method just returns what
     * has been placed there.  It does not investigate the values contained
     * within the episode.
     *
     * @param episodeNum
     *           the episode number, within this Season, of the episode to return
     * @param preferDvd
     *           whether the caller prefers the DVD ordering or the over-the-air ordering
     * @return the episode found at the requested place
     */
    public Episode get(int episodeNum, boolean preferDvd) {
        EpisodeOptions options = episodes.get(episodeNum);
        Episode found = null;

        if (options != null) {
            found = options.get(preferDvd);
        }
        logger.fine("for season " + seasonNum + ", episode " + episodeNum
                    + ", found " + found);
        return found;
    }

    /**
     * Add an episode to a season's index of episodes.
     *
     * This method does not interpret or validate the episode number in any
     * way.  The number may correspond to the Episode's DVD ordering, or its
     * over-the-air ordering, or really neither, if the caller wants.  The
     * caller can choose to put any episode in any slot.
     *
     * @param episode
     *           the episode to add at the given index
     * @param useDvd
     *           whether episodeNum refers to the DVD ordering or
     *           the over-the-air ordering
     */
    public void addEpisode(Episode episode, boolean useDvd) {
        if (episode == null) {
            logger.warning("can not add null episode");
            return;
        }

        EpisodePlacement placement = episode.getEpisodePlacement(useDvd);
        int episodeNum = placement.episode;
        EpisodeOptions found = episodes.get(episodeNum);

        if (found == null) {
            found = new EpisodeOptions();
            episodes.put(episodeNum, found);
        }

        found.addEpisode(useDvd, episode);
    }

    /**
     * Standard object method to represent this Season as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "[Season " + seasonNum + " of " + show.getName() + "]";
    }
}
