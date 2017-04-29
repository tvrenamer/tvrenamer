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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.Episode;
import org.tvrenamer.model.Show;
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
        final String showName = "Quintuplets";
        final String showId = "73732";
        final String ep2Name = "Quintagious";

        List<Show> options = TheTVDBProvider.getShowOptions(showName);
        assertNotNull(options);
        assertNotEquals(0, options.size());
        Show best = options.get(0);
        assertNotNull(best);
        assertEquals(showId, best.getId());
        assertEquals(showName, best.getName());

        TheTVDBProvider.getShowListing(best);

        best.preferProductionOrdering();
        Episode s1e02 = best.getEpisode(1, 2);
        assertNotNull("result of calling getEpisode(1, 2) on " + showName + " came back null",
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
        final String showName = "Firefly";
        final String showId = "78874";
        final String dvdName = "The Train Job";
        final String productionName = "Bushwhacked";

        List<Show> options = TheTVDBProvider.getShowOptions(showName);
        assertNotNull(options);
        assertNotEquals(0, options.size());
        Show best = options.get(0);
        assertNotNull(best);
        assertEquals(showId, best.getId());
        assertEquals(showName, best.getName());

        best.preferDvdOrdering();
        TheTVDBProvider.getShowListing(best);

        Episode s01e02 = null;

        best.preferHeuristicOrdering();
        s01e02 = best.getEpisode(1, 2);
        assertNotNull("result of calling getEpisode(1, 2) on " + showName
                      + "with heuristic ordering came back null", s01e02);
        assertEquals(dvdName, s01e02.getTitle());

        best.preferProductionOrdering();
        s01e02 = best.getEpisode(1, 2);
        assertNotNull("result of calling getEpisode(1, 2) on " + showName
                      + " with production ordering came back null", s01e02);
        assertEquals(productionName, s01e02.getTitle());

        best.preferDvdOrdering();
        s01e02 = best.getEpisode(1, 2);
        assertNotNull("result of calling getEpisode(1, 2) on " + showName
                      + " with DVD ordering came back null", s01e02);
        assertEquals(dvdName, s01e02.getTitle());
    }

    private static class TestInput {
        public final String show;
        public final String season;
        public final String episode;
        public final String episodeTitle;

        public TestInput(String show, String season, String episode, String episodeTitle) {
            this.show = show.toLowerCase();
            this.season = season;
            this.episode = episode;
            this.episodeTitle = episodeTitle;
        }
    }

    public static final List<TestInput> values = new LinkedList<>();

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
    public static void setupValues() {
        values.add(new TestInput("game of thrones", "5", "1", "The Wars to Come"));
        values.add(new TestInput("24", "8", "1", "Day 8: 4:00 P.M. - 5:00 P.M."));
        values.add(new TestInput("24", "7", "18", "Day 7: 1:00 A.M. - 2:00 A.M."));
        values.add(new TestInput("human target 2010", "1", "2", "Rewind"));
        values.add(new TestInput("dexter", "4", "7", "Slack Tide"));
        values.add(new TestInput("jag", "10", "1", "Hail and Farewell, Part II (2)"));
        values.add(new TestInput("lost", "6", "5", "Lighthouse"));
        values.add(new TestInput("warehouse 13", "1", "1", "Pilot"));
        values.add(new TestInput("one tree hill", "7", "14", "Family Affair"));
        values.add(new TestInput("gossip girl", "3", "15", "The Sixteen Year Old Virgin"));
        values.add(new TestInput("smallville", "9", "14", "Conspiracy"));
        values.add(new TestInput("smallville", "9", "15", "Escape"));
        values.add(new TestInput("the big bang theory", "3", "18", "The Pants Alternative"));
        values.add(new TestInput("castle 2009", "1", "9", "Little Girl Lost"));
        values.add(new TestInput("dexter", "5", "5", "First Blood"));
        values.add(new TestInput("lost", "2", "7", "The Other 48 Days"));
        values.add(new TestInput("american dad", "9", "17",
                                 "The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith"));
        values.add(new TestInput("californication", "7", "4", "Dicks"));
        values.add(new TestInput("continuum", "3", "7", "Waning Minutes"));
        values.add(new TestInput("elementary", "2", "23", "Art in the Blood"));
        values.add(new TestInput("family guy", "12", "19", "Meg Stinks!"));
        values.add(new TestInput("fargo", "1", "1", "The Crocodile's Dilemma"));
        values.add(new TestInput("girls", "3", "11", "I Saw You"));
        values.add(new TestInput("grimm", "3", "19", "Nobody Knows the Trubel I've Seen"));
        values.add(new TestInput("house of cards 2013", "1", "6", "Chapter 6"));
        values.add(new TestInput("modern family", "5", "12", "Under Pressure"));
        values.add(new TestInput("new girl", "3", "23", "Cruise"));
        values.add(new TestInput("nurse jackie", "6", "4", "Jungle Love"));
        values.add(new TestInput("offspring", "5", "1", "Back in the Game"));
        values.add(new TestInput("reign 2013", "1", "20", "Higher Ground"));
        values.add(new TestInput("robot chicken", "7", "4", "Rebel Appliance"));
        values.add(new TestInput("supernatural", "9", "21", "King of the Damned"));
        values.add(new TestInput("the americans 2013", "2", "10", "Yousaf"));
        values.add(new TestInput("the big bang theory", "7", "23", "The Gorilla Dissolution"));
        values.add(new TestInput("the good wife", "5", "20", "The Deep Web"));
        values.add(new TestInput("the walking dead", "4", "16", "A"));
        values.add(new TestInput("veep", "3", "5", "Fishing"));
        values.add(new TestInput("witches of east end", "1", "1", "Pilot"));
        values.add(new TestInput("warehouse 13", "5", "4", "Savage Seduction"));
        values.add(new TestInput("the 100", "2", "8", "Spacewalker"));
        values.add(new TestInput("firefly", "1", "1", "Serenity"));
        values.add(new TestInput("firefly", "1", "2", "The Train Job"));
        values.add(new TestInput("firefly", "1", "3", "Bushwhacked"));
        values.add(new TestInput("firefly", "1", "4", "Shindig"));
        values.add(new TestInput("firefly", "1", "5", "Safe"));
        values.add(new TestInput("firefly", "1", "6", "Our Mrs. Reynolds"));
        values.add(new TestInput("firefly", "1", "7", "Jaynestown"));
        values.add(new TestInput("firefly", "1", "8", "Out of Gas"));
        values.add(new TestInput("firefly", "1", "9", "Ariel"));
        values.add(new TestInput("firefly", "1", "10", "War Stories"));
        values.add(new TestInput("firefly", "1", "11", "Trash"));
        values.add(new TestInput("firefly", "1", "12", "The Message"));
        values.add(new TestInput("firefly", "1", "13", "Heart of Gold"));
        values.add(new TestInput("firefly", "1", "14", "Objects in Space"));
        values.add(new TestInput("strike back", "1", "1", "Chris Ryan's Strike Back, Episode 1"));
        values.add(new TestInput("law and order svu", "17", "05", "Community Policing"));
        values.add(new TestInput("ncis", "13", "04", "Double Trouble"));
        values.add(new TestInput("marvels agents of shield", "3", "3", "A Wanted (Inhu)man"));
        values.add(new TestInput("marvels agents of shield", "3", "10", "Maveth"));
        values.add(new TestInput("nip tuck", "6", "1", "Don Hoberman"));
        values.add(new TestInput("the big bang theory", "10", "4", "The Cohabitation Experimentation"));
        values.add(new TestInput("lucifer", "2", "3", "Sin-Eater"));
        values.add(new TestInput("marvels agents of shield", "4", "3", "Uprising"));
        values.add(new TestInput("supernatural", "11", "22", "We Happy Few"));
        values.add(new TestInput("channel zero", "1", "1", "You Have to Go Inside"));
        values.add(new TestInput("ncis", "14", "4", "Love Boat"));
    }

    // @Test
    public void testGetEpisodeTitle() {
        for (TestInput testInput : values) {
            if (testInput.episodeTitle != null) {
                final String showName = testInput.show;
                final String season = testInput.season;
                final String episode = testInput.episode;
                try {
                    final int seasonNum = Integer.parseInt(season);
                    final int episodeNum = Integer.parseInt(episode);
                    final CompletableFuture<String> future = new CompletableFuture<>();
                    ShowStore.getShow(showName, new ShowInformationListener() {
                        @Override
                        public void downloaded(Show show) {
                            future.complete(show.getEpisode(seasonNum, episodeNum).getTitle());
                        }

                        @Override
                        public void downloadFailed(Show show) {
                            future.complete(null);
                        }
                    });

                    String got = future.get(15, TimeUnit.SECONDS);
                    assertEquals(testInput.episodeTitle, got);
                } catch (TimeoutException e) {
                    String failMsg = "timeout trying to query for " + showName
                        + ", season " + season + ", episode " + episode;
                    String exceptionMessage = e.getMessage();
                    if (exceptionMessage != null) {
                        failMsg += exceptionMessage;
                    } else {
                        failMsg += "(no message)";
                    }
                    fail(failMsg);
                } catch (Exception e) {
                    String failMsg = "failure trying to query for " + showName
                        + ", season " + season + ", episode " + episode
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
