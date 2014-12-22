package com.google.code.tvrenamer;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.FileEpisode;

public class TVRenamerTest {
	public static final List<TestInput> values = new LinkedList<>();

	@BeforeClass
	public static void setupValues() {
		values.add(new TestInput("24.s08.e01.720p.hdtv.x264-immerse.mkv", "24", "8", "1"));
		values.add(new TestInput("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24", "7", "18"));
		values.add(new TestInput("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "human target 2010", "1", "2"));
		values.add(new TestInput("dexter.407.720p.hdtv.x264-sys.mkv", "dexter", "4", "7"));
		values.add(new TestInput("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "jag", "10", "1"));
		values.add(new TestInput("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "lost", "6", "5"));
		values.add(new TestInput("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "warehouse 13", "1", "1"));
		values.add(new TestInput("one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "one tree hill", "7", "14"));
		values.add(new TestInput("gossip.girl.s03e15.hdtv.xvid-fqm.avi", "gossip girl", "3", "15"));
		values.add(new TestInput("smallville.s09e14.hdtv.xvid-xii.avi", "smallville", "9", "14"));
		values.add(new TestInput("smallville.s09e15.hdtv.xvid-2hd.avi", "smallville", "9", "15"));
		values.add(new TestInput("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv", "the big bang theory", "3", "18"));
		values.add(new TestInput("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv", "castle 2009", "1", "9"));
		values.add(new TestInput("/TV/Dexter/S05E05 First Blood.mkv", "dexter", "5", "5"));
		values.add(new TestInput("/TV/Lost/Lost [2x07].mkv", "lost", "2", "7"));

		values.add(new TestInput("American.Dad.S09E17.HDTV.x264-2HD.mp4", "american dad", "9", "17"));
		values.add(new TestInput("Californication.S07E04.HDTV.x264-2HD.mp4", "californication", "7", "4"));
		values.add(new TestInput("Continuum.S03E07.HDTV.x264-2HD.mp4", "Continuum", "3", "7"));
		values.add(new TestInput("Elementary.S02E23.HDTV.x264-LOL.mp4", "Elementary", "2", "23"));
		values.add(new TestInput("Family.Guy.S12E19.HDTV.x264-2HD.mp4", "family guy", "12", "19"));
		values.add(new TestInput("Fargo.S01E01.HDTV.x264-2HD.mp4", "Fargo", "1", "1"));
		values.add(new TestInput("Girls.S03E11.HDTV.x264-KILLERS.mp4", "Girls", "3", "11"));
		values.add(new TestInput("Grimm.S03E19.HDTV.x264-LOL.mp4", "Grimm", "3", "19"));
		values.add(new TestInput("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4", "House Of Cards 2013", "1", "6"));
		values.add(new TestInput("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4", "Modern Family", "5", "12"));
		values.add(new TestInput("New.Girl.S03E23.HDTV.x264-LOL.mp4", "new girl", "3", "23"));
		values.add(new TestInput("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4", "Nurse Jackie", "6", "4"));
		values.add(new TestInput("Offspring - S05E01.mp4", "Offspring", "5", "1"));
		values.add(new TestInput("Reign.S01E20.HDTV.x264-2HD.mp4", "Reign", "1", "20"));
		values.add(new TestInput("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4", "Robot Chicken", "7", "4"));
		values.add(new TestInput("Supernatural.S09E21.HDTV.x264-LOL.mp4", "Supernatural", "9", "21"));
		values.add(new TestInput("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4", "The Americans 2013", "2", "10"));
		values.add(new TestInput("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4", "The Big Bang Theory", "7", "23"));
		values.add(new TestInput("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4", "The Good Wife", "5", "20"));
		values.add(new TestInput("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4", "The Walking Dead", "4", "16"));
		values.add(new TestInput("Veep.S03E05.HDTV.x264-KILLERS.mp4", "Veep", "3", "5"));
		values.add(new TestInput("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4", "Witches of East End", "1", "1"));
		values.add(new TestInput("Warehouse.13.S05E04.HDTV.x264-2HD.mp4", "Warehouse 13", "5", "4"));

		values.add(new TestInput("the.100.208.hdtv-lol.mp4", "The 100", "2", "8")); // issue #79
	}

	@Test
	public void testParseFileName() {
		for (TestInput testInput : values) {
			FileEpisode retval = TVRenamer.parseFilename(testInput.input);
			assertEquals(testInput.input, testInput.show, retval.getShowName());
			assertEquals(testInput.input, Integer.parseInt(testInput.season), retval.getSeasonNumber());
			assertEquals(testInput.input, Integer.parseInt(testInput.episode), retval.getEpisodeNumber());
		}
	}

	private static class TestInput {
		public final String input;
		public final String show;
		public final String season;
		public final String episode;

		public TestInput(String input, String show, String season, String episode) {
			this.input = input;
			this.show = show.toLowerCase();
			this.season = season;
			this.episode = episode;
		}
	}

	@Test
	public void testWarehouse13() {
		FileEpisode episode = TVRenamer.parseFilename("Warehouse.13.S05E04.HDTV.x264-2HD.mp4");
		assertEquals("warehouse 13", episode.getShowName());
		assertEquals(5, episode.getSeasonNumber());
		assertEquals(4, episode.getEpisodeNumber());
	}

}
