package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.IMDB_BASE_URL;

import org.tvrenamer.controller.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents a TV Show, with a name, url and list of seasons.
 */
public class Show implements Comparable<Show> {
    private static Logger logger = Logger.getLogger(Show.class.getName());

    public static final int NO_SEASON = -1;
    public static final int NO_EPISODE = 0;

    private enum NumberingScheme {
        GUESS,
        REGULAR,
        DVD_RELEASE,
        ABSOLUTE
    }

    private final String id;
    private final String name;
    private final String dirName;
    private final String imdb;

    private final Map<String, Episode> episodes;
    private final Map<Integer, Map<Integer, Episode>> seasons;

    // Not final.  Could be changed during the program's run.
    private NumberingScheme numberingScheme = NumberingScheme.GUESS;

    public Show(String id, String name, String imdb) {
        this.id = id;
        this.name = name;
        this.imdb = imdb;
        dirName = StringUtils.sanitiseTitle(name);

        episodes = new ConcurrentHashMap<>();
        seasons = new ConcurrentHashMap<>();
    }

    public Show(String id, String name) {
        this(id, name, null);
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
        return (imdb == null) ? "" : IMDB_BASE_URL + imdb;
    }

    private void addEpisodeToSeason(int seasonNum, int episodeNum, Episode episode) {
        // Check to see if there's already an existing episode.  Only applies if we
        // have a valid season number and episode number.
        Map<Integer, Episode> season = seasons.get(seasonNum);
        if (season == null) {
            season = new ConcurrentHashMap<>();
            seasons.put(seasonNum, season);
        }
        Episode found = season.remove(episodeNum);
        if (found == null) {
            // This is the expected case; we should only be adding the episode to
            // the index a single time.
            season.put(episodeNum, episode);
        } else if (found == episode) {
            // Well, this is unfortunate; if it happens, investigate why.  But it's
            // fine.  We still have a unique object.
            season.put(episodeNum, episode);
        } else if (found.getTitle().equals(episode.getTitle())) {
            // This is less fine.  We've apparently created two objects to represent
            // the same data.  This should be fixed.
            logger.warning("replacing episode object for show " + name + ", season "
                           + seasonNum + ", episode " + episodeNum + " (\""
                           + found.getTitle() + "\")");
            season.put(episodeNum, episode);
        } else {
            // In this very unexpected case, we will not keep EITHER episode
            // in the table.  Remember that both will be in the unordered List
            // of episodes.  A future feature may be that when an episode is not
            // found in the seasons map, for whatever reason, to search through
            // the episode list.  This could be for "special" episodes, DVD extras,
            // etc.  But it could also be used for this case.
            logger.warning("two episodes found for show " + name + ", season "
                           + seasonNum + ", episode " + episodeNum + ": \""
                           + found.getTitle() + "\" and \""
                           + episode.getTitle() + "\"");
        }
    }

    private void indexEpisodesBySeason(NumberingScheme effective) {
        for (Episode episode : episodes.values()) {
            if (episode == null) {
                logger.severe("internal error creating episodes for " + name);
                return;
            }

            String seasonNumString;
            String episodeNumString;
            if (effective == NumberingScheme.REGULAR) {
                seasonNumString = episode.getSeasonNumber();
                episodeNumString = episode.getEpisodeNumber();
            } else if (effective == NumberingScheme.DVD_RELEASE) {
                seasonNumString = episode.getDvdSeasonNumber();
                episodeNumString = episode.getDvdEpisodeNumber();
            } else {
                // not supported
                seasonNumString = "";
                episodeNumString = "";
            }

            Integer seasonNum = StringUtils.stringToInt(seasonNumString);
            Integer episodeNum = StringUtils.stringToInt(episodeNumString);

            if ((seasonNum == null) || (episodeNum == null)) {
                // Note, in this case, the Episode will be created and will be added to the
                // list of episodes, but will not be added to the season/episode organization.
                logger.fine("episode \"" + episode.getTitle() + "\" of show " + name
                            + " has non-numeric season: " + seasonNumString);
                continue;
            }

            addEpisodeToSeason(seasonNum, episodeNum, episode);
        }
    }

