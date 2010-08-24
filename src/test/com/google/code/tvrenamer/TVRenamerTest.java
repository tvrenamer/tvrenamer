package com.google.code.tvrenamer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.FileEpisode;

public class TVRenamerTest {

    public static final String[][] values = new String[13][4];

    @BeforeClass
    public static void setupValues() {
        int i = 0;
        values[i++] = new String[] { "24.s08.e01.720p.hdtv.x264-immerse.mkv", "24", "8", "1" };
        values[i++] = new String[] { "24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv", "24", "7", "18" };
        values[i++] = new String[] { "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "human target 2010", "1", "2" };
        values[i++] = new String[] { "dexter.407.720p.hdtv.x264-sys.mkv", "dexter", "4", "7" };
        values[i++] = new String[] { "JAG.S10E01.DVDRip.XviD-P0W4DVD.avi", "jag", "10", "1" };
        values[i++] = new String[] { "Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "lost", "6", "5" };
        values[i++] = new String[] { "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv", "warehouse 13", "1", "1" };
        values[i++] = new String[] { "one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "one tree hill", "7", "14" };
        values[i++] = new String[] { "gossip.girl.s03e15.hdtv.xvid-fqm.avi", "gossip girl", "3", "15" };
        values[i++] = new String[] { "smallville.s09e14.hdtv.xvid-xii.avi", "smallville", "9", "14" };
        values[i++] = new String[] { "smallville.s09e15.hdtv.xvid-2hd.avi", "smallville", "9", "15" };
        values[i++] = new String[] { "the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv", "the big bang theory", "3", "18" };
        values[i++] = new String[] { "castle.2009.s01e09.720p.hdtv.x264-ctu.mkv", "castle 2009", "1", "9" };
    }

    @Test
    public void testParseFileName() {
        for (int i = 0; i < values.length; i++) {
            System.out.println("testing " + values[i][0] + " -> " + values[i][1] + " [" + values[i][2] + "x" + values[i][3] + "]");
            FileEpisode retval = TVRenamer.parseFilename(values[i][0]);
            System.out.println(retval);
            assertEquals(values[i][1], retval.getShowName());
            assertEquals(Integer.parseInt(values[i][2]), retval.getSeasonNumber());
            assertEquals(Integer.parseInt(values[i][3]), retval.getEpisodeNumber());
        }
    }

}
