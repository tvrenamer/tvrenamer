package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TheTVDBProviderTest {

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

    @Test
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
                            future.complete(show.getSeason(seasonNum).getTitle(episodeNum));
                        }

                        @Override
                        public void downloadFailed(Show show) {
                            future.complete(null);
                        }
                    });

                    String got = future.get(15, TimeUnit.SECONDS);
                    assertEquals(testInput.episodeTitle, got);
                } catch (Exception e) {
                    String failMsg = "failure trying to query for " + showName
                        + ", season " + season + ", " + episode
                        + e.getClass().getName() + " ";
                    String exceptionMessage = e.getMessage();
                    if (exceptionMessage != null) {
                        failMsg += exceptionMessage;
                    } else {
                        failMsg += "(likely timeout)";
                    }
                    fail(failMsg);
                }
            }
        }
    }

    @Test
    public void testGetShowOptions() throws Exception {
        for (Show show : TheTVDBProvider.getShowOptions("Gossip Girl")) {
            assertNotNull(show);
            assertNotEquals(0, show.getId().length());
            assertNotEquals(0, show.getName().length());
        }
    }

    @Test
    public void testGetShowListing() throws Exception {
        TheTVDBProvider.getShowListing(new Show("80547", "Gossip Girl", ""));
    }
}
