package org.tvrenamer.model;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The ShowName class is an object that represents a string that is believed to represent
 * the name of a show.  Ultimately it will include a reference to the Show object.<p>
 *
 * Some examples may be helpful.  Let's say we have the following files:<ul>
 *   <li>"The Office S01E02 Work Experience.mp4"</li>
 *   <li>"The Office S05E07.mp4"</li>
 *   <li>"the.office.s06e20.mkv"</li>
 *   <li>"the.office.us.s08e11.avi"</li></ul><p>
 *
 * These would produce "filenameShow" values of "The Office ", "The Office ", "the.office.",
 * and "the.office.us", respectively.  The first two are the same, and therefore will map
 * to the same ShowName object.<p>
 *
 * From the filenameShow, we create a query string, which normalizes the case and punctuation.
 * For the examples given, the query strings would be "the office", "the office", "the office",
 * and "the office us"; that is, the first *three* have the same value.  So even though there's
 * a separate ShowName object for the third file, it maps to the same QueryString.<p>
 *
 * The QueryString will be sent to the provider, which will potentially give us options for
 * shows it knows about, that match the query string.  We map each query string to a Show.
 * Potentially, multiple query strings can map to the same show.  For example, the strings
 * "the office" and "office" might map to the same show.<p>
 *
 * This example was chosen because there are, in fact, two distinct shows called "The Office".
 * There are different ways to distinguish them, such as "The Office (US)", "The Office (UK)",
 * "The Office (2005)", etc.  But the filenames may not have these differentiators.<p>
 *
 * Currently, we pick one Show for a given ShowName, even though in this example, the two
 * files actually do refer to separate shows.  We (as humans) know the first one is the BBC
 * version, because of the episode title; we know the second one is the NBC version, because
 * it is Season 5, and the BBC version didn't run that long.  Currently, this program is
 * not able to make those inferences, but it would be good to add in the future.
 */
public class ShowName {
    private static final Logger logger = Logger.getLogger(ShowName.class.getName());

    /**
     * Inner class to hold a query string.  The query string is what we send to the provider
     * to try to resolve a show name.  We may re-use a single query string for multiple
     * show names.
     */
    private static class QueryString {
        final String queryString;
        private ShowOption matchedShow = null;
        private final List<ShowInformationListener> listeners = new LinkedList<>();

        private static final Map<String, QueryString> QUERY_STRINGS = new ConcurrentHashMap<>();

        private QueryString(String queryString) {
            this.queryString = queryString;
        }

        /**
         * Set the mapping between this QueryString and a Show.  Checks to see if this has already
         * been mapped to a show, but if it has, we still accept the new mapping; we just warn
         * about it.
         *
         * @param showOption the ShowOption to map this QueryString to
         */
        synchronized void setShowOption(ShowOption showOption) {
            if (matchedShow == null) {
                matchedShow = showOption;
                return;
            }
            if (matchedShow == showOption) {
                // same object; not just equals() but ==
                logger.info("re-setting show in QueryString " + queryString);
                return;
            }
            logger.warning("changing show in QueryString " + queryString);
            matchedShow = showOption;
        }

        // see ShowName.addListener for documentation
        private void addListener(ShowInformationListener listener) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }

        // see ShowName.hasListeners for documentation
        private boolean hasListeners() {
            synchronized (listeners) {
                return (listeners.size() > 0);
            }
        }

        // see ShowName.nameResolved for documentation
        private void nameResolved(Show show) {
            synchronized (listeners) {
                for (ShowInformationListener informationListener : listeners) {
                    informationListener.downloadSucceeded(show);
                }
            }
        }

        // see ShowName.nameNotFound for documentation
        private void nameNotFound(FailedShow failedShow) {
            synchronized (listeners) {
                for (ShowInformationListener informationListener : listeners) {
                    informationListener.downloadFailed(failedShow);
                }
            }
        }

        // see ShowName.apiDiscontinued for documentation
        private void apiDiscontinued() {
            synchronized (listeners) {
                listeners.forEach(ShowInformationListener::apiHasBeenDeprecated);
            }
        }

        /**
         * Get the mapping between this QueryString and a ShowOption, if any has been established.
         *
         * @return show the ShowOption to map this QueryString to
         */
        synchronized ShowOption getMatchedShow() {
            return matchedShow;
        }

