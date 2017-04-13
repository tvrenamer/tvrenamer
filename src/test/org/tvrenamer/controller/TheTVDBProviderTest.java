package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowStore;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TheTVDBProviderTest {

    private static class TestInput {
        public final String input;
        public final String show;
        public final String season;
        public final String episode;

        public final String episodeTitle;
        public final String episodeResolution;

        public TestInput(String input, String show, String season, String episode, String episodeTitle,
                         String episodeResolution)
        {
            this.input = input;
            this.show = show.toLowerCase();
            this.season = season;
            this.episode = episode;
            this.episodeTitle = episodeTitle;
            this.episodeResolution = episodeResolution;
        }
    }


    public static final List<TestInput> values = new LinkedList<>();

    @BeforeClass
    public static void setupValues() {
        values.add(new TestInput("game.of.thrones.5x01.mp4", "game of thrones", "5", "1",
                                 "The Wars to Come", ""));
        values.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24", "8", "1",
                                 "Day 8: 4:00 P.M. - 5:00 P.M.", "720p"));
        values.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24", "7", "18",
                                 "Day 7: 1:00 A.M. - 2:00 A.M.", "720p"));
        values.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "human target 2010",
                                 "1", "2", "Rewind", "720p"));
        values.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "dexter", "4", "7", "Slack Tide",
                                 "720p"));
        values.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "jag", "10", "1",
                                 "Hail and Farewell, Part II (2)", ""));
        values.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "lost", "6",
                                 "5", "Lighthouse", "720p"));
        values.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "warehouse 13", "1",
                                 "1", "Pilot", "720p"));
        values.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "one tree hill", "7", "14",
                                 "Family Affair", ""));
        values.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "gossip girl", "3", "15",
                                 "The Sixteen Year Old Virgin", ""));
        values.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "smallville", "9", "14",
                                 "Conspiracy", ""));
        values.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "smallville", "9", "15", "Escape",
                                 ""));
        values.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv",
                                 "the big bang theory", "3", "18", "The Pants Alternative", "720p"));
        values.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv", "castle 2009", "1", "9",
                                 "Little Girl Lost", "720p"));
        values.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "dexter", "5", "5", "First Blood", ""));
        values.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "lost", "2", "7", "The Other 48 Days", ""));
        values.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4", "american dad", "9", "17",
                                 "The Full Cognitive Redaction of Avery Bullock by the Coward Stan Smith", ""));
        values.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "californication", "7", "4",
                                 "Dicks", ""));
        values.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "continuum", "3", "7",
                                 "Waning Minutes", ""));
        values.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "elementary", "2", "23",
                                 "Art in the Blood", ""));
        values.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "family guy", "12", "19",
                                 "Meg Stinks!", ""));
        values.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "fargo", "1", "1",
                                 "The Crocodile's Dilemma", ""));
        values.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "girls", "3", "11", "I Saw You", ""));
        values.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "grimm", "3", "19",
                                 "Nobody Knows the Trubel I've Seen", ""));
        values.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4", "house of cards 2013",
                                 "1", "6", "Chapter 6", ""));
        values.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4", "modern family", "5",
                                 "12", "Under Pressure", ""));
        values.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "new girl", "3", "23", "Cruise", ""));
        values.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "nurse jackie", "6", "4",
                                 "Jungle Love", ""));
        values.add(new TestInput("Offspring - S05E01.mp4", "offspring", "5", "1", "Back in the Game", ""));
        values.add(new TestInput("Reign.2013.S01E20.HDTV.x264-2HD.mp4", "reign 2013", "1", "20",
                                 "Higher Ground", ""));
        values.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4", "robot chicken", "7",
                                 "4", "Rebel Appliance", ""));
        values.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "supernatural", "9", "21",
                                 "King of the Damned", ""));
        values.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4", "the americans 2013", "2",
                                 "10", "Yousaf", ""));
        values.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4", "the big bang theory",
                                 "7", "23", "The Gorilla Dissolution", ""));
        values.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "the good wife", "5", "20",
                                 "The Deep Web", ""));
        values.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4", "the walking dead",
                                 "4", "16", "A", ""));
        values.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "veep", "3", "5", "Fishing", ""));
        values.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4",
                                 "witches of east end", "1", "1", "Pilot", ""));
        values.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "warehouse 13", "5", "4",
                                 "Savage Seduction", ""));
        values.add(new TestInput("the.100.208.hdtv-lol.mp4", "the 100", "2", "8", "Spacewalker", ""));
        values.add(new TestInput("firefly.1x01.hdtv-lol.mp4", "firefly", "1", "1", "Serenity", ""));
        values.add(new TestInput("firefly.1x02.hdtv-lol.mp4", "firefly", "1", "2", "The Train Job", ""));
        values.add(new TestInput("firefly.1x03.hdtv-lol.mp4", "firefly", "1", "3", "Bushwhacked", ""));
        values.add(new TestInput("firefly.1x04.hdtv-lol.mp4", "firefly", "1", "4", "Shindig", ""));
        values.add(new TestInput("firefly.1x05.hdtv-lol.mp4", "firefly", "1", "5", "Safe", ""));
        values.add(new TestInput("firefly.1x06.hdtv-lol.mp4", "firefly", "1", "6", "Our Mrs. Reynolds", ""));
        values.add(new TestInput("firefly.1x07.hdtv-lol.mp4", "firefly", "1", "7", "Jaynestown", ""));
        values.add(new TestInput("firefly.1x08.hdtv-lol.mp4", "firefly", "1", "8", "Out of Gas", ""));
        values.add(new TestInput("firefly.1x09.hdtv-lol.mp4", "firefly", "1", "9", "Ariel", ""));
        values.add(new TestInput("firefly.1x10.hdtv-lol.mp4", "firefly", "1", "10", "War Stories", ""));
        values.add(new TestInput("firefly.1x11.hdtv-lol.mp4", "firefly", "1", "11", "Trash", ""));
        values.add(new TestInput("firefly.1x12.hdtv-lol.mp4", "firefly", "1", "12", "The Message", ""));
        values.add(new TestInput("firefly.1x13.hdtv-lol.mp4", "firefly", "1", "13", "Heart of Gold", ""));
        values.add(new TestInput("firefly.1x14.hdtv-lol.mp4", "firefly", "1", "14", "Objects in Space", ""));
        values.add(new TestInput("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv", "strike back", "1",
                                 "1", "Chris Ryan's Strike Back, Episode 1", "720p"));
        values.add(new TestInput("law.and.order.svu.1705.hdtv-lol", "law and order svu", "17", "05",
                                 "Community Policing", ""));
        values.add(new TestInput("ncis.1304.hdtv-lol", "ncis", "13", "04", "Double Trouble", ""));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET",
                                 "marvels agents of shield", "3", "3", "A Wanted (Inhu)man", ""));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS",
                                 "marvels agents of shield", "3", "10", "Maveth", ""));
        values.add(new TestInput("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv", "nip tuck", "6", "1",
                                 "Don Hoberman", "720p"));
        values.add(new TestInput("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "the big bang theory", "10", "4", "The Cohabitation Experimentation", "720p"));
        values.add(new TestInput("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv", "lucifer", "2", "3",
                                 "Sin-Eater", "720p"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv",
                                 "marvels agents of shield", "4", "3", "Uprising", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv", "supernatural",
                                 "11", "22", "We Happy Few", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv", "supernatural",
                                 "11", "22", "We Happy Few", "720p"));
        values.add(new TestInput("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv", "channel zero",
                                 "1", "1", "You Have to Go Inside", "480p"));
        values.add(new TestInput("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv", "ncis", "14", "4",
                                 "Love Boat", "720p"));
    }

    @Test
    public void testDownloadAndRename() {
        for (TestInput testInput : values) {
            if (testInput.episodeTitle != null) {
                final String filename = testInput.input;
                try {
                    final FileEpisode fileEpisode = TVRenamer.parseFilename(filename);
                    assertNotNull(fileEpisode);
                    String showName = fileEpisode.getShowName();

                    final CompletableFuture<String> future = new CompletableFuture<>();
                    ShowStore.getShow(showName, new ShowInformationListener() {
                        @Override
                        public void downloaded(Show show) {
                            future.complete(show.getSeason(fileEpisode.getSeasonNumber())
                                            .getTitle(fileEpisode.getEpisodeNumber()));
                        }

                        @Override
                        public void downloadFailed(Show show) {
                            future.complete(null);
                        }
                    });

                    String got = future.get(15, TimeUnit.SECONDS);
                    assertEquals(testInput.episodeTitle, got);
                } catch (Exception e) {
                    String failMsg = "failure trying to query for " + filename
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
