package org.tvrenamer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TheTVDBProvider;

public class ShowStore {

    private static Logger logger = Logger.getLogger(ShowStore.class.getName());

    private static final Map<String, Show> _shows = Collections.synchronizedMap(new HashMap<String, Show>());
    private static final Map<String, ShowRegistrations> _showRegistrations = new HashMap<>();

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static Show getShow(String showName) {
        Show s = _shows.get(showName.toLowerCase());
        if (s == null) {
            String message = "Show not found for show name: '" + showName + "'";
            logger.warning(message);
            throw new ShowNotFoundException(message);
        }

        return s;
    }

    /**
     * <p>
     * Download the show details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we have already downloaded the show (exists in _shows) then just call the method on the listener</li>
     * <li>if we don't have the show, but are in the process of downloading the show (exists in _showRegistrations) then
     * add the listener to the registration</li>
     * <li>if we don't have the show and aren't downloading, then create the registration, add the listener and kick off
     * the download</li>
     * </ul>
     *
     * @param showName
     *            the name of the show
     * @param listener
     *            the listener to notify or register
     */
    public static void getShow(String showName, ShowInformationListener listener) {
        Show show = _shows.get(showName.toLowerCase());
        if (show != null) {
            listener.downloaded(show);
        } else {
            ShowRegistrations registrations = _showRegistrations.get(showName.toLowerCase());
            if (registrations != null) {
                registrations.addListener(listener);
            } else {
                registrations = new ShowRegistrations();
                registrations.addListener(listener);
                _showRegistrations.put(showName.toLowerCase(), registrations);
                downloadShow(showName);
            }
        }
    }

    private static void downloadShow(final String showName) {
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
            @Override
            public Boolean call() throws InterruptedException {
                Show thisShow;
                try {
                    ArrayList<Show> options = TheTVDBProvider.getShowOptions(showName);
                    thisShow = options.get(0);

                    TheTVDBProvider.getShowListing(thisShow);
                } catch (TVRenamerIOException e) {
                    thisShow = new FailedShow("", showName, "", e);
                }

                addShow(showName, thisShow);

                return true;
            }
        };
        threadPool.submit(showFetcher);
    }

    private static void notifyListeners(String showName, Show show) {
        ShowRegistrations registrations = _showRegistrations.get(showName.toLowerCase());

        if (registrations != null) {
            for (ShowInformationListener informationListener : registrations.getListeners()) {
                if (show instanceof FailedShow) {
                    informationListener.downloadFailed(show);
                } else {
                    informationListener.downloaded(show);
                }
            }
        }
    }

    private static class ShowRegistrations {
        private final List<ShowInformationListener> _listeners;

        public ShowRegistrations() {
            this._listeners = new LinkedList<>();
        }

        public void addListener(ShowInformationListener listener) {
            this._listeners.add(listener);
        }

        public List<ShowInformationListener> getListeners() {
            return Collections.unmodifiableList(_listeners);
        }
    }

    public static void cleanUp() {
        threadPool.shutdownNow();
    }

    public static void clear() {
        _shows.clear();
        _showRegistrations.clear();
    }

    /**
     * Add a show to the store, registered by the show name.<br />
     * Added this distinct method to enable unit testing
     *
     * @param showName
     *            the show name
     * @param show
     *            the {@link Show}
     */
    static void addShow(String showName, Show show) {
        logger.info("Show listing for '" + show.getName() + "' downloaded");
        _shows.put(showName.toLowerCase(), show);
        notifyListeners(showName, show);
    }
}
