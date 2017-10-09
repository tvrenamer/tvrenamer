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
    private final Map<Integer, Episode> episodes = new ConcurrentHashMap<>();

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
     * @return the episode found at the requested place
     */
    public Episode get(int episodeNum) {
        Episode found = episodes.get(episodeNum);
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
     * @param episodeNum
     *           the episode, of this season, of the episode to add
     */
    public void addEpisode(Episode episode, int episodeNum) {
        if (episode == null) {
            logger.warning("can not add null episode");
            return;
        }

        Episode found = episodes.remove(episodeNum);

        if (found == null) {
            // This is the expected case; we should only be adding the episode to
            // the index a single time.
            episodes.put(episodeNum, episode);
        } else if (found == episode) {
            // Well, this is unfortunate; if it happens, investigate why.  But it's
            // fine.  We still have a unique object.  No action required.
            episodes.put(episodeNum, episode);
        } else if (found.getTitle().equals(episode.getTitle())) {
            // This is less fine.  We've apparently created two objects to represent
            // the same data.  This should be fixed.
            logger.warning("replacing episode " + found.getEpisodeId()
                           + " for show " + show.getName() + ", season "
                           + seasonNum + ", episode " + episodeNum + " (\""
                           + found.getTitle() + "\") with " + episode.getEpisodeId());
            episodes.put(episodeNum, episode);
        } else {
            // In this very unexpected case, we will not keep EITHER episode
            // in the table.  Remember that both will be in the unordered List
            // of episodes.  A future feature may be that when an episode is not
            // found in the seasons map, for whatever reason, to search through
            // the episode list.  This could be for "special" episodes, DVD extras,
            // etc.  But it could also be used for this case.
            logger.warning("two episodes found for show " + show.getName() + ", season "
                           + seasonNum + ", episode " + episodeNum + ": \""
                           + found.getTitle() + "\" (" + found.getEpisodeId() + ") and \""
                           + episode.getTitle() + "\" (" + episode.getEpisodeId() + ")");
        }
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
