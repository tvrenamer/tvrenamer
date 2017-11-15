package org.tvrenamer.model;

import org.tvrenamer.controller.ShowListingsListener;
import org.tvrenamer.controller.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * A Show represents a TV program, which has seasons, episodes, etc.  But this class
 * should rarely, if ever, be instantiated directly.  It is the superclass of "Series",
 * which is a Show that is recognized by the provider.  When a Show is instantiated
 * directly, it represents a Show that we want to support even though we cannot locate
 * it with the provider.  It is assigned an ID meant to not conflict with any of the
 * "real" Series we get from the provider.
 *
 * This class is used in testing, since it makes sense to not need to reach out to the
 * internet when not necessary.  It is really not used (directly) by the actual program,
 * but we may be able to make more use of it in the future.
 */
public class Show extends ShowOption {
    private static final Logger logger = Logger.getLogger(Show.class.getName());

    /**
     * Values to indicate lack of information about where an episode occurs
     * within a show's run.  Season 0 is a valid value, as it is used for
     * special episodes, and therefore, -1 is used as the sentinel for "not
     * known".  For episode number, episode 0 is not used, and therefore is
     * available for a sentinel.
     */
    public static final int NO_SEASON = -1;
    public static final int NO_EPISODE = 0;

    /*
     * More instance variables
     */
    final int idNum;
    private final String dirName;

    final Map<String, Episode> episodes;
    private final Map<Integer, Map<Integer, Episode>> seasons;
    final Queue<ShowListingsListener> registrations;

    /**
     * Create a Show object for a show that the provider knows about.  Initially
     * we just get the show's name and ID, but soon the
     * Show will be augmented with all of its episodes.
     *
     * @param idNum
     *     The ID of this show as an int
     * @param idString
     *     The ID of this show, from the provider, as a String
     * @param name
     *     The proper name of this show, from the provider.  May contain a distinguisher,
     *     such as a year.
     */
    Show(int idNum, String idString, String name) {
        super(idString, name);
        dirName = StringUtils.sanitiseTitle(name);

        this.idNum = idNum;

        episodes = new ConcurrentHashMap<>();
        seasons = new ConcurrentHashMap<>();
        registrations = new ConcurrentLinkedQueue<>();
    }

    /**
     * "Factory"-type static method to get an instance of a Show.  Looks
     * up the ID in a hash table, and returns the object if it's already
     * been created.  Otherwise, we create a new Show, put it into the
     * table, and return it.
     *
     * @param idString
     *     The ID of this show as a String
     * @param name
     *     The proper name of this show, from the provider.  May contain a distinguisher,
     *     such as a year.
     */
    public Show(String idString, String name) {
        this(fakeId(), idString, name);
    }

    private static int fakeShowId = 0;

    private static synchronized int fakeId() {
        return --fakeShowId;
    }

    /**
     * Get a string representing the Show's name, appropriate for use as a directory
     * name (or anywhere else within a filename).
     *
     * @return directory
     *            the name of the show, made appropriate for use within a file path
     */
    public String getDirName() {
        return dirName;
    }

    /**
     * Add an episode to a season's index of episodes, at the placement given.
     *
     * This method is agnostic of which ordering is being used.  It just asks the
     * Season to add the episode at the placement given.
     *
     * @param episode
     *           the episode to place at the index
     * @param placement
     *           the placement (season number and episode number) of the episode to add
     */
    private void addEpisodeToSeason(Episode episode, EpisodePlacement placement) {
        // Check to see if there's already an existing episode.  Only applies if we
        // have a valid season number and episode number.
        Map<Integer, Episode> season = seasons.get(placement.season);
        if (season == null) {
            season = new ConcurrentHashMap<>();
            seasons.put(placement.season, season);
        }
        Episode found = season.remove(placement.episode);
        if (found == null) {
            // This is the expected case; we should only be adding the episode to
            // the index a single time.
            season.put(placement.episode, episode);
        } else if (found == episode) {
            // Well, this is unfortunate; if it happens, investigate why.  But it's
            // fine.  We still have a unique object.
            season.put(placement.episode, episode);
        } else if (found.getTitle().equals(episode.getTitle())) {
            // This is less fine.  We've apparently created two objects to represent
            // the same data.  This should be fixed.
            logger.warning("replacing episode " + found.getEpisodeId()
                           + " for show " + name + ", season "
                           + placement.season + ", episode " + placement.episode + " (\""
                           + found.getTitle() + "\") with " + episode.getEpisodeId());
            season.put(placement.episode, episode);
        } else {
            // In this very unexpected case, we will not keep EITHER episode
            // in the table.  Remember that both will be in the unordered List
            // of episodes.  A future feature may be that when an episode is not
            // found in the seasons map, for whatever reason, to search through
            // the episode list.  This could be for "special" episodes, DVD extras,
            // etc.  But it could also be used for this case.
            logger.warning("two episodes found for show " + name + ", season "
                           + placement.season + ", episode " + placement.episode + ": \""
                           + found.getTitle() + "\" (" + found.getEpisodeId() + ") and \""
                           + episode.getTitle() + "\" (" + episode.getEpisodeId() + ")");
        }
    }

