/**
 * TheTVDBProviderTest -- test the code's ability to fetch information from thetvdb.com.
 *
 * This is kind of an unreliable thing to try to do.  We're depending on "correctly"
 * receiving information that we have no control over.  The database we're querying
 * against is generally open to the public, and even when it isn't, it's still open
 * to several administrators who aren't especially invested in our application.
 *
 * Beyond that, the site could simply be down, or we might want to test on a machine
 * that doesn't have internet access.
 *
 * Nevertheless, it's important to give this functionality some testing, and the
 * potential problems discussed are not that likely in practice.  We should try
 * to choose data that is most likely to fail if and only if our tvdb-fetching code
 * is broken, and not for any other reason.
 *
 */

package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.Test;

import org.tvrenamer.model.DiscontinuedApiException;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.EpisodePlacement;
import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.FailedShow;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.ShowOption;
import org.tvrenamer.model.ShowStore;
import org.tvrenamer.model.TVRenamerIOException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TheTVDBProviderTest {
    private static final String API_DISCONTINUED_NAME = "api_dead";
    private static final Boolean RUN_EXTRA_TESTS = false;

    /**
     * Static inner class to use as a Listener for downloading show listings.
     * Takes a completable future in its constructor, and episode information.
     * Makes sure to always complete its future no matter what, and in the
     * success case, provides the downloaded episode title to the future.
     */
    private static class ListingsDownloader implements ShowListingsListener {

        // Once we have a CompletableFuture, we need to complete it.  There are a few ways, but
        // obviously the simplest is to call complete().  If we simply call the JUnit method
        // fail(), the future thread does not die and the test never exits.  The same appears
        // to happen with an uncaught exception.  So, be very careful to make sure, one way or
        // other, we call complete.

        // Of course, none of this matters when everything works.  But if we want failure cases
        // to actually stop and report failure, we need to complete the future, one way or another.

        // We use a brief failure message as the show title in cases where we detect failure.
        // Just make sure to not add a test case where the actual episode's title is one of
        // the failure messages.  :)
        private static final String NO_EPISODE = "null episode";
        private static final String DOWNLOAD_FAILED = "download failed";

        final Show show;
        final EpisodePlacement placement;
        final CompletableFuture<String> future;

        ListingsDownloader(final Show show,
                           final EpisodePlacement placement,
                           final CompletableFuture<String> future) {
            this.show = show;
            this.placement = placement;
            this.future = future;
        }

        @Override
        public void listingsDownloadComplete() {
            Episode ep = show.getEpisode(placement);
            if (ep == null) {
                future.complete(NO_EPISODE);
            } else {
                String title = ep.getTitle();
                future.complete(title);
            }
        }

        @Override
        public void listingsDownloadFailed(Exception err) {
            future.complete(DOWNLOAD_FAILED);
        }
    }

    /**
     * Static inner class to use as a Listener for downloading show information.
     * Takes a completable future in its constructor, and completes it in the callbacks.
     */
    private static class ShowDownloader implements ShowInformationListener {

        final CompletableFuture<ShowOption> futureShow;

        ShowDownloader(final CompletableFuture<ShowOption> futureShow) {
            this.futureShow = futureShow;
        }

        @Override
        public void downloadSucceeded(Show show) {
            futureShow.complete(show);
        }

        @Override
        public void downloadFailed(FailedShow failedShow) {
            futureShow.complete(failedShow);
        }

        @Override
        public void apiHasBeenDeprecated() {
            TVRenamerIOException err
                = new TVRenamerIOException(API_DISCONTINUED_NAME,
                                           new DiscontinuedApiException());
            FailedShow standIn = new FailedShow(API_DISCONTINUED_NAME, err);

            futureShow.complete(standIn);
        }
    }

    /**
     * Fails if the given title does not match the expected title within the EpisodeTestData.
     *
     * @param epdata contains all the relevant information about the episode to look up, and
     *               what we expect to get back about it
     * @param foundTitle the value that was found for the episode title
     */
    private static void assertEpisodeTitle(final EpisodeTestData epdata,
                                           final String foundTitle)
    {
        final String expectedTitle = epdata.episodeTitle;
        if (!expectedTitle.equals(foundTitle)) {
            fail("expected title of season " + epdata.seasonNum + ", episode " + epdata.episodeNum
                 + " of " + epdata.properShowName + " to be \"" + expectedTitle
                 + "\", but got \"" + foundTitle + "\"");
        }
    }

    /**
     * Contacts the provider to look up a show and an episode, and returns true if we found the show
     * and the episode title matches the given expected value.
     *
     * Note that this method does not simply waits for the providers responses.  We don't use
     * callbacks here, so we're not testing that aspect of the real program.
     *
     * @param epdata contains all the relevant information about the episode to look up, and
     *               what we expect to get back about it
     * @param doCheck whether or not to check that the episode title matches the expected
     * @return the title of the given episode of the show returned by the provider, or null
     *         if we didn't get an episode title
     */
    private static String testSeriesNameAndEpisode(final EpisodeTestData epdata, boolean doCheck)
        throws Exception
    {
        final String actualName = epdata.properShowName;
        String queryString = epdata.queryString;
        if (queryString == null) {
            queryString = actualName;
        }
        final ShowName showName = ShowName.mapShowName(queryString);
        ShowOption best = showName.getMatchedShow();

        if (best == null) {
            try {
                TheTVDBProvider.getShowOptions(showName);
            } catch (DiscontinuedApiException api) {
                fail("API deprecation discovered getting show options for " + queryString);
            } catch (Exception e) {
                fail("exception getting show options for " + queryString);
            }
            assertTrue("got no options on showName <[" + showName.getExampleFilename()
                       + "]> (from input <[" + queryString + "]>)",
                       showName.hasShowOptions());

            best = showName.selectShowOption();
        }
        assertEquals("resolved show name <[" + showName.getExampleFilename() + "]> to wrong series;",
                     actualName, best.getName());

        Show show = best.getShowInstance();
        assertTrue("expected valid Series (<[" + epdata.properShowName + "]>) for \""
                   + showName.getExampleFilename() + "\" but got <[" + show + "]>",
                   show.isValidSeries());
        Series series = show.asSeries();
        assertEquals("got wrong series ID for <[" + actualName + "]>;",
                     epdata.showId, String.valueOf(series.getId()));

        if (epdata.preferDvd != null) {
            series.setPreferDvd(epdata.preferDvd);
        }
        if (series.noEpisodes()) {
            TheTVDBProvider.getSeriesListing(series);
        }

        final EpisodePlacement placement = new EpisodePlacement(epdata.seasonNum, epdata.episodeNum);
        final List<Episode> allEps = series.getEpisodes(placement);
        final Episode ep = allEps.get(0);
        if (ep == null) {
            fail("result of calling getEpisode(" + epdata.seasonNum + ", " + epdata.episodeNum
                 + ") on " + actualName + " came back null");
            return null;
        }
        final String foundTitle = ep.getTitle();
        if (doCheck) {
            assertEpisodeTitle(epdata, foundTitle);
        }
        return foundTitle;
    }

    /**
     * Contacts the provider to look up a show and an episode, and returns true if we found the show
     * and the episode title matches the given expected value.
     *
     * Note that this method does not simply waits for the providers responses.  We don't use
     * callbacks here, so we're not testing that aspect of the real program.
     *
     * @param epdata contains all the relevant information about the episode to look up, and
     *               what we expect to get back about it
     */
    private static void testSeriesNameAndEpisodeTitle(final EpisodeTestData epdata)
        throws Exception
    {
        testSeriesNameAndEpisode(epdata, true);
    }

    /**
     * Remember the show, "Quintuplets"?  No?  Good.  The less popular a show is,
     * it figures, the less likely it is for anyone to be editing it.  It's not
     * likely to have a reunion special, or a reboot, or anything of that nature.
     * "Quintuplets" is also a pretty unusual word to be found in the title of
     * a TV show.  At the time this test is created, the query returns only a
     * single option, and that's not too likely to change.  We also avoid having
     * to download a lot of data by choosing a series with just one season.
     *
     */
    @Test
    public void testGetShowOptionsAndListings() throws Exception {
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Quintuplets")
                                      .showId("73732")
                                      .seasonNum(1)
                                      .episodeNum(2)
                                      .episodeTitle("Quintagious")
                                      .build());
    }

    /**
     * Second download test.  This one is specifically chosen to ensure we
     * get the right preferences between "DVD number" and "regular number".
     *
     * This also tests the query string, by not querying for an exact character for
     * character match with the actual show name.
     *
     * This tests Robot Chicken, assuming the following episode placements:
     *
     * Episode Title         DVD Placement  Aired Placement
     * =============         =============  ===============
     * Western Hay Batch     S08E12           S08E11
     * Triple Hot Dog        S08E13           S08E12
     * Joel Hurwitz Returns  S08E14           S08E13
     * Hopefully Salt        (none)           S08E14
     * Yogurt in a Bag       (none)           S08E15
     *
     */
    @Test
    public void testDvdEpisodePreference() throws Exception {
        // In this first case, we specify we want the DVD ordering, so S08E13
        // should resolve to the first one, "Triple Hot Dog Sandwich on Wheat".
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .queryString("robot.chicken.")
                                      .properShowName("Robot Chicken")
                                      .showId("75734")
                                      .seasonNum(8)
                                      .episodeNum(13)
                                      .preferDvd(true)
                                      .episodeTitle("Triple Hot Dog Sandwich on Wheat")
                                      .build());
        // Now we specify a preference of the non-DVD ordering, S08E13 should
        // resolve to the other alternative, "Joel Hurwitz Returns"
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Robot Chicken")
                                      .showId("75734")
                                      .seasonNum(8)
                                      .episodeNum(13)
                                      .preferDvd(false)
                                      .episodeTitle("Joel Hurwitz Returns")
                                      .build());
        // This is meant to test the "fallback".  We go back to explicitly preferring DVD.
        // But for this placement, there is no DVD entry (as of the time of this writing).
        // Given that there is no DVD episode at the placement, it should "fall back" to
        // the over-the-air placement.  Of course, it is possible that in the future, the
        // producers of "Robot Chicken" will put out a "Season 8, part 2" DVD, or some such
        // thing, and then all of a sudden a different episode might appear as S08E15 in
        // the DVD ordering, and this test would start to fail.
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Robot Chicken")
                                      .showId("75734")
                                      .seasonNum(8)
                                      .episodeNum(15)
                                      .preferDvd(true)
                                      .episodeTitle("Yogurt in a Bag")
                                      .build());
        // Now we test S08E14, which was considered a true conflict in earlier versions.
        // That's because there are two episodes for which their BEST placement was the
        // same place.  In earlier versions, we "panicked" and put neither episode in
        // place in the index.  Now that we have the EpisodeOption class, we can store
        // both and retrieve either on demand.  First, try the over-the-air ordering:
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Robot Chicken")
                                      .showId("75734")
                                      .seasonNum(8)
                                      .episodeNum(14)
                                      .preferDvd(false)
                                      .episodeTitle("Hopefully Salt")
                                      .build());
        // Now, the DVD ordering for S08E14:
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Robot Chicken")
                                      .showId("75734")
                                      .seasonNum(8)
                                      .episodeNum(14)
                                      .preferDvd(true)
                                      .episodeTitle("Joel Hurwitz Returns")
                                      .build());
    }

    /**
     * Third download test.  This one is chosen to ensure we are consistent
     * with the numbering scheme.  If we use DVD ordering, it should be for
     * DVD season _and_ DVD episode, and if we use regular, it should be
     * both regular.
     *
     * This assumes the following information:
     *    DVD season 4, DVD episode 10: "The Why of Fry"
     *    air season 4, air episode 10: "A Leela of Her Own"
     *    air season 4, DVD episode 10: "Where the Buggalo Roam"
     *
     * Of course, it makes no sense to look at "air season" and "DVD episode".
     * But that's what we accidentally did in early versions of the program.
     * So this test is intended to verify that the bug is fixed, and check
     * that we don't regress.
     */
    @Test
    public void testSeasonMatchesEpisode() throws Exception {
        final String dvdTitle = "The Why of Fry";
        final String airedTitle = "A Leela of Her Own";
        final String jumbledTitle = "Where the Buggalo Roam";
        EpisodeTestData s04e10 = new EpisodeTestData.Builder()
            .properShowName("Futurama")
            .showId("73871")
            .seasonNum(4)
            .episodeNum(10)
            .episodeTitle(dvdTitle)
            .build();
        final String foundTitle = testSeriesNameAndEpisode(s04e10, false);
        if (airedTitle.equals(foundTitle)) {
            fail("expected to get DVD ordering for Futurama, but got over-the-air ordering");
        }
        if (jumbledTitle.equals(foundTitle)) {
            fail("expected to get purely DVD ordering for Futurama, but got over-the-air season");
        }
        assertEpisodeTitle(s04e10, foundTitle);
    }

    /*
     * Below this comment is a series of another 60 or so episode title tests.  But
     * I don't think these should be run as part of a normal regression test.  There
     * are all the problems discussed above, that the data in the database could change
     * out from under us, or the site could be down.
     *
     * Even beyond that, though, I don't think it makes sense to pull in this amount
     * of data every time we test.  It adds another 30 seconds to the build, and it
     * really doesn't tell us anything that testGetShowOptionsAndListings doesn't tell
     * us already.
     *
     * It would be nice to make it configurable to run on demand.  I'm not sure how to
     * do that.  So for now, I'll just leave it here, and if an individual wants to
     * run these tests, they can just uncomment the @Test annotation, below.
     *
     */
    @Test
    public void testEpisode01() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("game of thrones")
                    .properShowName("Game of Thrones")
                    .seasonNum(5)
                    .episodeNum(1)
                    .episodeTitle("The Wars to Come")
                    .build());

        }
    }

    @Test
    public void testEpisode02() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("24")
                    .properShowName("24")
                    .seasonNum(8)
                    .episodeNum(1)
                    .episodeTitle("Day 8: 4:00 P.M. - 5:00 P.M.")
                    .build());
        }

    }

    @Test
    public void testEpisode03() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("24")
                    .properShowName("24")
                    .seasonNum(7)
                    .episodeNum(18)
                    .episodeTitle("Day 7: 1:00 A.M. - 2:00 A.M.")
                    .build());
        }

    }

    @Test
    public void testEpisode04() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("human target 2010")
                    .properShowName("Human Target (2010)")
                    .seasonNum(1)
                    .episodeNum(2)
                    .episodeTitle("Rewind")
                    .build());
        }

    }

    @Test
    public void testEpisode05() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("dexter")
                    .properShowName("Dexter")
                    .seasonNum(4)
                    .episodeNum(7)
                    .episodeTitle("Slack Tide")
                    .build());
        }

    }

    @Test
    public void testEpisode06() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("jag")
                    .properShowName("JAG")
                    .seasonNum(10)
                    .episodeNum(1)
                    .episodeTitle("Hail and Farewell, Part II (2)")
                    .build());
        }

    }

    @Test
    public void testEpisode07() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("lost")
                    .properShowName("Lost")
                    .seasonNum(6)
                    .episodeNum(5)
                    .episodeTitle("Lighthouse")
                    .build());
        }

    }

    @Test
    public void testEpisode08() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("warehouse 13")
                    .properShowName("Warehouse 13")
                    .seasonNum(1)
                    .episodeNum(1)
                    .episodeTitle("Pilot")
                    .build());
        }

    }

    @Test
    public void testEpisode09() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("one tree hill")
                    .properShowName("One Tree Hill")
                    .seasonNum(7)
                    .episodeNum(14)
                    .episodeTitle("Family Affair")
                    .build());
        }

    }

    @Test
    public void testEpisode10() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("gossip girl")
                    .properShowName("Gossip Girl")
                    .seasonNum(3)
                    .episodeNum(15)
                    .episodeTitle("The Sixteen Year Old Virgin")
                    .build());
        }

    }

    @Test
    public void testEpisode11() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("smallville")
                    .properShowName("Smallville")
                    .seasonNum(9)
                    .episodeNum(14)
                    .episodeTitle("Conspiracy")
                    .build());
        }

    }

    @Test
    public void testEpisode12() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("smallville")
                    .properShowName("Smallville")
                    .seasonNum(9)
                    .episodeNum(15)
                    .episodeTitle("Escape")
                    .build());
        }

    }

    @Test
    public void testEpisode13() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("the big bang theory")
                    .properShowName("The Big Bang Theory")
                    .seasonNum(3)
                    .episodeNum(18)
                    .episodeTitle("The Pants Alternative")
                    .build());
        }

    }

    @Test
    public void testEpisode14() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("castle 2009")
                    .properShowName("Castle (2009)")
                    .seasonNum(1)
                    .episodeNum(9)
                    .episodeTitle("Little Girl Lost")
                    .build());
        }

    }

    @Test
    public void testEpisode15() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("dexter")
                    .properShowName("Dexter")
                    .seasonNum(5)
                    .episodeNum(5)
                    .episodeTitle("First Blood")
                    .build());
        }

    }

    @Test
    public void testEpisode16() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("lost")
                    .properShowName("Lost")
                    .seasonNum(2)
                    .episodeNum(7)
                    .episodeTitle("The Other 48 Days")
                    .build());
        }

    }

    @Test
    public void testEpisode17() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("american dad")
                    .properShowName("American Dad!")
                    .seasonNum(9)
                    .episodeNum(17)
                    .episodeTitle("The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith")
                    .build());
        }

    }

    @Test
    public void testEpisode18() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("californication")
                    .properShowName("Californication")
                    .seasonNum(7)
                    .episodeNum(4)
                    .episodeTitle("Dicks")
                    .build());
        }

    }

    @Test
    public void testEpisode19() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("continuum")
                    .properShowName("Continuum")
                    .seasonNum(3)
                    .episodeNum(7)
                    .episodeTitle("Waning Minutes")
                    .build());
        }

    }

    @Test
    public void testEpisode20() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("elementary")
                    .properShowName("Elementary")
                    .seasonNum(2)
                    .episodeNum(23)
                    .episodeTitle("Art in the Blood")
                    .build());
        }

    }

    @Test
    public void testEpisode21() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("family guy")
                    .properShowName("Family Guy")
                    .seasonNum(12)
                    .episodeNum(19)
                    .episodeTitle("Meg Stinks!")
                    .build());
        }

    }

    @Test
    public void testEpisode22() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("fargo")
                    .properShowName("Fargo")
                    .seasonNum(1)
                    .episodeNum(1)
                    .episodeTitle("The Crocodile's Dilemma")
                    .build());
        }

    }

    @Test
    public void testEpisode23() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("girls")
                    .properShowName("Girls")
                    .seasonNum(3)
                    .episodeNum(11)
                    .episodeTitle("I Saw You")
                    .build());
        }

    }

    @Test
    public void testEpisode24() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("grimm")
                    .properShowName("Grimm")
                    .seasonNum(3)
                    .episodeNum(19)
                    .episodeTitle("Nobody Knows the Trubel I've Seen")
                    .build());
        }

    }

    @Test
    public void testEpisode25() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("house of cards 2013")
                    .properShowName("House of Cards (US)")
                    .seasonNum(1)
                    .episodeNum(6)
                    .episodeTitle("Chapter 6")
                    .build());
        }

    }

    @Test
    public void testEpisode26() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("modern family")
                    .properShowName("Modern Family")
                    .seasonNum(5)
                    .episodeNum(12)
                    .episodeTitle("Under Pressure")
                    .build());
        }

    }

    @Test
    public void testEpisode27() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("new girl")
                    .properShowName("New Girl")
                    .seasonNum(3)
                    .episodeNum(23)
                    .episodeTitle("Cruise")
                    .build());
        }

    }

    @Test
    public void testEpisode28() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("nurse jackie")
                    .properShowName("Nurse Jackie")
                    .seasonNum(6)
                    .episodeNum(4)
                    .episodeTitle("Jungle Love")
                    .build());
        }

    }

    @Test
    public void testEpisode29() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("offspring")
                    .properShowName("Offspring")
                    .seasonNum(5)
                    .episodeNum(1)
                    .episodeTitle("Back in the Game")
                    .build());
        }

    }

    @Test
    public void testEpisode30() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("reign 2013")
                    .properShowName("Reign (2013)")
                    .seasonNum(1)
                    .episodeNum(20)
                    .episodeTitle("Higher Ground")
                    .build());
        }

    }

    @Test
    public void testEpisode31() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("robot chicken")
                    .properShowName("Robot Chicken")
                    .seasonNum(7)
                    .episodeNum(4)
                    .episodeTitle("Rebel Appliance")
                    .build());
        }

    }

    @Test
    public void testEpisode32() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("supernatural")
                    .properShowName("Supernatural")
                    .seasonNum(9)
                    .episodeNum(21)
                    .episodeTitle("King of the Damned")
                    .build());
        }

    }

    @Test
    public void testEpisode33() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("the americans 2013")
                    .properShowName("The Americans (2013)")
                    .seasonNum(2)
                    .episodeNum(10)
                    .episodeTitle("Yousaf")
                    .build());
        }

    }

    @Test
    public void testEpisode34() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("the big bang theory")
                    .properShowName("The Big Bang Theory")
                    .seasonNum(7)
                    .episodeNum(23)
                    .episodeTitle("The Gorilla Dissolution")
                    .build());
        }

    }

    @Test
    public void testEpisode35() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("the good wife")
                    .properShowName("The Good Wife")
                    .seasonNum(5)
                    .episodeNum(20)
                    .episodeTitle("The Deep Web")
                    .build());
        }

    }

    @Test
    public void testEpisode36() {
        // Trying options for "the walking dead" gives a "Series Not Permitted".
        // We issue a warning, but it's not really a problem.
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("the walking dead")
                    .properShowName("The Walking Dead")
                    .seasonNum(4)
                    .episodeNum(16)
                    .episodeTitle("A")
                    .build());
        }

    }

    @Test
    public void testEpisode37() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("veep")
                    .properShowName("Veep")
                    .seasonNum(3)
                    .episodeNum(5)
                    .episodeTitle("Fishing")
                    .build());
        }

    }

    @Test
    public void testEpisode38() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("witches of east end")
                    .properShowName("Witches of East End")
                    .seasonNum(1)
                    .episodeNum(1)
                    .episodeTitle("Pilot")
                    .build());
        }

    }

    @Test
    public void testEpisode39() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("warehouse 13")
                    .properShowName("Warehouse 13")
                    .seasonNum(5)
                    .episodeNum(4)
                    .episodeTitle("Savage Seduction")
                    .build());
        }

    }

    @Ignore("currently disabled due to an incorrect show coming back")
    @Test
    public void testEpisode40() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()

                    .queryString("the 100")
                    .properShowName("The 100")
                    .seasonNum(2)
                    .episodeNum(8)
                    .episodeTitle("Spacewalker")
                    .build());
        }
    }

    @Test
    public void testEpisode41() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(1)
                    .episodeTitle("Serenity")
                    .build());
        }
    }

    @Test
    public void testEpisode42() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(2)
                    .episodeTitle("The Train Job")
                    .build());
        }
    }

    @Test
    public void testEpisode43() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(3)
                    .episodeTitle("Bushwhacked")
                    .build());
        }
    }

    @Test
    public void testEpisode44() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(4)
                    .episodeTitle("Shindig")
                    .build());
        }
    }

    @Test
    public void testEpisode45() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(5)
                    .episodeTitle("Safe")
                    .build());
        }
    }

    @Test
    public void testEpisode46() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(6)
                    .episodeTitle("Our Mrs. Reynolds")
                    .build());
        }
    }

    @Test
    public void testEpisode47() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(7)
                    .episodeTitle("Jaynestown")
                    .build());
        }
    }

    @Test
    public void testEpisode48() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(8)
                    .episodeTitle("Out of Gas")
                    .build());
        }
    }

    @Test
    public void testEpisode49() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(9)
                    .episodeTitle("Ariel")
                    .build());
        }
    }

    @Test
    public void testEpisode50() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(10)
                    .episodeTitle("War Stories")
                    .build());
        }
    }

    @Test
    public void testEpisode51() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(11)
                    .episodeTitle("Trash")
                    .build());
        }
    }

    @Test
    public void testEpisode52() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(12)
                    .episodeTitle("The Message")
                    .build());
        }
    }

    @Test
    public void testEpisode53() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(13)
                    .episodeTitle("Heart of Gold")
                    .build());
        }
    }

    @Test
    public void testEpisode54() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("firefly")
                    .properShowName("Firefly")
                    .seasonNum(1)
                    .episodeNum(14)
                    .episodeTitle("Objects in Space")
                    .build());
        }
    }

    @Test
    public void testEpisode55() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("strike back")
                    .properShowName("Strike Back")
                    .seasonNum(1)
                    .episodeNum(1)
                    .episodeTitle("Chris Ryan's Strike Back, Episode 1")
                    .build());
        }
    }

    @Test
    public void testEpisode56() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("law and order svu")
                    .properShowName("Law & Order: Special Victims Unit")
                    .seasonNum(17)
                    .episodeNum(5)
                    .episodeTitle("Community Policing")
                    .build());
        }
    }

    @Test
    public void testEpisode57() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("ncis")
                    .properShowName("NCIS")
                    .seasonNum(13)
                    .episodeNum(4)
                    .episodeTitle("Double Trouble")
                    .build());
        }
    }

    @Ignore("currently disabled due to an incorrect show coming back")
    @Test
    public void testEpisode58() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("marvels agents of shield")
                    .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                    .seasonNum(3)
                    .episodeNum(3)
                    .episodeTitle("A Wanted (Inhu)man")
                    .build());
        }
    }

    @Ignore("currently disabled due to an incorrect show coming back")
    @Test
    public void testEpisode59() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("marvels agents of shield")
                    .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                    .seasonNum(3)
                    .episodeNum(10)
                    .episodeTitle("Maveth")
                    .build());
        }
    }

    @Test
    public void testEpisode60() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("nip tuck")
                    .properShowName("Nip/Tuck")
                    .seasonNum(6)
                    .episodeNum(1)
                    .episodeTitle("Don Hoberman")
                    .build());
        }
    }

    @Test
    public void testEpisode61() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("the big bang theory")
                    .properShowName("The Big Bang Theory")
                    .seasonNum(10)
                    .episodeNum(4)
                    .episodeTitle("The Cohabitation Experimentation")
                    .build());
        }
    }

    @Test
    public void testEpisode62() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("lucifer")
                    .properShowName("Lucifer")
                    .seasonNum(2)
                    .episodeNum(3)
                    .episodeTitle("Sin-Eater")
                    .build());
        }
    }

    @Ignore("currently disabled due to an incorrect show coming back")
    @Test
    public void testEpisode63() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("marvels agents of shield")
                    .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                    .seasonNum(4)
                    .episodeNum(3)
                    .episodeTitle("Uprising")
                    .build());
        }
    }

    @Test
    public void testEpisode64() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("supernatural")
                    .properShowName("Supernatural")
                    .seasonNum(11)
                    .episodeNum(22)
                    .episodeTitle("We Happy Few")
                    .build());
        }
    }

    @Test
    public void testEpisode65() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("channel zero")
                    .properShowName("Channel Zero")
                    .seasonNum(1)
                    .episodeNum(1)
                    .episodeTitle("You Have to Go Inside")
                    .build());
        }
    }

    @Test
    public void testEpisode66() {
        if (RUN_EXTRA_TESTS) {
            testGetEpisodeDataTitle(new EpisodeTestData.Builder()
                    .queryString("ncis")
                    .properShowName("NCIS")
                    .seasonNum(14)
                    .episodeNum(4)
                    .episodeTitle("Love Boat")
                    .build());
        }
    }

    /**
     * Look up the query string with the provider and return the Show based on the
     * information returned.
     *
     * @param testInput
     *    an EpisodeTestData containing all the values we need to look up
     *    a Show (or an Episode)
     * @return a Show based on the queryString of the testInput, or null
     */
    private static Show testQueryShow(final EpisodeTestData testInput) {
        final String queryString = testInput.queryString;
        final String properShowName = testInput.properShowName;
        try {
            final CompletableFuture<ShowOption> futureShow = new CompletableFuture<>();
            ShowStore.mapStringToShow(queryString, new ShowDownloader(futureShow));
            ShowOption gotShow = futureShow.get(4, TimeUnit.SECONDS);
            if (API_DISCONTINUED_NAME.equals(gotShow.getName())) {
                fail("API apparently discontinued parsing " + queryString);
                return null;
            }
            Show show = gotShow.getShowInstance();
            assertTrue("expected valid Series (<[" + properShowName + "]>) for \""
                       + queryString + "\" but got <[" + show + "]>",
                       show.isValidSeries());
            assertEquals("resolved show name <[" + properShowName + "]> to wrong series;",
                         properShowName, show.getName());
            return show;
        } catch (TimeoutException e) {
            String failMsg = "timeout trying to query for " + queryString;
            String exceptionMessage = e.getMessage();
            if (exceptionMessage != null) {
                failMsg += exceptionMessage;
            } else {
                failMsg += "(no message)";
            }
            fail(failMsg);
            return null;
        } catch (Exception e) {
            fail("failure (possibly timeout?) trying to query for " + queryString
                 + ": " + e.getMessage());
            return null;
        }
    }

    private static String timeoutExceptionMessage(final EpisodeTestData testInput,
                                                  final TimeoutException e)
    {
        final String queryString = testInput.queryString;
        final int seasonNum = testInput.seasonNum;
        final int episodeNum = testInput.episodeNum;

        String failMsg = "timeout trying to query for " + queryString
            + ", season " + seasonNum + ", episode " + episodeNum;
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
            failMsg += exceptionMessage;
        } else {
            failMsg += "(no message)";
        }
        return failMsg;
    }

    private static String genericExceptionMessage(final EpisodeTestData testInput,
                                                  final Exception e)
    {
        final String queryString = testInput.queryString;
        final int seasonNum = testInput.seasonNum;
        final int episodeNum = testInput.episodeNum;

        String failMsg = "failure trying to query for " + queryString
            + ", season " + seasonNum + ", episode " + episodeNum
            + e.getClass().getName() + " ";
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
            failMsg += exceptionMessage;
        } else {
            failMsg += "(possibly timeout?)";
        }
        return failMsg;
    }

    private static void assertGotShow(final Show show,
                                      final EpisodeTestData testInput)
    {
        assertNotNull("got null value from testQueryShow on <["
                      + testInput.queryString + "]>",
                      show);
    }

    private static void assertValidSeries(final Show show,
                                          final EpisodeTestData testInput)
    {
        assertTrue("expected valid Series (<[" + testInput.properShowName
                   + "]>) for \"" + testInput.queryString
                   + "\" but got <[" + show + "]>",
                   show.isValidSeries());
    }

    private static CompletableFuture<String> createListingsFuture(final Series series,
                                                                  final EpisodeTestData testInput)
    {
        final EpisodePlacement placement = new EpisodePlacement(testInput.seasonNum,
                                                                testInput.episodeNum);
        final CompletableFuture<String> future = new CompletableFuture<>();
        series.addListingsListener(new ListingsDownloader(series, placement, future));

        return future;
    }

    /**
     * Run testQueryShow to validate we get the expected show from the given
     * queryString, and then look up the listings to verify we get the expected
     * episode.  Does not return a value or throw an exception.  Just fails the
     * calling test if anything is not as expected.
     *
     * @param testInput
     *    an EpisodeTestData containing all the values we need to look up
     *    an episode
     */
    private static void testGetEpisodeDataTitle(final EpisodeTestData testInput) {
        try {
            final Show show = testQueryShow(testInput);
            assertGotShow(show, testInput);
            assertValidSeries(show, testInput);
            if (testInput.preferDvd != null) {
                show.setPreferDvd(testInput.preferDvd);
            }

            final CompletableFuture<String> future = createListingsFuture(show.asSeries(),
                                                                          testInput);
            String got = future.get(30, TimeUnit.SECONDS);
            assertEpisodeTitle(testInput, got);
        } catch (TimeoutException e) {
            fail(timeoutExceptionMessage(testInput, e));
        } catch (Exception e) {
            fail(genericExceptionMessage(testInput, e));
        }
    }
}
