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
        final String actualName = "Quintuplets";
        final int showId = 73732;
        final String ep2Name = "Quintagious";

        final ShowName showName = ShowName.lookupShowName(actualName);
        try {
            TheTVDBProvider.getShowOptions(showName);
        } catch (Exception e) {
            fail("exception getting show options for " + actualName);
        }
        assertTrue(showName.hasShowOptions());
        Show best = showName.selectShowOption();
        assertNotNull(best);
        assertFalse(best.isLocalShow());
        assertEquals(showId, best.getId());
        assertEquals(actualName, best.getName());

        best.clearEpisodes();
        TheTVDBProvider.getShowListing(best);

        Episode s1e02 = best.getEpisode(1, 2);
        assertNotNull("result of calling getEpisode(1, 2) on " + actualName + " came back null",
                      s1e02);
        assertEquals(ep2Name, s1e02.getTitle());

        // This is probably too likely to change.
        // assertEquals(1, options.size());
    }

    /**
     * Second download test.  This one is specifically chosen to ensure we
     * get the right preferences between "DVD number" and "regular number".
     */
    @Test
    public void testRegularEpisodePreference() throws Exception {
        final String actualName = "Firefly";
        final int showId = 78874;
        final String dvdName = "The Train Job";

        final ShowName showName = ShowName.lookupShowName(actualName);
        try {
            TheTVDBProvider.getShowOptions(showName);
        } catch (Exception e) {
            fail("exception getting show options for " + actualName);
        }
        assertTrue(showName.hasShowOptions());
        Show best = showName.selectShowOption();
        assertNotNull(best);
        assertFalse(best.isLocalShow());
        assertEquals(showId, best.getId());
        assertEquals(actualName, best.getName());

        best.clearEpisodes();
        TheTVDBProvider.getShowListing(best);

        Episode s01e02 = null;

        s01e02 = best.getEpisode(1, 2);
        assertNotNull("result of calling getEpisode(1, 2) on " + actualName
                      + "with heuristic ordering came back null", s01e02);
        assertEquals(dvdName, s01e02.getTitle());
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
                   .seasonNum(5)
                   .episodeNum(1)
                   .episodeTitle("The Wars to Come")
                   .build());
    }

    @BeforeClass
    public static void setupValues02() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("24")
                   .seasonNum(8)
                   .episodeNum(1)
                   .episodeTitle("Day 8: 4:00 P.M. - 5:00 P.M.")
                   .build());
    }

    @BeforeClass
    public static void setupValues03() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("24")
                   .seasonNum(7)
                   .episodeNum(18)
                   .episodeTitle("Day 7: 1:00 A.M. - 2:00 A.M.")
                   .build());
    }

    @BeforeClass
    public static void setupValues04() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("human target 2010")
                   .seasonNum(1)
                   .episodeNum(2)
                   .episodeTitle("Rewind")
                   .build());
    }

    @BeforeClass
    public static void setupValues05() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("dexter")
                   .seasonNum(4)
                   .episodeNum(7)
                   .episodeTitle("Slack Tide")
                   .build());
    }

    @BeforeClass
    public static void setupValues06() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("jag")
                   .seasonNum(10)
                   .episodeNum(1)
                   .episodeTitle("Hail and Farewell, Part II (2)")
                   .build());
    }

    @BeforeClass
    public static void setupValues07() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("lost")
                   .seasonNum(6)
                   .episodeNum(5)
                   .episodeTitle("Lighthouse")
                   .build());
    }

    @BeforeClass
    public static void setupValues08() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("warehouse 13")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Pilot")
                   .build());
    }

    @BeforeClass
    public static void setupValues09() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("one tree hill")
                   .seasonNum(7)
                   .episodeNum(14)
                   .episodeTitle("Family Affair")
                   .build());
    }

    @BeforeClass
    public static void setupValues10() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("gossip girl")
                   .seasonNum(3)
                   .episodeNum(15)
                   .episodeTitle("The Sixteen Year Old Virgin")
                   .build());
    }

    @BeforeClass
    public static void setupValues11() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("smallville")
                   .seasonNum(9)
                   .episodeNum(14)
                   .episodeTitle("Conspiracy")
                   .build());
    }

    @BeforeClass
    public static void setupValues12() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("smallville")
                   .seasonNum(9)
                   .episodeNum(15)
                   .episodeTitle("Escape")
                   .build());
    }

    @BeforeClass
    public static void setupValues13() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the big bang theory")
                   .seasonNum(3)
                   .episodeNum(18)
                   .episodeTitle("The Pants Alternative")
                   .build());
    }

    @BeforeClass
    public static void setupValues14() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("castle 2009")
                   .seasonNum(1)
                   .episodeNum(9)
                   .episodeTitle("Little Girl Lost")
                   .build());
    }

    @BeforeClass
    public static void setupValues15() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("dexter")
                   .seasonNum(5)
                   .episodeNum(5)
                   .episodeTitle("First Blood")
                   .build());
    }

    @BeforeClass
    public static void setupValues16() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("lost")
                   .seasonNum(2)
                   .episodeNum(7)
                   .episodeTitle("The Other 48 Days")
                   .build());
    }

    @BeforeClass
    public static void setupValues17() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("american dad")
                   .seasonNum(9)
                   .episodeNum(17)
                   .episodeTitle("The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith")
                   .build());
    }

    @BeforeClass
    public static void setupValues18() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("californication")
                   .seasonNum(7)
                   .episodeNum(4)
                   .episodeTitle("Dicks")
                   .build());
    }

    @BeforeClass
    public static void setupValues19() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("continuum")
                   .seasonNum(3)
                   .episodeNum(7)
                   .episodeTitle("Waning Minutes")
                   .build());
    }

    @BeforeClass
    public static void setupValues20() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("elementary")
                   .seasonNum(2)
                   .episodeNum(23)
                   .episodeTitle("Art in the Blood")
                   .build());
    }

    @BeforeClass
    public static void setupValues21() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("family guy")
                   .seasonNum(12)
                   .episodeNum(19)
                   .episodeTitle("Meg Stinks!")
                   .build());
    }

    @BeforeClass
    public static void setupValues22() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("fargo")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("The Crocodile's Dilemma")
                   .build());
    }

    @BeforeClass
    public static void setupValues23() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("girls")
                   .seasonNum(3)
                   .episodeNum(11)
                   .episodeTitle("I Saw You")
                   .build());
    }

    @BeforeClass
    public static void setupValues24() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("grimm")
                   .seasonNum(3)
                   .episodeNum(19)
                   .episodeTitle("Nobody Knows the Trubel I've Seen")
                   .build());
    }

    @BeforeClass
    public static void setupValues25() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("house of cards 2013")
                   .seasonNum(1)
                   .episodeNum(6)
                   .episodeTitle("Chapter 6")
                   .build());
    }

    @BeforeClass
    public static void setupValues26() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("modern family")
                   .seasonNum(5)
                   .episodeNum(12)
                   .episodeTitle("Under Pressure")
                   .build());
    }

    @BeforeClass
    public static void setupValues27() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("new girl")
                   .seasonNum(3)
                   .episodeNum(23)
                   .episodeTitle("Cruise")
                   .build());
    }

    @BeforeClass
    public static void setupValues28() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("nurse jackie")
                   .seasonNum(6)
                   .episodeNum(4)
                   .episodeTitle("Jungle Love")
                   .build());
    }

    @BeforeClass
    public static void setupValues29() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("offspring")
                   .seasonNum(5)
                   .episodeNum(1)
                   .episodeTitle("Back in the Game")
                   .build());
    }

    @BeforeClass
    public static void setupValues30() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("reign 2013")
                   .seasonNum(1)
                   .episodeNum(20)
                   .episodeTitle("Higher Ground")
                   .build());
    }

    @BeforeClass
    public static void setupValues31() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("robot chicken")
                   .seasonNum(7)
                   .episodeNum(4)
                   .episodeTitle("Rebel Appliance")
                   .build());
    }

    @BeforeClass
    public static void setupValues32() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("supernatural")
                   .seasonNum(9)
                   .episodeNum(21)
                   .episodeTitle("King of the Damned")
                   .build());
    }

    @BeforeClass
    public static void setupValues33() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the americans 2013")
                   .seasonNum(2)
                   .episodeNum(10)
                   .episodeTitle("Yousaf")
                   .build());
    }

    @BeforeClass
    public static void setupValues34() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the big bang theory")
                   .seasonNum(7)
                   .episodeNum(23)
                   .episodeTitle("The Gorilla Dissolution")
                   .build());
    }

    @BeforeClass
    public static void setupValues35() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the good wife")
                   .seasonNum(5)
                   .episodeNum(20)
                   .episodeTitle("The Deep Web")
                   .build());
    }

    @BeforeClass
    public static void setupValues36() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the walking dead")
                   .seasonNum(4)
                   .episodeNum(16)
                   .episodeTitle("A")
                   .build());
    }

    @BeforeClass
    public static void setupValues37() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("veep")
                   .seasonNum(3)
                   .episodeNum(5)
                   .episodeTitle("Fishing")
                   .build());
    }

    @BeforeClass
    public static void setupValues38() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("witches of east end")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Pilot")
                   .build());
    }

    @BeforeClass
    public static void setupValues39() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("warehouse 13")
                   .seasonNum(5)
                   .episodeNum(4)
                   .episodeTitle("Savage Seduction")
                   .build());
    }

    @BeforeClass
    public static void setupValues40() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the 100")
                   .seasonNum(2)
                   .episodeNum(8)
                   .episodeTitle("Spacewalker")
                   .build());
    }

    @BeforeClass
    public static void setupValues41() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Serenity")
                   .build());
    }

    @BeforeClass
    public static void setupValues42() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(2)
                   .episodeTitle("The Train Job")
                   .build());
    }

    @BeforeClass
    public static void setupValues43() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(3)
                   .episodeTitle("Bushwhacked")
                   .build());
    }

    @BeforeClass
    public static void setupValues44() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(4)
                   .episodeTitle("Shindig")
                   .build());
    }

    @BeforeClass
    public static void setupValues45() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(5)
                   .episodeTitle("Safe")
                   .build());
    }

    @BeforeClass
    public static void setupValues46() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(6)
                   .episodeTitle("Our Mrs. Reynolds")
                   .build());
    }

    @BeforeClass
    public static void setupValues47() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(7)
                   .episodeTitle("Jaynestown")
                   .build());
    }

    @BeforeClass
    public static void setupValues48() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(8)
                   .episodeTitle("Out of Gas")
                   .build());
    }

    @BeforeClass
    public static void setupValues49() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(9)
                   .episodeTitle("Ariel")
                   .build());
    }

    @BeforeClass
    public static void setupValues50() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(10)
                   .episodeTitle("War Stories")
                   .build());
    }

    @BeforeClass
    public static void setupValues51() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(11)
                   .episodeTitle("Trash")
                   .build());
    }

    @BeforeClass
    public static void setupValues52() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(12)
                   .episodeTitle("The Message")
                   .build());
    }

    @BeforeClass
    public static void setupValues53() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(13)
                   .episodeTitle("Heart of Gold")
                   .build());
    }

    @BeforeClass
    public static void setupValues54() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("firefly")
                   .seasonNum(1)
                   .episodeNum(14)
                   .episodeTitle("Objects in Space")
                   .build());
    }

    @BeforeClass
    public static void setupValues55() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("strike back")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("Chris Ryan's Strike Back, Episode 1")
                   .build());
    }

    @BeforeClass
    public static void setupValues56() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("law and order svu")
                   .seasonNum(17)
                   .episodeNum(5)
                   .episodeTitle("Community Policing")
                   .build());
    }

    @BeforeClass
    public static void setupValues57() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("ncis")
                   .seasonNum(13)
                   .episodeNum(4)
                   .episodeTitle("Double Trouble")
                   .build());
    }

    @BeforeClass
    public static void setupValues58() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("marvels agents of shield")
                   .seasonNum(3)
                   .episodeNum(3)
                   .episodeTitle("A Wanted (Inhu)man")
                   .build());
    }

    @BeforeClass
    public static void setupValues59() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("marvels agents of shield")
                   .seasonNum(3)
                   .episodeNum(10)
                   .episodeTitle("Maveth")
                   .build());
    }

    @BeforeClass
    public static void setupValues60() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("nip tuck")
                   .seasonNum(6)
                   .episodeNum(1)
                   .episodeTitle("Don Hoberman")
                   .build());
    }

    @BeforeClass
    public static void setupValues61() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("the big bang theory")
                   .seasonNum(10)
                   .episodeNum(4)
                   .episodeTitle("The Cohabitation Experimentation")
                   .build());
    }

    @BeforeClass
    public static void setupValues62() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("lucifer")
                   .seasonNum(2)
                   .episodeNum(3)
                   .episodeTitle("Sin-Eater")
                   .build());
    }

    @BeforeClass
    public static void setupValues63() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("marvels agents of shield")
                   .seasonNum(4)
                   .episodeNum(3)
                   .episodeTitle("Uprising")
                   .build());
    }

    @BeforeClass
    public static void setupValues64() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("supernatural")
                   .seasonNum(11)
                   .episodeNum(22)
                   .episodeTitle("We Happy Few")
                   .build());
    }

    @BeforeClass
    public static void setupValues65() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("channel zero")
                   .seasonNum(1)
                   .episodeNum(1)
                   .episodeTitle("You Have to Go Inside")
                   .build());
    }

    @BeforeClass
    public static void setupValues66() {
        values.add(new EpisodeTestData.Builder()
                   .queryString("ncis")
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
            assertFalse(gotShow.isLocalShow());
            // assertEquals(testInput.actualShowName, gotShow.getName());
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

    // @Test
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

                    String got = future.get(15, TimeUnit.SECONDS);
                    assertEquals(testInput.episodeTitle, got);
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
