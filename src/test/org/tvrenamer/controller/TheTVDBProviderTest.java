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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.Episode;
import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.LocalShow;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.ShowStore;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TheTVDBProviderTest {

    /**
     * Fails if the given title does not match the expected title within the EpisodeTestData.
     *
     * @param epdata contains all the relevant information about the episode to look up, and
     *               what we expect to get back about it
     * @param foundTitle the value that was found for the episode title
     */
    public void assertEpisodeTitle(final EpisodeTestData epdata, final String foundTitle) {
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
    public String testSeriesNameAndEpisode(final EpisodeTestData epdata, boolean doCheck)
        throws Exception
    {
        final String actualName = epdata.properShowName;
        final ShowName showName = ShowName.lookupShowName(actualName);

        try {
            TheTVDBProvider.getShowOptions(showName);
        } catch (Exception e) {
            fail("exception getting show options for " + actualName);
        }
        assertTrue(showName.hasShowOptions());
        final Show best = showName.selectShowOption();
        assertNotNull(best);
        assertFalse(best instanceof LocalShow);
        assertEquals(epdata.showId, String.valueOf(best.getId()));
        assertEquals(actualName, best.getName());

        TheTVDBProvider.getShowListing(best);

        final Episode ep = best.getEpisode(epdata.seasonNum, epdata.episodeNum);
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
     * @return the title of the given episode of the show returned by the provider, or null
     *         if we didn't get an episode title
     */
    public String testSeriesNameAndEpisodeTitle(final EpisodeTestData epdata) throws Exception {
        return testSeriesNameAndEpisode(epdata, true);
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
     */
    @Test
    public void testRegularEpisodePreference() throws Exception {
        testSeriesNameAndEpisodeTitle(new EpisodeTestData.Builder()
                                      .properShowName("Firefly")
                                      .showId("78874")
                                      .seasonNum(1)
                                      .episodeNum(2)
                                      .episodeTitle("The Train Job")
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

    private static final List<EpisodeTestData> values = new LinkedList<>();

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
     * do that.  So for now, I'll just leave it here, and if an inidividual wants to
     * run these tests, they can just uncomment the @Test annotation, below.
     *
     */
    @BeforeClass
    public static void setupValues01() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("game of thrones")
                   .properShowName("Game of Thrones")
                   .seasonNum(5)
                   .episodeNum(1)
                   .episodeTitle("The Wars to Come")
                   .build());
    }

    @BeforeClass
    public static void setupValues02() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("24")
                   .properShowName("24")
                   .seasonNum(8)
                   .episodeNum(1)
                   .episodeTitle("Day 8: 4:00 P.M. - 5:00 P.M.")
                   .build());
    }

    @BeforeClass
    public static void setupValues03() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("24")
                   .properShowName("24")
                   .seasonNum(7)
                   .episodeNum(18)
                   .episodeTitle("Day 7: 1:00 A.M. - 2:00 A.M.")
                   .build());
    }

    @BeforeClass
    public static void setupValues04() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("human target 2010")
                   .properShowName("Human Target (2010)")
                   .seasonNum(1)
                   .episodeNum(2)
                   .episodeTitle("Rewind")
                   .build());
    }

    @BeforeClass
    public static void setupValues05() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("dexter")
                   .properShowName("Dexter")
                   .seasonNum(4)
                   .episodeNum(7)
                   .episodeTitle("Slack Tide")
                   .build());
    }

    @BeforeClass
    public static void setupValues06() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("jag")
                   .properShowName("JAG")
                   .seasonNum(10)
                   .episodeNum(1)
                   .episodeTitle("Hail and Farewell, Part II (2)")
                   .build());
    }

    @BeforeClass
    public static void setupValues07() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("lost")
                   .properShowName("Lost")
                   .seasonNum(6)
                   .episodeNum(5)
                   .episodeTitle("Lighthouse")
                   .build());
    }

    @BeforeClass
    public static void setupValues08() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("warehouse 13")
                   .properShowName("Warehouse 13")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Pilot")
                   .build());
    }

    @BeforeClass
    public static void setupValues09() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("one tree hill")
                   .properShowName("One Tree Hill")
                   .seasonNum(7)
                   .episodeNum(14)
                   .episodeTitle("Family Affair")
                   .build());
    }

    @BeforeClass
    public static void setupValues10() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("gossip girl")
                   .properShowName("Gossip Girl")
                   .seasonNum(3)
                   .episodeNum(15)
                   .episodeTitle("The Sixteen Year Old Virgin")
                   .build());
    }

    @BeforeClass
    public static void setupValues11() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("smallville")
                   .properShowName("Smallville")
                   .seasonNum(9)
                   .episodeNum(14)
                   .episodeTitle("Conspiracy")
                   .build());
    }

    @BeforeClass
    public static void setupValues12() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("smallville")
                   .properShowName("Smallville")
                   .seasonNum(9)
                   .episodeNum(15)
                   .episodeTitle("Escape")
                   .build());
    }

    @BeforeClass
    public static void setupValues13() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the big bang theory")
                   .properShowName("The Big Bang Theory")
                   .seasonNum(3)
                   .episodeNum(18)
                   .episodeTitle("The Pants Alternative")
                   .build());
    }

    @BeforeClass
    public static void setupValues14() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("castle 2009")
                   .properShowName("Castle (2009)")
                   .seasonNum(1)
                   .episodeNum(9)
                   .episodeTitle("Little Girl Lost")
                   .build());
    }

    @BeforeClass
    public static void setupValues15() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("dexter")
                   .properShowName("Dexter")
                   .seasonNum(5)
                   .episodeNum(5)
                   .episodeTitle("First Blood")
                   .build());
    }

    @BeforeClass
    public static void setupValues16() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("lost")
                   .properShowName("Lost")
                   .seasonNum(2)
                   .episodeNum(7)
                   .episodeTitle("The Other 48 Days")
                   .build());
    }

    @BeforeClass
    public static void setupValues17() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("american dad")
                   .properShowName("American Dad!")
                   .seasonNum(9)
                   .episodeNum(17)
                   .episodeTitle("The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith")
                   .build());
    }

    @BeforeClass
    public static void setupValues18() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("californication")
                   .properShowName("Californication")
                   .seasonNum(7)
                   .episodeNum(4)
                   .episodeTitle("Dicks")
                   .build());
    }

    @BeforeClass
    public static void setupValues19() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("continuum")
                   .properShowName("Continuum")
                   .seasonNum(3)
                   .episodeNum(7)
                   .episodeTitle("Waning Minutes")
                   .build());
    }

    @BeforeClass
    public static void setupValues20() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("elementary")
                   .properShowName("Elementary")
                   .seasonNum(2)
                   .episodeNum(23)
                   .episodeTitle("Art in the Blood")
                   .build());
    }

    @BeforeClass
    public static void setupValues21() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("family guy")
                   .properShowName("Family Guy")
                   .seasonNum(12)
                   .episodeNum(19)
                   .episodeTitle("Meg Stinks!")
                   .build());
    }

    @BeforeClass
    public static void setupValues22() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("fargo")
                   .properShowName("Fargo")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("The Crocodile's Dilemma")
                   .build());
    }

    @BeforeClass
    public static void setupValues23() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("girls")
                   .properShowName("Girls")
                   .seasonNum(3)
                   .episodeNum(11)
                   .episodeTitle("I Saw You")
                   .build());
    }

    @BeforeClass
    public static void setupValues24() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("grimm")
                   .properShowName("Grimm")
                   .seasonNum(3)
                   .episodeNum(19)
                   .episodeTitle("Nobody Knows the Trubel I've Seen")
                   .build());
    }

    @BeforeClass
    public static void setupValues25() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("house of cards 2013")
                   .properShowName("House of Cards (US)")
                   .seasonNum(1)
                   .episodeNum(6)
                   .episodeTitle("Chapter 6")
                   .build());
    }

    @BeforeClass
    public static void setupValues26() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("modern family")
                   .properShowName("Modern Family")
                   .seasonNum(5)
                   .episodeNum(12)
                   .episodeTitle("Under Pressure")
                   .build());
    }

    @BeforeClass
    public static void setupValues27() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("new girl")
                   .properShowName("New Girl")
                   .seasonNum(3)
                   .episodeNum(23)
                   .episodeTitle("Cruise")
                   .build());
    }

    @BeforeClass
    public static void setupValues28() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("nurse jackie")
                   .properShowName("Nurse Jackie")
                   .seasonNum(6)
                   .episodeNum(4)
                   .episodeTitle("Jungle Love")
                   .build());
    }

    // @BeforeClass
    public static void setupValues29() {
        // Comment this out because Offspring has three untitled episodes with
        // the same season and episode, but different IDs
        values.add(new EpisodeTestData.Builder()
                   .queryString("offspring")
                   .properShowName("Offspring")
                   .seasonNum(5)
                   .episodeNum(1)
                   .episodeTitle("Back in the Game")
                   .build());
    }

    @BeforeClass
    public static void setupValues30() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("reign 2013")
                   .properShowName("Reign (2013)")
                   .seasonNum(1)
                   .episodeNum(20)
                   .episodeTitle("Higher Ground")
                   .build());
    }

    // @BeforeClass
    public static void setupValues31() {
        // Comment this out because Robot Chicken has a conflict between DVD
        // and regular numbering.
        values.add(new EpisodeTestData.Builder()
                   .queryString("robot chicken")
                   .properShowName("Robot Chicken")
                   .seasonNum(7)
                   .episodeNum(4)
                   .episodeTitle("Rebel Appliance")
                   .build());
    }

    @BeforeClass
    public static void setupValues32() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("supernatural")
                   .properShowName("Supernatural")
                   .seasonNum(9)
                   .episodeNum(21)
                   .episodeTitle("King of the Damned")
                   .build());
    }

    @BeforeClass
    public static void setupValues33() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the americans 2013")
                   .properShowName("The Americans (2013)")
                   .seasonNum(2)
                   .episodeNum(10)
                   .episodeTitle("Yousaf")
                   .build());
    }

    @BeforeClass
    public static void setupValues34() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the big bang theory")
                   .properShowName("The Big Bang Theory")
                   .seasonNum(7)
                   .episodeNum(23)
                   .episodeTitle("The Gorilla Dissolution")
                   .build());
    }

    // @BeforeClass
    public static void setupValues35() {
        // Comment this out because the episode listing for The Good Wife
        // currently (2017/06/02) comes through unparseable.
        values.add(new EpisodeTestData.Builder()
                   .queryString("the good wife")
                   .properShowName("The Good Wife")
                   .seasonNum(5)
                   .episodeNum(20)
                   .episodeTitle("The Deep Web")
                   .build());
    }

    // @BeforeClass
    public static void setupValues36() {
        // Trying options for "the walking dead" gives a "Series Not Permitted".
        // We issue a warning, but it's not really a problem.
        values.add(new EpisodeTestData.Builder()
                   .queryString("the walking dead")
                   .properShowName("The Walking Dead")
                   .seasonNum(4)
                   .episodeNum(16)
                   .episodeTitle("A")
                   .build());
    }

    @BeforeClass
    public static void setupValues37() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("veep")
                   .properShowName("Veep")
                   .seasonNum(3)
                   .episodeNum(5)
                   .episodeTitle("Fishing")
                   .build());
    }

    @BeforeClass
    public static void setupValues38() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("witches of east end")
                   .properShowName("Witches of East End")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Pilot")
                   .build());
    }

    @BeforeClass
    public static void setupValues39() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("warehouse 13")
                   .properShowName("Warehouse 13")
                   .seasonNum(5)
                   .episodeNum(4)
                   .episodeTitle("Savage Seduction")
                   .build());
    }

    @BeforeClass
    public static void setupValues40() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the 100")
                   .properShowName("The 100")
                   .seasonNum(2)
                   .episodeNum(8)
                   .episodeTitle("Spacewalker")
                   .build());
    }

    @BeforeClass
    public static void setupValues41() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Serenity")
                   .build());
    }

    @BeforeClass
    public static void setupValues42() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(2)
                   .episodeTitle("The Train Job")
                   .build());
    }

    @BeforeClass
    public static void setupValues43() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(3)
                   .episodeTitle("Bushwhacked")
                   .build());
    }

    @BeforeClass
    public static void setupValues44() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(4)
                   .episodeTitle("Shindig")
                   .build());
    }

    @BeforeClass
    public static void setupValues45() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(5)
                   .episodeTitle("Safe")
                   .build());
    }

    @BeforeClass
    public static void setupValues46() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(6)
                   .episodeTitle("Our Mrs. Reynolds")
                   .build());
    }

    @BeforeClass
    public static void setupValues47() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(7)
                   .episodeTitle("Jaynestown")
                   .build());
    }

    @BeforeClass
    public static void setupValues48() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(8)
                   .episodeTitle("Out of Gas")
                   .build());
    }

    @BeforeClass
    public static void setupValues49() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(9)
                   .episodeTitle("Ariel")
                   .build());
    }

    @BeforeClass
    public static void setupValues50() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(10)
                   .episodeTitle("War Stories")
                   .build());
    }

    @BeforeClass
    public static void setupValues51() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(11)
                   .episodeTitle("Trash")
                   .build());
    }

    @BeforeClass
    public static void setupValues52() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(12)
                   .episodeTitle("The Message")
                   .build());
    }

    @BeforeClass
    public static void setupValues53() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(13)
                   .episodeTitle("Heart of Gold")
                   .build());
    }

    @BeforeClass
    public static void setupValues54() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .properShowName("Firefly")
                   .seasonNum(1)
                   .episodeNum(14)
                   .episodeTitle("Objects in Space")
                   .build());
    }

    // @BeforeClass
    public static void setupValues55() {
        // Comment this out because "strike back" apparently no longer
        // resolves to the correct show.
        values.add(new EpisodeTestData.Builder()
                   .queryString("strike back")
                   .properShowName("Strike Back")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Chris Ryan's Strike Back, Episode 1")
                   .build());
    }

    @BeforeClass
    public static void setupValues56() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("law and order svu")
                   .properShowName("Law & Order: Special Victims Unit")
                   .seasonNum(17)
                   .episodeNum(5)
                   .episodeTitle("Community Policing")
                   .build());
    }

    @BeforeClass
    public static void setupValues57() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("ncis")
                   .properShowName("NCIS")
                   .seasonNum(13)
                   .episodeNum(4)
                   .episodeTitle("Double Trouble")
                   .build());
    }

    @BeforeClass
    public static void setupValues58() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("marvels agents of shield")
                   .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                   .seasonNum(3)
                   .episodeNum(3)
                   .episodeTitle("A Wanted (Inhu)man")
                   .build());
    }

    @BeforeClass
    public static void setupValues59() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("marvels agents of shield")
                   .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                   .seasonNum(3)
                   .episodeNum(10)
                   .episodeTitle("Maveth")
                   .build());
    }

    @BeforeClass
    public static void setupValues60() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("nip tuck")
                   .properShowName("Nip/Tuck")
                   .seasonNum(6)
                   .episodeNum(1)
                   .episodeTitle("Don Hoberman")
                   .build());
    }

    @BeforeClass
    public static void setupValues61() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the big bang theory")
                   .properShowName("The Big Bang Theory")
                   .seasonNum(10)
                   .episodeNum(4)
                   .episodeTitle("The Cohabitation Experimentation")
                   .build());
    }

    // @BeforeClass
    public static void setupValues62() {
        // Comment this out because "Lucifer" has conflicting special episodes
        // (season "0", episode "2")
        values.add(new EpisodeTestData.Builder()
                   .queryString("lucifer")
                   .properShowName("Lucifer")
                   .seasonNum(2)
                   .episodeNum(3)
                   .episodeTitle("Sin-Eater")
                   .build());
    }

    @BeforeClass
    public static void setupValues63() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("marvels agents of shield")
                   .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                   .seasonNum(4)
                   .episodeNum(3)
                   .episodeTitle("Uprising")
                   .build());
    }

    @BeforeClass
    public static void setupValues64() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("supernatural")
                   .properShowName("Supernatural")
                   .seasonNum(11)
                   .episodeNum(22)
                   .episodeTitle("We Happy Few")
                   .build());
    }

    @BeforeClass
    public static void setupValues65() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("channel zero")
                   .properShowName("Channel Zero")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("You Have to Go Inside")
                   .build());
    }

    @BeforeClass
    public static void setupValues66() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("ncis")
                   .properShowName("NCIS")
                   .seasonNum(14)
                   .episodeNum(4)
                   .episodeTitle("Love Boat")
                   .build());
    }


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
    private Show testQueryShow(final EpisodeTestData testInput, final String queryString) {
        try {
            final CompletableFuture<Show> futureShow = new CompletableFuture<>();
            ShowStore.getShow(queryString, new ShowInformationListener() {
                    @Override
                    public void downloaded(Show show) {
                        futureShow.complete(show);
                    }

                    @Override
                    public void downloadFailed(Show show) {
                        futureShow.complete(show);
                    }
                });
            Show gotShow = futureShow.get(4, TimeUnit.SECONDS);
            if (gotShow == null) {
                fail("could not parse show name input " + queryString);
                return null;
            }
            assertFalse(gotShow instanceof LocalShow);
            assertEquals(testInput.properShowName, gotShow.getName());
            return gotShow;
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

    @Test
    public void testGetEpisodeTitle() {
        for (EpisodeTestData testInput : values) {
            if (testInput.episodeTitle != null) {
                final String queryString = testInput.queryString;
                final int seasonNum = testInput.seasonNum;
                final int episodeNum = testInput.episodeNum;
                try {
                    final Show show = testQueryShow(testInput, queryString);
                    final CompletableFuture<String> future = new CompletableFuture<>();
                    show.addListingsListener(new ShowListingsListener() {
                        @Override
                        public void listingsDownloadComplete() {
                            Episode ep = show.getEpisode(seasonNum, episodeNum);
                            if (ep == null) {
                                future.complete("null episode");
                            } else {
                                String title = ep.getTitle();
                                future.complete(title);
                            }
                        }

                        @Override
                        public void listingsDownloadFailed(Exception err) {
                            future.complete("downloadFailed");
                        }
                    });

                    String got = future.get(30, TimeUnit.SECONDS);
                    assertEpisodeTitle(testInput, got);
                } catch (TimeoutException e) {
                    String failMsg = "timeout trying to query for " + queryString
                        + ", season " + seasonNum + ", episode " + episodeNum;
                    String exceptionMessage = e.getMessage();
                    if (exceptionMessage != null) {
                        failMsg += exceptionMessage;
                    } else {
                        failMsg += "(no message)";
                    }
                    fail(failMsg);
                } catch (Exception e) {
                    String failMsg = "failure trying to query for " + queryString
                        + ", season " + seasonNum + ", episode " + episodeNum
                        + e.getClass().getName() + " ";
                    String exceptionMessage = e.getMessage();
                    if (exceptionMessage != null) {
                        failMsg += exceptionMessage;
                    } else {
                        failMsg += "(possibly timeout?)";
                    }
                    fail(failMsg);
                }
            }
        }
    }
}
