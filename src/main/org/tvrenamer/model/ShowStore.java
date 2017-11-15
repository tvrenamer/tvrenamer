package org.tvrenamer.model;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TheTVDBProvider;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

/**
 * ShowStore -- maps strings to Show objects.<p>
 *
 * Note that, just for a single file, we may have up to five versions of the show's "name".
 * Let's look at an example.  Say we have a file named "Cosmos, A Space Time Odyssey S01E02.mp4".
 * The first thing we do is try to extract the show name from the filename.  If we do it right,
 * we'll get "Cosmos, A Space Time Odyssey".  That string is stored as the "filenameShow" of
 * the FileEpisode.<p>
 *
 * Next, we'll want to query for that string.  But first we try to eliminate characters that
 * are either problematic, because they might serve as meta-characters in various contexts,
 * or simply irrelevant.  We consider show titles to essentially be case-insensitive, and
 * we don't think punctuation matters, at this point.  So we normalize the string.  Since
 * this is the text we're going to send to the provider to query for which actual show it
 * might match, I sometimes call this the "query string".  In this case, it would be
 * "cosmos a space time odyssey".<p>
 *
 * Then, from the provider, we get back the actual show name: "Cosmos: A Spacetime Odyssey".<p>
 *
 * But this is a bit of a problem, because Windows does not allow the colon character to
 * appear in filenames.  So we "sanitise" the title to "Cosmos - A Spacetime Odyssey".
 * That's four versions of the same show name.<p>
 *
 * The fifth?  We allow users to set a preference to use dots instead of spaces in the
 * filenames, which would turn this into "Cosmos-A.Spacetime.Odyssey".<p>
 *
 * (Note that I did say, "up to" five versions.  In the case of a show like "Futurama",
 * we'd likely only deal with two versions, upper-case and lower-case.  For "24", there
 * is probably just the one version.)<p>
 *
 * Once again, in table form:
 * <table summary="Versions of a show's name">
 *  <tr><td>(1) filename show</td><td>"Cosmos, A Space Time Odyssey"</td></tr>
 *  <tr><td>(2) query string</td>       <td>"cosmos a space time odyssey"</td></tr>
 *  <tr><td>(3) actual show name</td>   <td>"Cosmos: A Spacetime Odyssey"</td></tr>
 *  <tr><td>(4) sanitised name</td>     <td>"Cosmos - A Spacetime Odyssey"</td></tr>
 *  <tr><td>(5) output name</td>        <td>"Cosmos-A.Spacetime.Odyssey"</td></tr></table><p>
 *
 * Most of these transitions are simple string transformations, provided by
 * {@link org.tvrenamer.controller.util.StringUtils}:<ul>
 *  <li>(1) -&gt;(2) makeQueryString</li>
 *  <li>(3) -&gt;(4) sanitiseTitle</li>
 *  <li>(4) -&gt;(5) makeDotTitle</li></ul><p>
 *
 * This file is how we get from (2) -&gt;(3).  It maps query strings to Show objects, and the
 * Show objects obviously contain the actual show name.  So we have:<ul>
 *
 *  <li>(1) -&gt;(2)  makeQueryString</li>
 *  <li>(2) -&gt;(3a) ShowStore.getShow</li>
 *  <li>(3a) -&gt;(3) Show.getName</li>
 *  <li>(3) -&gt;(4)  sanitiseTitle</li>
 *  <li>(4) -&gt;(5)  makeDotTitle</li></ul><p>
 *
 * Note that makeQueryString should be idempotent.  If you already have a query string, and
 * you call makeQueryString on it, you should get back the identical string.<p>
 *
 * One other small note, the "actual show name" is not necessarily the true, actual actual
 * show name.  In fact, the strings we consider as "actual show name" are expected to be
 * unique (not sure if I can say "guaranteed", that's kind of out of our hands), whereas
 * actual show names are not.  There was never a show called "Archer (2009)"; the show that
 * refers to was just called "Archer".  But The TVDB adds the date because there had been
 * a previous show called "Archer".<p>
 *
 * This is true despite the fact that Shows also have a show ID, which is presumably even more
 * guaranteed to be unique.<p>
 *
 * Given the assumption about the uniqueness of the "actual show name", we hope to have:<ul>
 *  <li>(1) -&gt;(2)  many to one</li>
 *  <li>(2) -&gt;(3a) many to one</li>
 *  <li>(3a) -&gt;(3) one to one</li>
 *  <li>(3) -&gt;(4)  one to one</li>
 *  <li>(4) -&gt;(5)  one to one</li></ul><p>
 *
 * I still must say "hope to have", because this does all depend on the idea that a show
 * is never identified by punctuation or case.  That is, if we had DIFFERENT shows, one
 * called "Cosmos: A Spacetime Odyssey" and the other called "Cosmos - A Spacetime Odyssey",
 * or the other called "Cosmos: a spacetime odyssey", we would not be able to accurately
 * tell them apart.  But it's a safe assumption that won't happen.<p>
 *
 * On the other hand, we likely DO have issues involving the non-uniqueness of a title like
 * "Archer" or "The Office".  The fact that The TVDB assigns unique names to these series
 * does not necessarily help us much in doing the (2) -&gt;(3a) mapping.<p>
 *
 * What we might want to do in the future is make it potentially a many-to-many relation,
 * and say that calling getShow() does not necessarily pin down the exact series the file
 * refers to.  We might be able to figure it out later, based on additional information.
 * For example, if we're looking at "The Office, Season 8", we know it has to be the US
 * version, because the UK version didn't do that many seasons.  Or, if the actual episode
 * name is already embedded in the filename, we could try to match that up with the information
 * we get about episode listings.<p>
 *
 * Perhaps the best option would be to have something in the UI to notify the user of the
 * ambiguity, make our best guess, and let the user correct it, if they like. But we don't
 * have that functionality, now.<p>
 *
 * So, anyway, this class.  :)  Again, this is the (2) -&gt;(3a) step, mapping query strings
 * to Show objects.  The real work here is when we take a query string, pass it to the
 * provider to get back a list of options, choose the best option, and return it to the
 * listener via callback.  But we do, of course, also store the mapping in a hash map, so
 * if a second file comes in with the same query string, we don't go look it up again,
 * but simply return the same answer we gave the first time.
 *
 */