        /**
         * Factory-style method to obtain a QueryString.  If an object has already been created
         * for the query string we need for the found name, re-use it.
         *
         * @param foundName
         *    the portion of the filename that is believed to represent the show's name
         * @return a QueryString object for looking up the foundName
         */
        static QueryString lookupQueryString(String foundName) {
            String queryString = StringUtils.makeQueryString(foundName);
            QueryString queryObj = QUERY_STRINGS.get(queryString);
            if (queryObj == null) {
                queryObj = new QueryString(queryString);
                QUERY_STRINGS.put(queryString, queryObj);
            }
            return queryObj;
        }
    }

    /**
     * A mapping from Strings to ShowName objects.  This is potentially a
     * many-to-one relationship.
     */
    private static final Map<String, ShowName> SHOW_NAMES = new ConcurrentHashMap<>();

    /**
     * Get the ShowName object for the given String.  If one was already created,
     * it is returned, and if not, one will be created, stored, and returned.
     *
     * @param filenameShow
     *            the name of the show as it appears in the filename
     * @return the ShowName object for that filenameShow
     */
    public static ShowName lookupShowName(String filenameShow) {
        ShowName showName = SHOW_NAMES.get(filenameShow);
        if (showName == null) {
            showName = new ShowName(filenameShow);
            SHOW_NAMES.put(filenameShow, showName);
        }
        return showName;
    }

    /*
     * Instance variables
     */
    private final String foundName;
    private final String sanitised;
    private final QueryString queryString;

    private final List<ShowOption> showOptions;

    /*
     * QueryString methods -- these four methods are the public interface to the
     * functionality, but they are just pass-throughs to the real implementations
     * kept inside the QueryString inner class.
     */

    /**
     * Add a listener for this ShowName's query string.
     *
     * @param listener
     *            the listener registering interest
     */
    public void addListener(ShowInformationListener listener) {
        synchronized (queryString) {
            queryString.addListener(listener);
        }
    }

    /**
     * Determine if this ShowName's query string has any listeners yet
     *
     * @return true if this ShowName's query string already has a listener;
     *     false if not
     */
    public boolean hasListeners() {
        synchronized (queryString) {
            return queryString.hasListeners();
        }
    }

    /**
     * Notify registered interested parties that the provider has found a show,
     * and we've created a Show object to represent it.
     *
     * @param show
     *    the Show object representing the TV show we've mapped the string to.
     */
    public void nameResolved(Show show) {
        synchronized (queryString) {
            queryString.nameResolved(show);
        }
    }

    /**
     * Notify registered interested parties that the provider did not give us a
     * viable option, and provide a stand-in object.
     *
     * @param show
     *    the FailedShow object representing the string we searched for.
     */
    public void nameNotFound(FailedShow show) {
        synchronized (queryString) {
            queryString.nameNotFound(show);
        }
    }

    /**
     * Notify registered interested parties that the provider is unusable
     * due to a discontinued API.
     *
     */
    public void apiDiscontinued() {
        synchronized (queryString) {
            queryString.apiDiscontinued();
        }
    }

    /**
     * Create a ShowName object for the given "foundName" String.  The "foundName"
     * is expected to be the exact String that was extracted by the FilenameParser
     * from the filename, that is believed to represent the show name.
     *
     * @param foundName
     *            the name of the show as it appears in the filename
     */
    private ShowName(String foundName) {
        this.foundName = foundName;
        sanitised = StringUtils.sanitiseTitle(foundName);
        queryString = QueryString.lookupQueryString(foundName);

        showOptions = new LinkedList<>();
    }

    /**
     * Find out if this ShowName has received its options from the provider yet.
     *
     * @return true if this ShowName has show options; false otherwise
     */
    public boolean hasShowOptions() {
        return (showOptions.size() > 0);
    }

    /**
     * Add a possible Show option that could be mapped to this ShowName
     *
     * @param tvdbId
     *    the show's id in the TVDB database
     * @param seriesName
     *    the "official" show name
     */
    public void addShowOption(final String tvdbId, final String seriesName) {
        ShowOption option = ShowOption.getShowOption(tvdbId, seriesName);
        showOptions.add(option);
    }

    /**
     * Add a possible Show option that could be mapped to this ShowName
     *
     * @param seriesInfo
     *    the show's info
     */
    public void addShowOption(final SeriesInfo seriesInfo) {
        addShowOption(String.valueOf(seriesInfo.id), seriesInfo.seriesName);
    }

    /**
     * Create a stand-in Show object in the case of failure from the provider.
     *
     * @param err any TVRenamerIOException that occurred trying to look up the show.
     *            May be null.
     * @return a Show representing this ShowName
     */
    public FailedShow getFailedShow(TVRenamerIOException err) {
        FailedShow standIn = new FailedShow(foundName, err);
        queryString.setShowOption(standIn);
        return standIn;
    }

    /**
     * Given a list of two or more options for which series we're dealing with,
     * choose the best one and return it.
     *
     * @return the series from the list which best matches the series information
     */
    public ShowOption selectShowOption() {
        int nOptions = showOptions.size();
        if (nOptions == 0) {
            logger.info("did not find any options for " + foundName);
            return getFailedShow(null);
        }
        // logger.info("got " + nOptions + " options for " + foundName);
        ShowOption selected = null;
        for (ShowOption s : showOptions) {
            String actualName = s.getName();
            // Possibly instead of ignore case, we should make the foundName be
            // properly capitalized, and then we can do an exact comparison.
            if (foundName.equalsIgnoreCase(actualName)) {
                if (selected == null) {
                    selected = s;
                } else {
                    // TODO: could check language?  other criteria?  Case sensitive?
                    logger.warning("multiple exact hits for " + foundName
                                   + "; choosing first one");
                }
            }
        }
        // TODO: still might be better ways to choose if we don't have an exact match.
        // Levenshtein distance?
        if (selected == null) {
            selected = showOptions.get(0);
        }

        queryString.setShowOption(selected);
        return selected;
    }

    /**
     * Get this ShowName's "foundName" attribute.
     *
     * @return foundName
     *            the name of the show as it appears in the filename
     */
    public String getFoundName() {
        return foundName;
    }

    /**
     * Get this ShowName's "sanitised" attribute.
     *
     * @return sanitised
     *            the name of the show after being run through the
     *            "sanitising" filter.  The value should be appropriate
     *            for any supported filesystem (free from illegal characters)
     */
    @SuppressWarnings("unused")
    public String getSanitised() {
        return sanitised;
    }

    /**
     * Get this ShowName's "query string"
     *
     * @return the query string
     *            the string we want to use as the value to query for a show
     *            that matches this ShowName
     */
    public String getQueryString() {
        return queryString.queryString;
    }

    /**
     * Get the mapping between this ShowName and a Show, if any has been established.
     *
     * @return a Show, if this ShowName is matched to one.  Null if not.
     */
    public synchronized ShowOption getMatchedShow() {
        return queryString.getMatchedShow();
    }

    /**
     * Standard object method to represent this ShowName as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "ShowName [" + foundName + "]";
    }
}
