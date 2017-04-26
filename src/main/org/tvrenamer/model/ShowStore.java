/**
 * ShowStore -- maps strings to Show objects.
 *
 * Note that, just for a single file, we may have up to five versions of the show's "name".
 * Let's look at an example.  Say we have a file named "Cosmos, A Space Time Odyssey S01E02.mp4".
 * The first thing we do is try to extract the show name from the filename.  If we do it right,
 * we'll get "Cosmos, A Space Time Odyssey".  That string is stored as the "filenameShow" of
 * the FileEpisode.
 *
 * Next, we'll want to query for that string.  But first we try to eliminate characters that
 * are either problematic, because they might serve as meta-characters in various contexts,
 * or simply irrelevant.  We consider show titles to essentially be case-insensitive, and
 * we don't think punctuation matters, at this point.  So we normalize the string.  Since
 * this is the text we're going to send to the provider to query for which actual show it
 * might match, I sometimes call this the "query string".  In this case, it would be
 * "cosmos a space time odyssey".
 *
 * Then, from the provider, we get back the actual show name: "Cosmos: A Spacetime Odyssey".
 *
 * But this is a bit of a problem, because Windows does not allow the colon character to
 * appear in filenames.  So we "sanitise" the title to "Cosmos - A Spacetime Odyssey".
 * That's four versions of the same show name.
 *
 * The fifth?  We allow users to set a preference to use dots instead of spaces in the
 * filenames, which would turn this into "Cosmos-A.Spacetime.Odyssey".
 *
 * (Note that I did say, "up to" five versions.  In the case of a show like "Futurama",
 * we'd likely only deal with two versions, upper-case and lower-case.)
 *
 * Once again, in table form:
 *  (1) filename show     | "Cosmos, A Space Time Odyssey"
 *  (2) query string      | "cosmos a space time odyssey"
 *  (3) actual show name  | "Cosmos: A Spacetime Odyssey"
 *  (4) sanitised name    | "Cosmos - A Spacetime Odyssey"
 *  (5) output name       | "Cosmos-A.Spacetime.Odyssey"
 *
 * Most of these transitions are simple string transformations, provided by StringUtils.java:
 *  (1) -> (2) makeQueryString
 *  (3) -> (4) sanitiseTitle
 *  (4) -> (5) makeDotTitle
 *
 * This file is how we get from (2) -> (3).  It maps query strings to Show objects, and the
 * Show objects obviously contain the actual show name.  So we have:
 *
 *  (1) -> (2)  makeQueryString
 *  (2) -> (3a) ShowStore.getShow
 *  (3a) -> (3) Show.getName
 *  (3) -> (4)  sanitiseTitle
 *  (4) -> (5)  makeDotTitle
 *
 * Note that makeQueryString should be idempotent.  If you already have a query string, and
 * you call makeQueryString on it, you should get back the identical string.
 *
 * One other small note, the "actual show name" is not necessarily the true, actual actual
 * show name.  In fact, the strings we consider as "actual show name" are expected to be
 * unique (not sure if I can say "guaranteed", that's kind of out of our hands), whereas
 * actual show names are not.  There was never a show called "Archer (2009)"; the show that
 * refers to was just called "Archer".  But The TVDB adds the date because there had been
 * a previous show called "Archer".
 *
 * This is true despite the fact that Shows also have a show ID, which is presumably even more
 * guaranteed to be unique.
 *
 * Given the assumption about the uniqueness of the "actual show name", we hope to have:
 *  (1) -> (2)  many to one
 *  (2) -> (3a) many to one
 *  (3a) -> (3) one to one
 *  (3) -> (4)  one to one
 *  (4) -> (5)  one to one
 *
 * I still must say "hope to have", because this does all depend on the idea that a show
 * is never identified by punctuation or case.  That is, if we had DIFFERENT shows, one
 * called "Cosmos: A Spacetime Odyssey" and the other called "Cosmos - A Spacetime Odyssey",
 * or the other called "Cosmos: a spacetime odyssey", we would not be able to accurately
 * tell them apart.  But it's a safe assumption that won't happen.
 *
 * On the other hand, we likely DO have issues involving the non-uniqueness of a title like
 * "Archer" or "The Office".  The fact that The TVDB assigns unique names to these series
 * does not necessarily help us much in doing the (2) -> (3a) mapping.
 *
 * What we might want to do in the future is make it potentially a many-to-many relation,
 * and say that calling getShow() does not necessarily pin down the exact series the file
 * refers to.  We might be able to figure it out later, based on additional information.
 * For example, if we're looking at "The Office, Season 8", we know it has to be the US
 * version, because the UK version didn't do that many seasons.  Or, if the actual episode
 * name is already embedded in the filename, we could try to match that up with the information
 * we get about episode listings.
 *
 * Perhaps the best option would be to have something in the UI to notify the user of the
 * ambiguity, make our best guess, and let the user correct it, if they like. But we don't
 * have that functionality, now.
 *
 * So, anyway, this class.  :)  Again, this is the (2) -> (3a) step, mapping query strings
 * to Show objects.  The real work here is when we take a query string, pass it to the
 * provider to get back a list of options, choose the best option, and return it to the
 * listener via callback.  But we do, of course, also store the mapping in a hash map, so
 * if a second file comes in with the same query string, we don't go look it up again,
 * but simply return the same answer we gave the first time.
 *
 */

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

    public static Show getShow(String filenameShow) {
        String queryString = makeQueryString(filenameShow);
        Show s = _shows.get(queryString);
        if (s == null) {
            String message = "Show not found for show name: '" + filenameShow + "'";
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
     * <li>if we have already downloaded the show (exists in _shows) then just notify the listener</li>
     * <li>if we don't have the show, but are in the process of downloading the show
     *     (exists in _showRegistrations) then add the listener to the registration</li>
     * <li>if we don't have the show and aren't downloading, then create the registration,
     *     add the listener and kick off the download</li>
     * </ul>
     *
     * @param filenameShow
     *            the name of the show as it appears in the filename
     * @param listener
     *            the listener to notify or register
     */
    public static void getShow(String filenameShow, ShowInformationListener listener) {
        String queryString = makeQueryString(filenameShow);
        Show show = _shows.get(queryString);
        if (show != null) {
            listener.downloaded(show);
        } else {
            ShowRegistrations registrations = _showRegistrations.get(queryString);
            if (registrations != null) {
                registrations.addListener(listener);
            } else {
                registrations = new ShowRegistrations();
                registrations.addListener(listener);
                _showRegistrations.put(queryString, registrations);
                downloadShow(filenameShow, queryString);
            }
        }
    }

    /**
     * Given a list of two or more options for which series we're dealing with,
     * choose the best one and return it.
     *
     * @param options the potential shows that match the string we searched for.
                Must not be null.
     * @param filenameShow the part of the filename that is presumed to name the show
     * @return the series from the list which best matches the series information
     */
    private static Show selectShowOption(List<Show> options, String filenameShow) {
        int nOptions = options.size();
        if (nOptions == 0) {
            logger.info("did not find any options for " + filenameShow);
            return new FailedShow("", filenameShow, null);
        }
        if (nOptions == 1) {
            return options.get(0);
        }
        // logger.info("got " + nOptions + " options for " + filenameShow);
        Show selected = null;
        for (int i=0; i<nOptions; i++) {
            Show s = options.get(i);
            String actualName = s.getName();
            if (filenameShow.equals(actualName)) {
                if (selected == null) {
                    selected = s;
                } else {
                    // TODO: could check language?  other criteria?
                    logger.warning("multiple exact hits for " + filenameShow
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

    private static void downloadShow(final String filenameShow, final String queryString) {
        Callable<Boolean> showFetcher = new Callable<Boolean>() {
            @Override
            public Boolean call() throws InterruptedException {
                Show thisShow;
                try {
                    List<Show> options = TheTVDBProvider.getShowOptions(queryString);
                    thisShow = selectShowOption(options, filenameShow);
                } catch (TVRenamerIOException e) {
                    thisShow = new FailedShow("", filenameShow, e);
                }

                logger.fine("Show listing for '" + thisShow.getName() + "' downloaded");
                _shows.put(queryString, thisShow);
                notifyListeners(queryString, thisShow);
                return true;
            }
        };
        threadPool.submit(showFetcher);
    }

    private static void notifyListeners(String queryString, Show show) {
        ShowRegistrations registrations = _showRegistrations.get(queryString);

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
     * @param  filenameShow
     *            the show name as it appears in the filename
     * @param  actualName
     *            the proper show name, as it appears in the provider DB
     * @return show
     *            the {@link Show}
     */
    static Show getOrAddShow(String filenameShow, String actualName) {
        String queryString = makeQueryString(filenameShow);
        Show show = _shows.get(queryString);
        if (show == null) {
            show = new Show(filenameShow, actualName);
            _shows.put(queryString, show);
        }
        return show;
    }
}