public class ShowStore {

    private static final Logger logger = Logger.getLogger(ShowStore.class.getName());

    private static final ExecutorService threadPool = Executors.newCachedThreadPool();


    /**
     * Submits the task to download the information about the ShowName.
     *
     * Makes sure that the task is successfully submitted, and provides the
     * ShowName with an alternate path if anything goes wrong with the task.
     *
     * @param showName
     *    an object containing the part of the filename that is presumed to name
     *    the show, as well as the version of that string we can give the provider
     * @param showFetcher
     *    the task that will download the information
     */
    private static void submitDownloadTask(final ShowName showName,
                                           final Callable<Boolean> showFetcher)
    {
        Future<Boolean> result = null;
        Show failure = null;
        try {
            result = threadPool.submit(showFetcher);
        } catch (RejectedExecutionException | NullPointerException e) {
            logger.warning("unable to submit download task (" + showName + ") for execution");
            failure = showName.getFailedShow(new TVRenamerIOException(e.getMessage()));
        }
        if ((result == null) && (failure == null)) {
            logger.warning("not downloading " + showName);
            failure = showName.getFailedShow(null);
        }
        if (failure != null) {
            showName.nameNotFound(failure);
        }
    }

    /**
     * <p>
     * Download the show details if required, otherwise notify listener.
     * </p>
     * <ul>
     * <li>if we have already downloaded the show (the ShowName returns a matched show)
     *     then just notify the listener</li>
     * <li>if we don't have the show, but are in the process of downloading the show
     *     (the show already has listeners) then add the listener to the registration</li>
     * <li>if we don't have the show and aren't downloading, then add the listener and
     *     kick off the download</li>
     * </ul>
     *
     * @param filenameShow
     *            the name of the show as it appears in the filename
     * @param listener
     *            the listener to notify or register
     */
    public static void getShow(String filenameShow, ShowInformationListener listener) {
        if (listener == null) {
            logger.warning("cannot look up show without a listener");
            return;
        }
        ShowName showName = ShowName.lookupShowName(filenameShow);
        Show show = showName.getMatchedShow();

        if (show == null) {
            // Since "show" is null, we know we haven't downloaded the options for
            // this filenameShow yet; that is, we know we haven't FINISHED doing so.
            // But we might have started.  If the showName already has one or more
            // listeners, that means the download is already underway.
            synchronized (showName) {
                boolean needsDownload = !showName.hasListeners();
                // We add this listener whether or not the download has been started.
                showName.addListener(listener);
                // Now we start a download only if we need to.
                if (needsDownload) {
                    downloadShow(showName);
                }
            }
        } else {
            // Since we've already downloaded the show, we don't need to involve the
            // ShowName at all.  We invoke the listener's callback immediately and
            // directly.  If, in the future, we expand ShowInformationListener so
            // that there is more information to be sent later, we'd want to edit
            // this to add the listener.
            if (show instanceof LocalShow) {
                listener.downloadFailed(show);
            } else {
                listener.downloaded(show);
            }
        }
    }

    /**
     * Download information about shows that match the given ShowName, and
     * choose the best option, if one exists.
     *
     * This method is private, because only this class can decide when it is
     * necessary to go to the provider to get information.  We might already
     * have the information.  Callers must go through the public interfaces
     * which check our internal data structures before initiating an call to
     * the provider.
     *
     * Does not return the value.  Spawns a thread to notify all interested
     * listeners after it has an answer.
     *
     * @param showName
     *    an object containing the part of the filename that is presumed to name
     *    the show, as well as the version of that string we can give the provider
     *
     * Returns nothing; but via callback, sends the series from the list which best
     * matches the series information.
     */
    private static void downloadShow(final ShowName showName) {
        Callable<Boolean> showFetcher = () -> {
            Show thisShow;
            try {
                TheTVDBProvider.getShowOptions(showName);
                thisShow = showName.selectShowOption();
            } catch (TVRenamerIOException e) {
                thisShow = showName.getFailedShow(e);
            }

            logger.fine("Show options for '" + thisShow.getName() + "' downloaded");
            if (thisShow instanceof FailedShow) {
                showName.nameNotFound(thisShow);
            } else {
                showName.nameResolved(thisShow);
            }
            return true;
        };
        submitDownloadTask(showName, showFetcher);
    }

    public static void cleanUp() {
        threadPool.shutdownNow();
    }

    /**
     * Create a show and add it to the store, unless a show is already registered
     * by the show name.<p>
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
        ShowName showName = ShowName.lookupShowName(filenameShow);
        Show show = showName.getMatchedShow();
        if (show == null) {
            show = showName.getLocalShow(actualName);
        }
        return show;
    }
}
