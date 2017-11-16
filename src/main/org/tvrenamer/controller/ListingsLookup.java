package org.tvrenamer.controller;

import org.tvrenamer.model.Show;
import org.tvrenamer.model.TVRenamerIOException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class to help with looking up show listings from the provider.
 * This class does not manage the listeners, or deal with the epsisodes.
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
     * Spawn a thread to ask the provider to look up the listings for the given Show.
     *
     * This is public so it can be called from the Show class.  No one else should
     * call it.  Other classes which are interested in show listings should call
     * addListener() on the Show itself.
     *
     * @param show
     *           the show to download listings for
     */
    public static void downloadListings(final Show show) {
        if (!show.beginDownload()) {
            logger.warning("should not call downloadListings; Show is already download[ing/ed].");
            return;
        }
        Callable<Boolean> listingsFetcher = () -> {
            try {
                TheTVDBProvider.getShowListing(show);
                return true;
            } catch (TVRenamerIOException e) {
                show.listingsFailed(e);
                return false;
            } catch (Exception e) {
                // Because this is running in a separate thread, an uncaught
                // exception does not get caught by the main thread, and
                // prevents this thread from dying.  Try to make sure that the
                // thread dies, one way or another.
                logger.log(Level.WARNING, "generic exception doing getListings for "
                           + show, e);
                show.listingsFailed(e);
                return false;
            }
        };
        Future<Boolean> future = THREAD_POOL.submit(listingsFetcher);
        show.addFuture(future);
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