    /**
     * Build an index of this show's episodes, at the placement given.
     *
     * Placements are not definitive.  Production companies sometimes re-order them.  In
     * particular, they take liberties when releasing DVDs.  The TVDB tries to keep track
     * of the original, production order, as well as the DVD ordering (when applicable).
     * The truth is that some shows still have ambiguity beyond these options, but those
     * are the two basic options available.
     *
     * When adding an episode to a show's index of episodes, we prefer the DVD ordering but
     * fall back on the over-the-air ordering for episodes which don't have info in the
     * DVD ordering.
     *
     * Does not change the episode list at all; just organizes them into seasons
     * and episode numbers.
     *
     * Clears the season index before beginning, and iterates over all known episodes.
     * Indexes each episode by its preferred ordering if info exists, or by the non-
     * preferred ordering otherwise.
     */
    public synchronized void indexEpisodesBySeason() {
        seasons.clear();
        for (Episode episode : episodes.values()) {
            if (episode == null) {
                logger.severe("internal error creating episodes for " + name);
                return;
            }

            EpisodePlacement placement = episode.getDvdEpisodePlacement();

            // If we don't have good DVD information, fall back on over-the-air info.
            if (placement == null) {
                placement = episode.getAirEpisodePlacement();
            }

            // If we still don't have info, we can't index this episode
            if (placement == null) {
                // Note, in this case, the Episode will be created and will be added to the
                // list of episodes, but will not be added to the season/episode organization.
                logger.fine("episode \"" + episode.getTitle() + "\" of show " + name
                            + " lacks placement information");
                continue;
            }

            addEpisodeToSeason(episode, placement);
        }
    }

    /**
     * Log a message about each episode of this Show for which we found a problem.
     * Generally a "problem" means that we have found two (or more) episodes with
     * the same season and episode information.  Another problem could be that we
     * got a null episodeInfo, though there's very little information we can give,
     * in that case.
     *
     * @param problems
     *    a list of the EpisodeInfos that caused problems while we were adding them
     *    to the Show
     */
    private void logEpisodeProblems(List<EpisodeInfo> problems) {
        for (EpisodeInfo info : problems) {
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

    /**
     * Add a single episode to this Show's list.  Does not add the episode to the
     * (season number/episode number) index, nor does it generate any log messages
     * if anything goes wrong.  It is expected that the caller will handle any of
     * that, if desired.
     *
     * @param info
     *    information about the Episode, downloaded from the provider
     * @return true if the episode was added successfully
     *         false if anything went wrong, such as:
     *         <ul><li>if the episode couldn't be parsed</li>
     *             <li>if an  episode with the given ID was already present</li></ul>
     */
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

    /**
     * Creates Episodes, and adds them to this Show, for each of the given EpisodeInfos.
     * Relies on addOneEpisode() to create and verify the episode.  Collects failures
     * from addOneEpisode(), and logs messages about them.  Generally a "problem" means
     * that we have found two (or more) episodes with the same season and episode
     * information.  Another problem could be that we got a null episodeInfo, though
     * there's very little information we can give, in that case.
     *
     * After all the episodes are added, creates an index of the episodes by season and
     * episode number, according to the current numbering scheme.
     *
     * @param infos
     *    an array containing information about the episodes, downloaded from the provider
     */
    public void addEpisodeInfos(final EpisodeInfo[] infos) {
        List<EpisodeInfo> problems = new LinkedList<>();
        for (EpisodeInfo info : infos) {
            boolean added = addOneEpisode(info);
            if (!added) {
                problems.add(info);
            }
        }
        indexEpisodesBySeason();
        logEpisodeProblems(problems);
    }

    /**
     * Look up an episode for the given season and episode of this show.
     * Returns null if no such episode was found.
     *
     * Note that the value this returns is dependent on the numbering scheme
     * in use.  There is not always a definitive answer for which episode goes
     * in which spot.
     *
     * In the future, we may be able to expand it so that we use other
     * meta-information to try to determine which episode a given filename
     * refers to.  For now, we just go with season number and episode number.
     *
     * @param seasonNum
     *           the season of the episode to return
     * @param episodeNum
     *           the episode, within the given season, of the episode to return
     * @return the episode indexed at the given season and episode of this show.
     *    Null if no such episode was found.
     */
    public Episode getEpisode(int seasonNum, int episodeNum) {
        Map<Integer, Episode> season = seasons.get(seasonNum);
        if (season == null) {
            logger.fine("no season " + seasonNum + " found for show " + name);
            return null;
        }
        Episode episode = season.get(episodeNum);
        logger.fine("for season " + seasonNum + ", episode " + episodeNum
                    + ", found " + episode);

        return episode;
    }

    /**
     * Find out whether or not there are seasons associated with this show.
     * Generally this indicates that the show's listings have been downloaded,
     * the episodes have been organized into seasons, and the show is ready to go.
     *
     * @return a count of how many seasons we have for this Show
     */
    public boolean hasSeasons() {
        return (seasons.size() > 0);
    }

    /**
     * Find out whether or not there are episodes associated with this show.
     * Generally this indicates that the show's listings have been downloaded
     * and the show is ready to go.
     *
     * @return a count of how many episodes we have for this Show
     */
    public boolean hasEpisodes() {
        return (episodes.size() > 0);
    }

    /**
     * Get a count of how many episodes we have for this Show.
     *
     * @return a count of how many episodes we have for this Show
     */
    public int getEpisodeCount() {
        return episodes.size();
    }

    @Override
    public String toString() {
        return "Show [" + name + ", id=" + idString + ", "
            + episodes.size() + " episodes]";
    }
}
