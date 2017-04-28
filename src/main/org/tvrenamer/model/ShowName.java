package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The ShowName class is an object that represents a string that is believed to represent
 * the name of a show.  Ultimately it will include a reference to the Show object.
 *
 * Some examples may be helpful.  Let's say we have the following files:
 *   "The Office S01E02 Work Experience.mp4"
 *   "The Office S05E07.mp4"
 *   "the.office.s06e20.mkv"
 *   "the.office.us.s08e11.avi"
 *
 * These would produce "filenameShow" values of "The Office ", "The Office ", "the.office.",
 * and "the.office.us", respectively.  The first two are the same, and therefore will map
 * to the same ShowName object.
 *
 * From the filenameShow, we create a query string, which normalizes the case and punctuation.
 * For the examples given, the query strings would be "the office", "the office", "the office",
 * and "the office us"; that is, the first *three* have the same value.  So even though there's
 * a separate ShowName object for the third file, it maps to the same QueryString.
 *
 * The QueryString will be sent to the provider, which will potentially give us options for
 * shows it knows about, that match the query string.  We map each query string to a Show.
 * Potentially, multiple query strings can map to the same show.  For example, the strings
 * "the office" and "office" might map to the same show.
 *
 * This example was chosen because there are, in fact, two distinct shows called "The Office".
 * There are different ways to distinguish them, such as "The Office (US)", "The Office (UK)",
 * "The Office (2005)", etc.  But the filenames may not have these differentiators.
 *
 * Currently, we pick one Show for a given ShowName, even though in this example, the two
 * files actually do refer to separate shows.  We (as humans) know the first one is the BBC
 * version, because of the episode title; we know the second one is the NBC version, because
 * it is Season 5, and the BBC version didn't run that long.  Currently, this program is
 * not able to make those inferences, but it would be good to add in the future.
 *
 */
public class ShowName implements Comparable<ShowName> {
    private static Logger logger = Logger.getLogger(ShowName.class.getName());

    /**
     * Inner class to hold a query string.  The query string is what we send to the provider
     * to try to resolve a show name.  We may re-use a single query string for multiple
     * show names.
     */
    private static class QueryString {
        final String queryString;
        private Show matchedShow = null;

        private static final Map<String, QueryString> QUERY_STRINGS = new ConcurrentHashMap<>();

        private QueryString(String queryString) {
            this.queryString = queryString;
        }

        /**
         * Set the mapping between this QueryString and a Show.  Checks to see if this has already
         * been mapped to a show, but if it has, we still accept the new mapping; we just warn
         * about it.
         *
         * @param show the Show to map this QueryString to
         * @return false if this QueryString had already been mapped to a show;
         *         true otherwise.
         */
        synchronized boolean setShow(Show show) {
            if (matchedShow == null) {
                matchedShow = show;
                return true;
            }
            if (matchedShow == show) {
                // same object; not just equals() but ==
                logger.info("re-setting show in QueryString " + queryString);
                return true;
            }
            logger.warning("changing show in QueryString " + queryString);
            matchedShow = show;
            return false;
        }

        /**
         * Get the mapping between this QueryString and a Show, if any has been established.
         *
         * @return show the Show to map this QueryString to
         */
        synchronized Show getMatchedShow() {
            return matchedShow;
        }

        /**
         * Factory-style method to obtain a QueryString.  If an object has already been created
         * for the query string we need for the found name, re-use it.
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

    private List<Show> showOptions;

    /**
     * Create a ShowName object for the given "foundName" String.  The "foundName"
     * is expected to be the exact String that was extracted by the TVRenamer parser
     * from the filename, that is believed to represent the show name.
     *
     * @param foundName
     *            the name of the show as it appears in the filename
     */
    public ShowName(String foundName) {
        this.foundName = foundName;
        sanitised = StringUtils.sanitiseTitle(foundName);
        queryString = QueryString.lookupQueryString(foundName);
    }

    /**
     * Find out if this ShowName has received its options from the provider yet.
     *
     * @return true if this ShowName has show options; false otherwise
     */
    public boolean hasShowOptions() {
        return (showOptions != null) && (showOptions.size() > 0);
    }

    /**
     * Add all possible Show options that could be mapped to this ShowName
     *
     * @param options
     *    list of Shows that could be mapped to this ShowName
     */
    public void setShowOptions(List<Show> options) {
        showOptions = options;
    }

    /**
     * Create a stand-in Show object in the case of failure from the provider.
     *
     * @return a Show representing this ShowName
     */
    public Show getFailedShow(TVRenamerIOException err) {
        Show standIn = new FailedShow(foundName, err);
        queryString.setShow(standIn);
        return standIn;
    }

    /**
     * Given a list of two or more options for which series we're dealing with,
     * choose the best one and return it.
     *
     * @return the series from the list which best matches the series information
     */
    public Show selectShowOption() {
        int nOptions = showOptions.size();
        if (nOptions == 0) {
            logger.info("did not find any options for " + foundName);
            return getFailedShow(null);
        }
        // logger.info("got " + nOptions + " options for " + foundName);
        Show selected = null;
        for (int i=0; i<nOptions; i++) {
            Show s = showOptions.get(i);
            String actualName = s.getName();
            if (foundName.equals(actualName)) {
                if (selected == null) {
                    selected = s;
                } else {
                    // TODO: could check language?  other criteria?
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

        queryString.setShow(selected);
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
     * Set the matched show for the QueryString associated with this ShowName.
     *
     * @param show the Show to map the QueryString to
     * @return false if the QueryString had already been mapped to a show;
     *         true otherwise.
     */
    public boolean setShow(Show matchedShow) {
        return queryString.setShow(matchedShow);
    }

    /**
     * Get the mapping between this ShowName and a Show, if any has been established.
     *
     * @return show the Show to map this ShowName to
     */
    synchronized Show getMatchedShow() {
        return queryString.getMatchedShow();
    }

    /**
     * Standard object method to represent this StringName as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "ShowName [" + foundName + "]";
    }

    /**
     * Standard compareTo method to make this class comparable
     *
     * @return this object's relative ordering compared to the given object
     */
    @Override
    public int compareTo(ShowName other) {
        return foundName.compareTo(other.foundName);
    }
}
