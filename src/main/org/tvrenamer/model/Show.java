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

    /* When going from a filename to a Show object, there's a class in the way
     * which helps avoid duplication.  If we have two files like:
     *    "Lost.S06E05.mp4"
     *    "Lost.S06E06.mp4"
     * ... they will share a common ShowName object, and therefore be mapped to
     * the same Show object.  Which is good.
     *
     * But in a case like this one:
     *    "Real.Oneals.S01E01.avi"
     *    "The Real O'Neals.S01E02.avi"
     * ... they probably won't, because of the "The".  There will be two
     * separate ShowName objects created, one with "the" and one without.
     * But, that doesn't mean we have to create two Show objects.  Once
     * we query the provider and determine the ID of the show we're going
     * to map to, we can look in "knownShows" to see if a show has already
     * been created for that ID.  If it has, return that object, and don't
     * create a new one.
     *
     * Without using this hashmap, we might very well create two or more
     * instances of the same Show.  In fact, it happened all the time.
     * It doesn't cause anything to break, it just results in a lot of
     * unnecessary work.
     *
     * Note we even put LocalShows in here, too, even though it does not
     * serve the purpose of avoiding unnecessary work.  That's because
     * another usage of this map is that its values represent all the
     * Shows we have created, which can be useful information to have.
     */
    private static Map<String, Show> knownShows = new ConcurrentHashMap<>();

    private final String id;
    private final String name;
    private final String dirName;
    private final String imdb;

    private final Map<String, Episode> episodes;
    private final Map<Integer, Map<Integer, Episode>> seasons;

    // Not final.  Could be changed during the program's run.
    private NumberingScheme numberingScheme = NumberingScheme.GUESS;

    protected Show(String id, String name, String imdb) {
        this.id = id;
        this.name = name;
        this.imdb = imdb;
        dirName = StringUtils.sanitiseTitle(name);

        episodes = new ConcurrentHashMap<>();
        seasons = new ConcurrentHashMap<>();

        knownShows.put(id, this);
    }

    /**
     * "Factory"-type static method to get an instance of a Show.  Looks
     * up the ID in a hash table, and returns the object if it's already
     * been created.  Otherwise, we create a new Show, put it into the
     * table, and return it.
     *
     * @return a Show with the given ID
     */
    public static Show getShowInstance(String id, String name, String imdb) {
        Show matchedShow = null;
        synchronized (knownShows) {
            matchedShow = knownShows.get(id);
            if (matchedShow == null) {
                matchedShow = new Show(id, name, imdb);
            }
        }
        return matchedShow;
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
            logger.warning("replacing episode " + found.getEpisodeId()
                           + " for show " + name + ", season "
                           + seasonNum + ", episode " + episodeNum + " (\""
                           + found.getTitle() + "\") with " + episode.getEpisodeId());
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
            // Make the threshold 75%.  That's probably low, but the program has a history
            // of preferring DVD episode numbers, and 75% is easy to do.
            if (withDVD > (withoutDVD * 3)) {
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
            if (info == null) {
                logger.warning("received null episode info");
            } else {
                Episode episode = episodes.get(info.episodeId);
                String msg = episode.getDifferenceMessage(info);
                if (msg == null) {
                    logger.warning("handling again: " + info.episodeName);
                } else {
                    logger.warning(msg);
                }
            }
        }
    }

    public boolean addOneEpisode(final EpisodeInfo info) {
        if (info != null) {
            String episodeId = info.episodeId;
            Episode existing = episodes.get(episodeId);
            if (existing == null) {
                Episode episode = new Episode(info);
                episodes.put(episodeId, episode);
                return true;
            }
        }
        return false;
    }

    public void addEpisodes(final EpisodeInfo[] infos) {
        List<EpisodeInfo> conflicts = new LinkedList<>();
        for (EpisodeInfo info : infos) {
            boolean added = addOneEpisode(info);
            if (!added) {
                conflicts.add(info);
            }
        }
        indexEpisodesBySeason();
        dealWithConflicts(conflicts);
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
