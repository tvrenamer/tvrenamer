package org.tvrenamer.controller;

import org.tvrenamer.model.Series;
import org.tvrenamer.model.TVRenamerIOException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class to help with looking up series listings from the provider.
 * This class does not manage the listeners, or deal with the episodes.
 * It is just for managing threads and communication with the provider.
 */
public class ListingsLookup {
    private static final Logger logger = Logger.getLogger(ListingsLookup.class.getName());

    /**
     * A pool of low-priority threads to execute the listings lookups.
     */
    private static final ExecutorService THREAD_POOL
        = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setPriority(Thread.MIN_PRIORITY);
            t.setDaemon(true);
            return t;
        });

    /**
     * Spawn a thread to ask the provider to look up the listings for the given Series.
     *
     * This is public so it can be called from the Series class.  No one else should
     * call it.  Other classes which are interested in series listings should call
     * addListener() on the Series itself.
     *
     * @param series
     *           the series to download listings for
     */
    public static void downloadListings(final Series series) {
        if (!series.beginDownload()) {
            logger.warning("should not call downloadListings; Series is already download[ing/ed].");
            return;
        }
        Callable<Boolean> listingsFetcher = () -> {
            try {
                TheTVDBSwaggerProvider.getSeriesListing(series);
                return true;
            } catch (TVRenamerIOException e) {
                series.listingsFailed(e);
                return false;
            } catch (Exception e) {
                // Because this is running in a separate thread, an uncaught
                // exception does not get caught by the main thread, and
                // prevents this thread from dying.  Try to make sure that the
                // thread dies, one way or another.
                logger.log(Level.WARNING, "generic exception doing getListings for "
                           + series, e);
                series.listingsFailed(e);
                return false;
            }
        };
        try {
            Future<Boolean> future = THREAD_POOL.submit(listingsFetcher);
            logger.fine("successfully submitted task " + future);
        } catch (RejectedExecutionException | NullPointerException e) {
            logger.log(Level.WARNING, "unable to submit listings download task ("
                       + series.getName() + ") for execution", e);
        }
    }

    /**
     * Kill any threads that might be running, so the program can shut down.
     *
     */
    public static void cleanUp() {
        THREAD_POOL.shutdownNow();
    }

    /**
     * This is a utility class; prevent it from being instantiated.
     *
     */
    private ListingsLookup() { }
}
