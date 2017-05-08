package org.tvrenamer.controller;

import org.tvrenamer.model.Show;
import org.tvrenamer.model.TVRenamerIOException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ListingsLookup {

    private static Logger logger = Logger.getLogger(ListingsLookup.class.getName());

    private static class ListingsRegistrations {
        private final List<ShowListingsListener> listeners;

        public ListingsRegistrations() {
            this.listeners = new LinkedList<>();
        }

        public void addListener(ShowListingsListener listener) {
            this.listeners.add(listener);
        }

        public List<ShowListingsListener> getListeners() {
            return Collections.unmodifiableList(listeners);
        }
    }

    private static final Map<String, ListingsRegistrations> listenersMap = new ConcurrentHashMap<>(100);

    private static final ExecutorService THREAD_POOL
        = Executors.newCachedThreadPool(new ThreadFactory() {
                // We want the lookup thread to run at the minimum priority, to try to
                // keep the UI as responsive as possible.
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.setDaemon(true);
                    return t;
                }
            });

    private static void notifyListeners(Show show) {
        ListingsRegistrations registrations = listenersMap.get(show.getId());

        if (registrations != null) {
            for (ShowListingsListener listener : registrations.getListeners()) {
                if (show.hasSeasons()) {
                    listener.listingsDownloadComplete(show);
                } else {
                    listener.listingsDownloadFailed(show);
                }
            }
        }
    }

    private static void downloadListings(final Show show) {
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
                @Override
                public Boolean call() throws InterruptedException {
                    try {
                        TheTVDBProvider.getShowListing(show);
                        notifyListeners(show);
                        return true;
                    } catch (TVRenamerIOException e) {
                        notifyListeners(show);
                        return false;
                    } catch (Exception e) {
                        // Because this is running in a separate thread, an uncaught
                        // exception does not get caught by the main thread, and
                        // prevents this thread from dying.  Try to make sure that the
                        // thread dies, one way or another.
                        logger.log(Level.WARNING, "generic exception doing getListings for "
                                   + show, e);
                        notifyListeners(show);
                        return false;
                    }
                }
            };
        THREAD_POOL.submit(showFetcher);
    }

    /**
     * <p>
     * Download the show details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we already have the show listings (the Show has season info) then just  call the method on the listener</li>
     * <li>if we don't have the listings, but are in the process of processing them (exists in listenersMap) then
     * add the listener to the registration</li>
     * <li>if we don't have the listings and aren't processing, then create the
     registration, add the listener and kick off
     * the download</li>
     * </ul>
     *
     * @param show
     *            the Show object representing the show
     * @param listener
     *            the listener to notify or register
     */
    public static void getListings(final Show show, ShowListingsListener listener) {
        String key = show.getId();
        synchronized (listenersMap) {
            ListingsRegistrations registrations = listenersMap.get(key);
            if (registrations == null) {
                registrations = new ListingsRegistrations();
                listenersMap.put(key, registrations);
            }
            registrations.addListener(listener);
        }

        if (show.hasSeasons()) {
            notifyListeners(show);
            return;
        }

        downloadListings(show);
    }

    public static void cleanUp() {
        THREAD_POOL.shutdownNow();
    }

    public static void clear() {
        listenersMap.clear();
    }
}
