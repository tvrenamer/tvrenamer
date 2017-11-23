package org.tvrenamer.model;

import org.tvrenamer.controller.ListingsLookup;
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
 * Represents a TV Show, with a name, url and list of seasons.
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

    private enum DownloadStatus {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCESS,
        FAILURE
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
     * to map to, we can look in "KNOWN_SHOWS" to see if a show has already
     * been created for that ID.  If it has, return that object, and don't
     * create a new one.
     *
     * Without using this hashmap, we might very well create two or more
     * instances of the same Show.  In fact, it happened all the time.
     * It doesn't cause anything to break, it just results in a lot of
     * unnecessary work.
     *
     * Note we even put local Shows in here, too, even though it does not
     * serve the purpose of avoiding unnecessary work.  That's because
     * another usage of this map is that its values represent all the
     * Shows we have created, which can be useful information to have.
     */
    private static final Map<String, Show> KNOWN_SHOWS = new ConcurrentHashMap<>();

    /**
     * Looks up the ID in a hash table, and returns the object if it's already
     * been created.  Otherwise, returns null.
     *
     * @param idString
     *     The ID of this show, from the provider, as a String
     * @return a Show with the given ID
     */
    public static synchronized Show getExistingShow(String idString) {
        return KNOWN_SHOWS.get(idString);
    }

    /*
     * More instance variables
     */
    private final Integer idNum;
    private final String dirName;

    private final Map<String, Episode> episodes;
    private final Map<Integer, Map<Integer, Episode>> seasons;
    private final Queue<ShowListingsListener> registrations;

    private DownloadStatus listingsStatus = DownloadStatus.NOT_STARTED;

    /**
     * Create a Show object for a show that the provider knows about.  Initially
     * we just get the show's name and ID, but soon the
     * Show will be augmented with all of its episodes.
     *
     * This class should not be used (directly) for any kind of "stand in".
     *
     * @param idString
     *     The ID of this show, from the provider, as a String
     * @param name
     *     The proper name of this show, from the provider.  May contain a distinguisher,
     *     such as a year.
     */
    Show(String idString, String name) {
        super(idString, name);
        dirName = StringUtils.sanitiseTitle(name);

        Integer parsedId = null;
        try {
            parsedId = Integer.parseInt(idString);
        } catch (Exception e) {
            logger.fine("Show's ID " + idString + " could not be parsed as integer");
        }
        idNum = parsedId;

        episodes = new ConcurrentHashMap<>();
        seasons = new ConcurrentHashMap<>();
        registrations = new ConcurrentLinkedQueue<>();

        KNOWN_SHOWS.put(idString, this);
    }

    /**
     * "Factory"-type static method to get an instance of a Show.  Looks
     * up the ID in a hash table, and returns the object if it's already
     * been created.  Otherwise, we create a new Show, put it into the
     * table, and return it.
     *
     * @param id
     *     The ID of this show, from the provider, as a String
     * @param name
     *     The proper name of this show, from the provider.  May contain a distinguisher,
     *     such as a year.
     * @return a Show with the given ID
     */
    public static Show createShowInstance(String id, String name) {
        return new Show(id, name);
    }

    /**
     * If there is a show mapped to the given ID in KNOWN_SHOWS, remove it.
     *
     * @param id
     *     The ID of the show option, from the provider, as a String
     */
    public static void removeShowInstance(String id) {
        synchronized (KNOWN_SHOWS) {
            KNOWN_SHOWS.remove(id);
        }
    }

    /**
     * Called to indicate the caller is about to initiate downloading the
     * listings for this show.  If we find that the listings are already
     * in progress (or, already finished), we return false, and the caller
     * should abort.  Otherwise, we will return true, and we assume that
     * means the caller will immediately initiate a download right after that,
     * so we update the download status to "in progress".
     *
     * @return true if the listings need to be downloaded, false otherwise
     */
    public synchronized boolean beginDownload() {
        if (listingsStatus == DownloadStatus.NOT_STARTED) {
            listingsStatus = DownloadStatus.IN_PROGRESS;
            return true;
        }
        return false;
    }

    /**
     * Registers a listener interested in this Show's listings.  If we
     * already have the listings, and we can notify the new listener
     * immediately, and not have to iterate over the list of existing
     * listeners.
     *
     * @param listener
     *   the listener to add to the registrations
     */
    public synchronized void addListingsListener(ShowListingsListener listener) {
        if (listener == null) {
            logger.warning("cannot get listings without a listener");
            return;
        }
        registrations.add(listener);
        if (listingsStatus == DownloadStatus.NOT_STARTED) {
            ListingsLookup.downloadListings(this);
        } else if (listingsStatus == DownloadStatus.SUCCESS) {
            listener.listingsDownloadComplete();
        } else if (listingsStatus == DownloadStatus.FAILURE) {
            listener.listingsDownloadFailed(null);
        }
        // Else, listings are currently in progress, and listener will be
        // notified when they're complete.
    }

    /**
     * Get this Show's ID, as an Integer.
     *
     * @return ID
     *            the ID of the show from the provider, as an Integer,
     *            or null if the given ID was not an int
     */
    public Integer getId() {
        return idNum;
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
     * Clear this show's episodes.
     */
    public void clearEpisodeInfos() {
        episodes.clear();
    }

    /**
     * Add an episode to a season's index of episodes.
     *
     * This method is independent of which number scheme is being used.  That is,
     * it's up to the caller to pass in the correct arguments for the current
     * numbering scheme.
     *
     * @param seasonNum
     *           the season of the episode to return
     * @param episodeNum
     *           the episode, within the given season, of the episode to return
     * @param episode
     *           the episode to add at the given indices
     */
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
                           + found.getTitle() + "\" (" + found.getEpisodeId() + ") and \""
                           + episode.getTitle() + "\" (" + episode.getEpisodeId() + ")");
        }
    }

    /**
     * Build an index of this show's episodes, by season and episode number.
     *
     * Episode numbers are not definitive.  Production companies sometimes
     * re-order them.  In particular, they take liberties when releasing
     * DVDs.  The TVDB tries to keep track of the original, production order,
     * as well as the DVD ordering (when applicable).  The truth is that some
     * shows still have ambiguity beyond these options, but those are the two
     * basic options available.
     *
     * Does not change the episode list at all; just organizes them into seasons
     * and episode numbers.
     *
     * Clears the season index before beginning, and iterates over all known episodes.
     *
     */
    public synchronized void indexEpisodesBySeason() {
        seasons.clear();
        for (Episode episode : episodes.values()) {
            if (episode == null) {
                logger.severe("internal error creating episodes for " + name);
                return;
            }

            Integer seasonNum = episode.getDvdSeasonNumber();
            Integer episodeNum = episode.getDvdEpisodeNumber();

            // If we don't have good DVD information, fall back on over-the-air info.
            if ((seasonNum == null) || (episodeNum == null)) {
                seasonNum = episode.getSeasonNumber();
                episodeNum = episode.getEpisodeNumber();
            }

            // If we still don't have info, we can't index this episode
            if ((seasonNum == null) || (episodeNum == null)) {
                // Note, in this case, the Episode will be created and will be added to the
                // list of episodes, but will not be added to the season/episode organization.
                logger.fine("episode \"" + episode.getTitle() + "\" of show " + name
                            + " lacks season and episode information");
                continue;
            }

            addEpisodeToSeason(seasonNum, episodeNum, episode);
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
     * Return whether or not this is a "local" show.
     *
     * A "local" show is one that is not found in the provider.  It's generally a
     * sort of a substitute, and can also be thought of as "fake" in some way.
     * It is assigned an ID meant to not conflict with any of the "real" shows
     * we get from the provider.
     *
     * @return true the show is "local", false if it was found in the provider's db
     */
    public boolean isLocalShow() {
        return (this instanceof LocalShow);
    }

    /**
     * Return whether or not this is a "failed" show.
     *
     * @return true the show is "failed", false otherwise
     */
    public boolean isFailedShow() {
        return (this instanceof FailedShow);
    }

    /**
     * Called by ListingsLookup to let us know that it has finished trying to look up
     * the listings for this Show, but it did not succeed.
     *
     * @param err
     *     an exception that was thrown while trying to look up the listings.
     *     May be null.
     */
    public synchronized void listingsFailed(Exception err) {
        listingsStatus = DownloadStatus.FAILURE;
        for (ShowListingsListener listener : registrations) {
            listener.listingsDownloadFailed(err);
        }
    }

    /**
     * Called after we've added all the episodes to the hashmap.  At that point,
     * we have the listings, and we can notify the listeners.
     *
     */
    public synchronized void listingsSucceeded() {
        listingsStatus = DownloadStatus.SUCCESS;
        registrations.forEach(ShowListingsListener::listingsDownloadComplete);
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

    /**
     * Standard object method to represent this Show as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "Show [" + name + ", id=" + idString + ", "
            + episodes.size() + " episodes]";
    }
}
