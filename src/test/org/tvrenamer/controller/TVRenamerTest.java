package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.FileEpisode;

import java.util.LinkedList;
import java.util.List;

public class TVRenamerTest {
    public static final List<TestInput> values = new LinkedList<>();

    @BeforeClass
    public static void setupValues() {
        values.add(new TestInput("game.of.thrones.5x01.mp4", "game.of.thrones.", "5", "1", ""));
        values.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24.", "8", "1", "720p"));
        values.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24.", "7", "18", "720p"));
        values.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "human.target.2010.",
                                 "1", "2", "720p"));
        values.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "dexter.", "4", "7", "720p"));
        values.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "JAG.", "10", "1", ""));
        values.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "Lost.", "6", "5",
                                 "720p"));
        values.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "warehouse.13.", "1",
                                 "1", "720p"));
        values.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "one.tree.hill.", "7", "14", ""));
        values.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "gossip.girl.", "3", "15", ""));
        values.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "smallville.", "9", "14", ""));
        values.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "smallville.", "9", "15", ""));
        values.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv", "the.big.bang.theory.",
                                 "3", "18", "720p"));
        values.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv", "castle.2009.", "1", "9",
                                 "720p"));
        values.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "Dexter ", "5", "5", ""));
        values.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "Lost [", "2", "7", ""));
        values.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4", "American.Dad.", "9", "17", ""));
        values.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "Californication.", "7", "4",
                                 ""));
        values.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "Continuum.", "3", "7", ""));
        values.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "Elementary.", "2", "23", ""));
        values.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "Family.Guy.", "12", "19", ""));
        values.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "Fargo.", "1", "1", ""));
        values.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "Girls.", "3", "11", ""));
        values.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "Grimm.", "3", "19", ""));
        values.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4", "House.Of.Cards.2013.",
                                 "1", "6", ""));
        values.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4", "Modern.Family.", "5",
                                 "12", ""));
        values.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "New.Girl.", "3", "23", ""));
        values.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "Nurse.Jackie.", "6", "4", ""));
        values.add(new TestInput("Offspring - S05E01.mp4", "Offspring - ", "5", "1", ""));
        values.add(new TestInput("Reign.2013.S01E20.HDTV.x264-2HD.mp4", "Reign.2013.", "1", "20", ""));
        values.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4", "Robot.Chicken.", "7", "4",
                                 ""));
        values.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "Supernatural.", "9", "21", ""));
        values.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4", "The.Americans.2013.", "2",
                                 "10", ""));
        values.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4", "The.Big.Bang.Theory.", "7",
                                 "23", ""));
        values.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "The.Good.Wife.", "5", "20", ""));
        values.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4", "The.Walking.Dead.",
                                 "4", "16", ""));
        values.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "Veep.", "3", "5", ""));
        values.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4",
                                 "Witches.of.East.End.", "1", "1", ""));
        values.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "Warehouse.13.", "5", "4", ""));
        values.add(new TestInput("the.100.208.hdtv-lol.mp4", "the.100.", "2", "8", ""));
        values.add(new TestInput("firefly.1x01.hdtv-lol.mp4", "firefly.", "1", "1", ""));
        values.add(new TestInput("firefly.1x02.hdtv-lol.mp4", "firefly.", "1", "2", ""));
        values.add(new TestInput("firefly.1x03.hdtv-lol.mp4", "firefly.", "1", "3", ""));
        values.add(new TestInput("firefly.1x04.hdtv-lol.mp4", "firefly.", "1", "4", ""));
        values.add(new TestInput("firefly.1x05.hdtv-lol.mp4", "firefly.", "1", "5", ""));
        values.add(new TestInput("firefly.1x06.hdtv-lol.mp4", "firefly.", "1", "6", ""));
        values.add(new TestInput("firefly.1x07.hdtv-lol.mp4", "firefly.", "1", "7", ""));
        values.add(new TestInput("firefly.1x08.hdtv-lol.mp4", "firefly.", "1", "8", ""));
        values.add(new TestInput("firefly.1x09.hdtv-lol.mp4", "firefly.", "1", "9", ""));
        values.add(new TestInput("firefly.1x10.hdtv-lol.mp4", "firefly.", "1", "10", ""));
        values.add(new TestInput("firefly.1x11.hdtv-lol.mp4", "firefly.", "1", "11", ""));
        values.add(new TestInput("firefly.1x12.hdtv-lol.mp4", "firefly.", "1", "12", ""));
        values.add(new TestInput("firefly.1x13.hdtv-lol.mp4", "firefly.", "1", "13", ""));
        values.add(new TestInput("firefly.1x14.hdtv-lol.mp4", "firefly.", "1", "14", ""));
        values.add(new TestInput("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv", "Strike.Back.", "1",
                                 "1", "720p"));
        values.add(new TestInput("law.and.order.svu.1705.hdtv-lol", "law.and.order.svu.", "17", "05", ""));
        values.add(new TestInput("ncis.1304.hdtv-lol", "ncis.", "13", "04", ""));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET",
                                 "Marvels.Agents.of.S.H.I.E.L.D.", "3", "3", ""));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS",
                                 "Marvels.Agents.of.S.H.I.E.L.D.", "3", "10", ""));
        values.add(new TestInput("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv", "Nip.Tuck.", "6", "1",
                                 "720p"));
        values.add(new TestInput("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv",
                                 "The.Big.Bang.Theory.", "10", "4", "720p"));
        values.add(new TestInput("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv", "Lucifer.", "2", "3",
                                 "720p"));
        values.add(new TestInput("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv",
                                 "Marvels.Agents.of.S.H.I.E.L.D.", "4", "3", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv", "Supernatural.",
                                 "11", "22", "1080p"));
        values.add(new TestInput("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv", "Supernatural.",
                                 "11", "22", "720p"));
        values.add(new TestInput("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv", "Channel.Zero.",
                                 "1", "1", "480p"));
        values.add(new TestInput("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv", "NCIS.", "14", "4",
                                 "720p"));
    }

    @Test
    public void testRemoveLast() {
        // Straighforward removal; note does not remove punctuation/separators
        assertEquals("foo..baz", TVRenamer.removeLast("foo.bar.baz", "bar"));

        // Implementation detail, but the match is required to be all lower-case,
        // while the input doesn't
        assertEquals("Foo..Baz", TVRenamer.removeLast("Foo.Bar.Baz", "bar"));

        // Like the name says, the method only removes the last instance
        assertEquals("bar.foo..baz", TVRenamer.removeLast("bar.foo.bar.baz", "bar"));

        // Doesn't have to be delimited
        assertEquals("emassment", TVRenamer.removeLast("embarassment", "bar"));

        // Doesn't necessarily replace anything
        assertEquals("Foo.Schmar.baz", TVRenamer.removeLast("Foo.Schmar.baz", "bar"));

        // This frankly is probably a bug, but this is currently the expected behavior.
        // If the match is not all lower-case to begin with, nothing will be matched.
        assertEquals("Foo.Bar.Baz", TVRenamer.removeLast("Foo.Bar.Baz", "Bar"));
    }

    @Test
    public void testParseFileName() {
        for (TestInput testInput : values) {
            FileEpisode retval = new FileEpisode(testInput.input);
            assertTrue(TVRenamer.parseFilename(retval));
            assertEquals(testInput.input, testInput.showName, retval.getFilenameShow());
            assertEquals(testInput.input, Integer.parseInt(testInput.season), retval.getSeasonNum());
            assertEquals(testInput.input, Integer.parseInt(testInput.episode), retval.getEpisodeNum());
            assertEquals(testInput.input, testInput.episodeResolution, retval.getFilenameResolution());
        }
    }

    @Test
    public void testWarehouse13() {
        FileEpisode episode = new FileEpisode("Warehouse.13.S05E04.HDTV.x264-2HD.mp4");
        assertTrue(TVRenamer.parseFilename(episode));
        assertEquals("Warehouse.13.", episode.getFilenameShow());
        assertEquals(5, episode.getSeasonNum());
        assertEquals(4, episode.getEpisodeNum());
    }

    private static class TestInput {
        public final String input;
        public final String showName;
        public final String season;
        public final String episode;
        public final String episodeResolution;

        public TestInput(String input, String showName, String season, String episode,
                         String episodeResolution)
        {
            this.input = input;
            this.showName = showName;
            this.season = season;
            this.episode = episode;
            this.episodeResolution = episodeResolution;
        }
    }
}
