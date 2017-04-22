package org.tvrenamer.model;

import static org.tvrenamer.controller.util.StringUtils.makeQueryString;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TheTVDBProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ShowStore {

    private static Logger logger = Logger.getLogger(ShowStore.class.getName());

    private static final Map<String, Show> _shows = Collections.synchronizedMap(new HashMap<String, Show>());
    private static final Map<String, ShowRegistrations> _showRegistrations = new HashMap<>();

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static Show getShow(String showName) {
        Show s = _shows.get(makeQueryString(showName));
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
        String showKey = makeQueryString(showName);
        Show show = _shows.get(showKey);
        if (show != null) {
            listener.downloaded(show);
        } else {
            ShowRegistrations registrations = _showRegistrations.get(showKey);
            if (registrations != null) {
                registrations.addListener(listener);
            } else {
                registrations = new ShowRegistrations();
                registrations.addListener(listener);
                _showRegistrations.put(showKey, registrations);
                downloadShow(showName);
            }
        }
    }

    /**
     * Given a list of two or more options for which series we're dealing with,
     * choose the best one and return it.
     *
     * @param options the potential shows that match the string we searched for.
                Must not be null.
     * @param showName the part of the filename that is presumed to name the show
     * @return the series from the list which best matches the series information
     */
    private static Show selectShowOption(List<Show> options, String showName) {
        int nOptions = options.size();
        if (nOptions == 0) {
            logger.info("did not find any options for " + showName);
            return null;
        }
        if (nOptions == 1) {
            return options.get(0);
        }
        // logger.info("got " + nOptions + " options for " + showName);
        Show selected = null;
        for (int i=0; i<nOptions; i++) {
            Show s = options.get(i);
            String sName = s.getName();
            if (showName.equals(sName)) {
                if (selected == null) {
                    selected = s;
                } else {
                    // TODO: could check language?  other criteria?
                    logger.warning("multiple exact hits for " + showName
                                   + "; choosing first one");
                }
            }
        }
        // TODO: still might be better ways to choose if we don't have an exact match.
        // Levenshtein distance?
        if (selected == null) {
            selected = options.get(0);
        }
        return selected;
    }

    private static void downloadShow(final String showName) {
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
            @Override
            public Boolean call() throws InterruptedException {
                Show thisShow;
                try {
                    List<Show> options = TheTVDBProvider.getShowOptions(showName);
                    thisShow = selectShowOption(options, showName);
                } catch (TVRenamerIOException e) {
                    thisShow = new FailedShow("", showName, "", e);
                }

                logger.fine("Show listing for '" + thisShow.getName() + "' downloaded");
                String showKey = makeQueryString(showName);
                _shows.put(showKey, thisShow);
                notifyListeners(showKey, thisShow);
                return true;
            }
        };
        threadPool.submit(showFetcher);
    }

    private static void notifyListeners(String showKey, Show show) {
        ShowRegistrations registrations = _showRegistrations.get(showKey);

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
        private final List<ShowInformationListener> mListeners;

        public ShowRegistrations() {
            this.mListeners = new LinkedList<>();
        }

        public void addListener(ShowInformationListener listener) {
            this.mListeners.add(listener);
        }

        public List<ShowInformationListener> getListeners() {
            return Collections.unmodifiableList(mListeners);
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
     * Create a show and add it to the store, unless a show is already registered
     * by the show name.<br />
     *
     * Added this distinct method to enable unit testing.  Unlike the "real" method
     * (<code>getShow</code>), this does not spawn a thread, connect to the internet,
     * or use listeners in any way.  This is just accessing the data store.
     *
     * @param  showName
     *            the show name
     * @return show
     *            the {@link Show}
     */
    static Show getOrAddShow(String showName, String properName) {
        String showKey = makeQueryString(showName);
        Show show = _shows.get(showKey);
        if (show == null) {
            show = new Show(showKey, properName, "");
        }
        _shows.put(showKey, show);
        return show;
    }
}
