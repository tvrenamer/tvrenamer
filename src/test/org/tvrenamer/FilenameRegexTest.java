package org.tvrenamer;

import org.tvrenamer.controller.TVRenamer;
import org.tvrenamer.model.FileEpisode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static org.junit.Assert.assertNotNull;


@RunWith(Parameterized.class)
public class FilenameRegexTest {
    @Parameters
    public static final Iterable<? extends Object> data() {
        return Arrays.asList(
            "24.s08.e01.720p.hdtv.x264-immerse.mkv",
            "24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv",
            "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv",
            "dexter.407.720p.hdtv.x264-sys.mkv",
            "JAG.S10E01.DVDRip.XviD-P0W4DVD.avi",
            "Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv",
            "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv",
            "one.tree.hill.s07e14.hdtv.xvid-fqm.avi",
            "gossip.girl.s03e15.hdtv.xvid-fqm.avi",
            "smallville.s09e14.hdtv.xvid-xii.avi",
            "smallville.s09e15.hdtv.xvid-2hd.avi",
            "the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv",
            "castle.2009.s01e09.720p.hdtv.x264-ctu.mkv",
            "Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET"
        );
    }

    private String input;

    public FilenameRegexTest(String input) {
        this.input = input;
    }

    @Test
    public void testRegex() {
        FileEpisode result = TVRenamer.parseFilename(input);
        assertNotNull(result);
    }
}
