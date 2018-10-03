package org.tvrenamer.model;

import org.tvrenamer.controller.ListingsLookup;
import org.tvrenamer.controller.ShowListingsListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Represents a TV Show, with a name, url and list of seasons.
 */
public class Series extends Show {
    private static final Logger logger = Logger.getLogger(Series.class.getName());

    private enum DownloadStatus {
        NOT_STARTED,
        IN_PROGRESS,
        SUCCESS,
        FAILURE
    }

    /* When going from a filename to a Series object, there's a class in the way
     * which helps avoid duplication.  If we have two files like:
     *    "Lost.S06E05.mp4"
     *    "Lost.S06E06.mp4"
     * ... they will share a common ShowName object, and therefore be mapped to
     * the same Series object.  Which is good.
     *
     * But in a case like this one:
     *    "Real.Oneals.S01E01.avi"
     *    "The Real O'Neals.S01E02.avi"
     * ... they probably won't, because of the "The".  There will be two
     * separate ShowName objects created, one with "the" and one without.
     * But, that doesn't mean we have to create two Series objects.  Once
     * we query the provider and determine the ID of the series we're going
     * to map to, we can look in "KNOWN_SERIES" to see if a series has already
     * been created for that ID.  If it has, return that object, and don't
     * create a new one.
     *
     * Without using this hashmap, we might very well create two or more
     * instances of the same Series.  In fact, it happened all the time.
     * It doesn't cause anything to break, it just results in a lot of
     * unnecessary work.
     */
    private static final Map<String, Series> KNOWN_SERIES = new ConcurrentHashMap<>();

    /**
     * Looks up the ID in a hash table, and returns the object if it's already
     * been created.  Otherwise, returns null.
     *
     * @param idString
     *     The ID of this show, from the provider, as a String
     * @return a Series with the given ID
     */
    public static synchronized Series getExistingSeries(String idString) {
        return KNOWN_SERIES.get(idString);
    }

    /*
     * Instance data
     */
    private DownloadStatus listingsStatus = DownloadStatus.NOT_STARTED;

    // private constructor; assumes idNum > 0 and that no Series instance already exists with
    // the given idNum.  That's what the factory method is for.
    private Series(int idNum, String name) {
        super(idNum, String.valueOf(idNum), name);
    }

    /**
     * Create a Series object for a series that the provider knows about.  Initially
     * we just get the series's name and ID -- as a positive integer -- but soon the
     * Series will be augmented with all of its episodes.
     *
     * This class should not be used (directly) for any kind of "stand in".  It should
     * be given a valid integer ID that is known to the provider.  For a stand-in or
     * "local" show, instantiate the parent class (Show) or a FailedShow.
     *
     * @param id
     *     The ID of this series, from the provider, as an int
     * @param name
     *     The proper name of this series, from the provider.  May contain a distinguisher,
     *     such as a year.
     * @throws IllegalArgumentException if the id is not positive or if a Series with the
     *     given ID already exists
     * @return an instance of Series with the given ID and name, if here is no error.
     */
    public static Series createSeries(int id, String name) {
        if (id <= 0) {
            throw new IllegalArgumentException("Series ID num must be positive");
        }
        String idString = String.valueOf(id);
        Series mapped;
        synchronized (KNOWN_SERIES) {
            mapped = KNOWN_SERIES.get(idString);
        }
        if (mapped != null) {
            if (name.equals(mapped.name)) {
                logger.warning("already created series " + name);
            } else {
                logger.warning("ID for " + name + " clashes with existing Series " + id + ": "
                               + mapped);
                throw new IllegalArgumentException("Series ID num must be unique");
            }
        }

        mapped = new Series(id, name);
        synchronized (KNOWN_SERIES) {
            KNOWN_SERIES.put(idString, mapped);
        }
        return mapped;
    }

    /**
     * Called to indicate the caller is about to initiate downloading the
     * listings for this series.  If we find that the listings are already
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
     * Get this Series's ID, as an Integer.
     *
     * @return ID
     *            the ID of the series from the provider, as an Integer,
     *            or null if the given ID was not an int
     */
    public int getId() {
        return idNum;
    }

    /**
     * Registers a listener interested in this Series's listings.  If we
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
     * Called by ListingsLookup to let us know that it has finished trying to look up
     * the listings for this Series, but it did not succeed.
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
     * Standard object method to represent this Series as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "Series [" + name + ", id=" + idString + ", "
            + episodes.size() + " episodes]";
    }
}
