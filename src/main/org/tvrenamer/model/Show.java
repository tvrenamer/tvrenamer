package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a TV Show, with a name, url and list of seasons.
 */
public class Show implements Comparable<Show> {
    private static Logger logger = Logger.getLogger(Show.class.getName());

    public static final int NO_SEASON = -1;
    public static final int NO_EPISODE = 0;

    private final String id;
    private final String name;
    private final String dirName;
    private final String url;

    private final List<Episode> episodes;
    private final Map<Integer, Map<Integer, Episode>> seasons;

    public Show(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
        dirName = StringUtils.sanitiseTitle(name);

        episodes = new ArrayList<>();
        seasons = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDirName() {
        return dirName;
    }

    public String getUrl() {
        return url;
    }

    private void addEpisodeToSeason(int seasonNum, int episodeNum, Episode episode) {
        Map<Integer, Episode> season = seasons.get(seasonNum);
        if (season == null) {
            season = new HashMap<>();
            seasons.put(seasonNum, season);
        }
        Episode found = season.remove(episodeNum);
        if (found == null) {
            season.put(episodeNum, episode);
        } else {
            logger.warning("two episodes found for show " + name + ", season "
                           + seasonNum + ", episode " + episodeNum + ": \""
                           + found.getTitle() + "\" and \""
                           + episode.getTitle() + "\"");
            // In this very unexpected case, we will not keep EITHER episode
            // in the table.  Remember that both will be in the unordered List
            // of episodes.  A future feature may be that when an episode is not
            // found in the seasons map, for whatever reason, to search through
            // the episode list.  This could be for "special" episodes, DVD extras,
            // etc.  But it could also be used for this case.
        }
    }

    public void addEpisode(String seasonString, int episodeNum,
                           String title, LocalDate firstAired)
    {
        int seasonNum = NO_SEASON;
        try {
            seasonNum = Integer.parseInt(seasonString);
        } catch (NumberFormatException nfe) {
            // Note, in this case, the Episode will be created and will be added to the
            // list of episodes, but will not be added to the season/episode organization.
            logger.info("episode \"" + title + "\" of show " + name
                        + " has non-numeric season: " + seasonString);
        }
        Episode episode = new Episode(seasonNum, episodeNum, title, firstAired);
        episodes.add(episode);
        if (seasonNum > NO_SEASON) {
            addEpisodeToSeason(seasonNum, episodeNum, episode);
        }
    }

    public Episode getEpisode(int seasonNum, int episodeNum) {
        Map<Integer, Episode> season = seasons.get(seasonNum);
        if (season == null) {
            logger.warning("no season " + seasonNum + " found for show " + name);
            return null;
        }
        return season.get(episodeNum);
    }

    public boolean hasSeasons() {
        return (episodes.size() > 0);
    }

    public int getEpisodeCount() {
        return episodes.size();
    }

    @Override
    public String toString() {
        return "Show [" + name + ", id=" + id + ", url=" + url + ", " + episodes.size() + " episodes]";
    }

    public String toLongString() {
        return "Show [id=" + id + ", name=" + name + ", url=" + url + ", episodes =" + episodes + "]";
    }

    @Override
    public int compareTo(Show other) {
        return Integer.parseInt(other.id) - Integer.parseInt(this.id);
    }

}
