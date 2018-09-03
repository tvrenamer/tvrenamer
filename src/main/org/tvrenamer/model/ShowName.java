package org.tvrenamer.model;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.util.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * The ShowName class is an object that represents a string that we want to send to
 * the provider to find a show..  Ultimately it will include a reference to the
 * Show object.<p>
 *
 * Some examples may be helpful.  Let's say we have the following files:<ul>
 *   <li>"The Office S01E02 Work Experience.mp4"</li>
 *   <li>"The Office S05E07.mp4"</li>
 *   <li>"the.office.s06e20.mkv"</li>
 *   <li>"the.office.us.s08e11.avi"</li></ul><p>
 *
 * The FilenameParser class breaks up filenames into "filenameShow", "filenameSeason", etc.
 * It would produce filenameShow values of "The Office", "The Office", "the.office", and
 * "the.office.us", respectively.  Note, the first two are identical, but the third is a
 * different String value.<p>
 *
 * The filenameShow is passed to a static method of this class.  In that method, we create a
 * query string, which normalizes the case and punctuation.  For the examples given, the
 * query strings would be "the office", "the office", "the office", and "the office us";
 * that is, the first *three* have the same value.  So even though the third filenameShow
 * differed, it maps to the same ShowName as the first two.<p>
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
     * A mapping from Strings to ShowName objects.  This is potentially a
     * many-to-one relationship.
     */
    private static final Map<String, ShowName> SHOW_NAMES = new ConcurrentHashMap<>();

    /**
     * Get the ShowName object for the given String.  If one was already created,
     * it is returned, and if not, one will be created, stored, and returned.
     *
     * Note, the functionality here is identical to {@link #lookupShowName}.  The only
     * implementation difference is the error message.  But callers should know which
     * one they want.
     *
     * @param filenameShow
     *            the name of the show as it appears in the filename
     * @return the ShowName object for that filenameShow
     */
    public static ShowName mapShowName(String filenameShow) {
        final String queryString = StringUtils.makeQueryString(filenameShow);
        ShowName showName = SHOW_NAMES.get(queryString);
        if (showName == null) {
            showName = new ShowName(queryString, filenameShow);
            SHOW_NAMES.put(queryString, showName);
        }
        return showName;
    }

    /**
     * Get the ShowName object for the given String, under the assumption that such
     * a mapping already exists.  If no mapping is found, one will be created, stored,
     * and returned, but an error message will also be generated.
     *
     * Note, the functionality here is identical to {@link #mapShowName}.  The only
     * implementation difference is the error message.  But callers should know which
     * one they want.
     *
     * @param filenameShow
     *            the name of the show as it appears in the filename
     * @return the ShowName object for that filenameShow
     */
    public static ShowName lookupShowName(String filenameShow) {
        String queryString = StringUtils.makeQueryString(filenameShow);
        ShowName showName = SHOW_NAMES.get(queryString);
        if (showName == null) {
            showName = new ShowName(queryString, filenameShow);
            SHOW_NAMES.put(queryString, showName);
            logger.severe("could not get show name for " + filenameShow
                          + "(" + queryString + "), so created one instead");
        }
        return showName;
    }

    /*
     * Instance variables
     */
    private final String exampleFilename;
    private final String queryString;
    private final List<ShowInformationListener> listeners = new LinkedList<>();
    private final List<ShowOption> showOptions;
    private ShowOption matchedShow = null;

    /**
     * Add a listener for this ShowName's query string.
     *
     * @param listener
     *            the listener registering interest
     */
    void addShowInformationListener(final ShowInformationListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Determine if this ShowName needs to be queried.
     *
     * If the answer is "yes", we add a listener and query immediately,
     * in a synchronized block.  Therefore, that becomes how we determine
     * the answer: if this ShowName already has a listener, that means
     * its download is already underway.
     *
     * @return false if this ShowName's query string already has a listener;
     *     true if not
     */
    boolean needsQuery() {
        synchronized (listeners) {
            return (listeners.size() == 0);
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
        synchronized (listeners) {
            listeners.forEach(l -> l.downloadSucceeded(show));
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
        synchronized (listeners) {
            listeners.forEach(l -> l.downloadFailed(show));
        }
    }

    /**
     * Notify registered interested parties that the provider is unusable
     * due to a discontinued API.
     *
     */
    public void apiDiscontinued() {
        synchronized (listeners) {
            listeners.forEach(ShowInformationListener::apiHasBeenDeprecated);
        }
    }

    /**
     * Create a ShowName object for the given query string and example filename.
     * The example filename is expected to be an exact String that was extracted
     * by the FilenameParser from the filename, that is believed to represent the
     * show name.
     *
     * @param queryString
     *            the string we want to use as the value to query for a show
     *            that matches this ShowName
     * @param exampleFilename
     *            the name of the show as it appears in a filename
     */
    private ShowName(final String queryString, final String exampleFilename) {
        this.queryString = queryString;
        this.exampleFilename = exampleFilename;
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
     * Set the mapping between this ShowName and a Show.  Checks to see if this has already
     * been mapped to a show, but if it has, we still accept the new mapping; we just warn
     * about it.
     *
     * @param showOption the ShowOption to map this ShowName to
     */
    private synchronized void setShowOption(final ShowOption showOption) {
        if (matchedShow == null) {
            matchedShow = showOption;
            return;
        }
        if (matchedShow == showOption) {
            // same object; not just equals() but ==
            logger.info("re-setting show in ShowName " + queryString);
            return;
        }
        logger.warning("changing show in ShowName " + queryString);
        matchedShow = showOption;
    }

    /**
     * Create a stand-in Show object in the case of failure from the provider.
     *
     * @param err any TVRenamerIOException that occurred trying to look up the show.
     *            May be null.
     * @return a Show representing this ShowName
     */
    public FailedShow getFailedShow(TVRenamerIOException err) {
        FailedShow standIn = new FailedShow(exampleFilename, err);
        setShowOption(standIn);
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
            logger.info("did not find any options for " + exampleFilename);
            return getFailedShow(null);
        }
        // logger.info("got " + nOptions + " options for " + exampleFilename);
        ShowOption selected = null;
        for (ShowOption s : showOptions) {
            String actualName = s.getName();
            // Possibly instead of ignore case, we should make the exampleFilename be
            // properly capitalized, and then we can do an exact comparison.
            if (exampleFilename.equalsIgnoreCase(actualName)) {
                if (selected == null) {
                    selected = s;
                } else {
                    // TODO: could check language?  other criteria?  Case sensitive?
                    logger.warning("multiple exact hits for " + exampleFilename
                                   + "; choosing first one");
                }
            }
        }
        // TODO: still might be better ways to choose if we don't have an exact match.
        // Levenshtein distance?
        if (selected == null) {
            selected = showOptions.get(0);
        }

        setShowOption(selected);
        return selected;
    }

    /**
     * Get this ShowName's "example filename".<p>
     *
     * The "example filename" is an exact substring of the filename that caused
     * this ShowName to be created; specifically, it's the part of the filename
     * that we believe represents the show.  This ShowName may be used for other,
     * slightly different filenames, as well.  For example, the ShowName is
     * independent of separators, so "The Office" and "The.Office" would share a
     * ShowName.  The exampleFilename would be whichever one randomly was
     * processed first.  As long as it refers to an actual part of SOME filename,
     * it's fine.
     *
     * @return the example filename
     */
    public String getExampleFilename() {
        return exampleFilename;
    }

    /**
     * Get this ShowName's "query string"
     *
     * @return the query string
     *            the string we want to use as the value to query for a show
     *            that matches this ShowName
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Get the mapping between this ShowName and a Show, if any has been established.
     *
     * @return a Show, if this ShowName is matched to one.  Null if not.
     */
    public synchronized ShowOption getMatchedShow() {
        return matchedShow;
    }

    /**
     * Standard object method to represent this ShowName as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        return "ShowName [" + queryString + "]";
    }
}
