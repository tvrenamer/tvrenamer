/**
 * TVmazeProviderTest -- test the code's ability to fetch information from tvmaze.com.
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
 * to choose data that is most likely to fail if and only if our provider-fetching
 * code is broken, and not for any other reason.
 *
 */

package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import org.tvrenamer.model.DiscontinuedApiException;
import org.tvrenamer.model.Episode;
import org.tvrenamer.model.EpisodePlacement;
import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.ShowOption;

import java.util.List;

public class TVmazeProviderTest {

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
                TVmazeProvider.getShowOptions(showName);
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
            TVmazeProvider.getSeriesListing(series);
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
                                      .showId("4728")
                                      .seasonNum(1)
                                      .episodeNum(2)
                                      .episodeTitle("Quintagious")
                                      .build());
    }

    /**
     * Second download test.  TVmaze publishes only the over-the-air ordering, so
     * every episode arrives without DVD placement information.  That makes this a
     * test of the fallback in {@link org.tvrenamer.model.Show}: whichever ordering
     * the show prefers, an episode with no DVD placement still has to be findable
     * at its aired placement.
     *
     * This also tests the query string, by not querying for an exact character for
     * character match with the actual show name.
     *
     * This assumes the following aired placements for Robot Chicken season 8:
     *
     * Episode Title                  Aired Placement
     * =============                  ===============
     * Western Hay Batch                S08E12
     * Triple Hot Dog Sandwich          S08E13
     * Joel Hurwitz Returns             S08E14
     */
    @Test
    public void testAiredPlacementWithoutDvdData() throws Exception {
        // The default preference is for DVD ordering, which the provider does not
        // give us, so this has to fall back to the aired placement.
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .queryString("robot.chicken.")
                                      .properShowName("Robot Chicken")
                                      .showId("686")
                                      .seasonNum(8)
                                      .episodeNum(13)
                                      .preferDvd(true)
                                      .episodeTitle("Triple Hot Dog Sandwich on Wheat")
                                      .build());
        // Asking for the aired ordering explicitly must give the same answer.
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Robot Chicken")
                                      .showId("686")
                                      .seasonNum(8)
                                      .episodeNum(13)
                                      .preferDvd(false)
                                      .episodeTitle("Triple Hot Dog Sandwich on Wheat")
                                      .build());
        // A neighbouring placement, to catch an off-by-one in the index.
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Robot Chicken")
                                      .showId("686")
                                      .seasonNum(8)
                                      .episodeNum(14)
                                      .preferDvd(true)
                                      .episodeTitle("Joel Hurwitz Returns")
                                      .build());
    }

    /**
     * Third download test.  This one is chosen to ensure the season number and
     * the episode number are read from the same ordering.  Early versions of the
     * program mixed them, taking the aired season with the DVD episode number.
     *
     * Futurama is a good check because its DVD ordering differs from its aired
     * ordering.  TVmaze gives us the aired ordering:
     *    air season 4, air episode 10: "A Leela of Her Own"
     * while "The Why of Fry" is DVD season 4, DVD episode 10, and "Where the
     * Buggalo Roam" is what mixing the two orderings used to produce.  Getting
     * either of those back means the numbering is being crossed somewhere.
     */
    @Test
    public void testSeasonMatchesEpisode() throws Exception {
        final String dvdTitle = "The Why of Fry";
        final String airedTitle = "A Leela of Her Own";
        final String jumbledTitle = "Where the Buggalo Roam";
        EpisodeTestData s04e10 = new EpisodeTestData.Builder()
            .properShowName("Futurama")
            .showId("538")
            .seasonNum(4)
            .episodeNum(10)
            .episodeTitle(airedTitle)
            .build();
        final String foundTitle = testSeriesNameAndEpisode(s04e10, false);
        if (dvdTitle.equals(foundTitle)) {
            fail("expected over-the-air ordering for Futurama, but got DVD ordering");
        }
        if (jumbledTitle.equals(foundTitle)) {
            fail("got Futurama episode from a mix of the DVD and over-the-air orderings");
        }
        assertEpisodeTitle(s04e10, foundTitle);
    }
}