    private void indexEpisodesBySeason() {
        if (numberingScheme == NumberingScheme.GUESS) {
            int withDVD = 0;
            int withoutDVD = 0;
            for (Episode episode : episodes.values()) {
                if (episode == null) {
                    logger.severe("internal error creating episodes for " + name);
                    return;
                }

                Integer seasonNum = StringUtils.stringToInt(episode.getDvdSeasonNumber());
                Integer episodeNum = StringUtils.stringToInt(episode.getDvdEpisodeNumber());

                if ((seasonNum == null) || (episodeNum == null)) {
                    withoutDVD++;
                } else {
                    withDVD++;
                }
            }
            // Make the threshold 50%.  That's probably low, but the program has a history
            // of preferring DVD episode numbers, and 50% is really easy to do.
            if (withDVD > withoutDVD) {
                indexEpisodesBySeason(NumberingScheme.DVD_RELEASE);
            } else {
                indexEpisodesBySeason(NumberingScheme.REGULAR);
            }
        } else {
            indexEpisodesBySeason(numberingScheme);
        }
    }

    private void dealWithConflicts(List<EpisodeInfo> conflicts) {
        for (EpisodeInfo info : conflicts) {
            Episode episode = episodes.get(info.episodeId);
            String msg = episode.getDifferenceMessage(info);
            if (msg == null) {
                logger.warning("handling again: " + info.episodeName);
            } else {
                logger.warning(msg);
            }
        }
    }

    void addEpisodes(final EpisodeInfo[] infos, final boolean logConflicts) {
        List<EpisodeInfo> conflicts = new LinkedList<>();
        for (EpisodeInfo info : infos) {
            if (info != null) {
                String episodeId = info.episodeId;
                Episode existing = episodes.get(episodeId);
                if (existing == null) {
                    Episode episode = new Episode(info);
                    episodes.put(episodeId, episode);
                } else {
                    conflicts.add(info);
                }
            }
        }
        indexEpisodesBySeason();
        if (logConflicts) {
            dealWithConflicts(conflicts);
        }
    }

    public void addEpisodes(final EpisodeInfo[] infos) {
        addEpisodes(infos, true);
    }

    public synchronized void preferHeuristicOrdering() {
        seasons.clear();
        numberingScheme = NumberingScheme.GUESS;
        indexEpisodesBySeason();
    }

    public synchronized void preferProductionOrdering() {
        seasons.clear();
        numberingScheme = NumberingScheme.REGULAR;
        indexEpisodesBySeason(NumberingScheme.REGULAR);
    }

    public synchronized void preferDvdOrdering() {
        seasons.clear();
        numberingScheme = NumberingScheme.DVD_RELEASE;
        indexEpisodesBySeason(NumberingScheme.DVD_RELEASE);
    }

    // Private -- not supported -- haven't tested.  TODO.
    private synchronized void preferAbsoluteOrdering() {
        seasons.clear();
        numberingScheme = NumberingScheme.ABSOLUTE;
        indexEpisodesBySeason(NumberingScheme.ABSOLUTE);
    }

    public Episode getEpisode(int seasonNum, int episodeNum) {
        Map<Integer, Episode> season = seasons.get(seasonNum);
        if (season == null) {
            logger.warning("no season " + seasonNum + " found for show " + name);
            return null;
        }
        Episode episode = season.get(episodeNum);
        logger.fine("for season " + seasonNum + ", episode " + episodeNum
                    + ", found " + episode);

        return episode;
    }

    public boolean hasSeasons() {
        return (episodes.size() > 0);
    }

    public int getEpisodeCount() {
        return episodes.size();
    }

    @Override
    public String toString() {
        return "Show [" + name + ", id=" + id + ", imdb=" + imdb + ", " + episodes.size() + " episodes]";
    }

    public String toLongString() {
        return "Show [id=" + id + ", name=" + name + ", imdb=" + imdb + ", episodes =" + episodes + "]";
    }

    @Override
    public int compareTo(Show other) {
        return Integer.parseInt(other.id) - Integer.parseInt(this.id);
    }
}
