package com.google.code.tvrenamer;

import java.text.DecimalFormat;
import java.util.regex.Matcher;

import org.junit.Test;

import com.google.code.tvrenamer.controller.TVRenamer;

public class FilenameRegexTest {
  private final String[] testFilenames = { "24.s08.e01.720p.hdtv.x264-immerse.mkv", "24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv",
      "human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv", "dexter.407.720p.hdtv.x264-sys.mkv", "JAG.S10E01.DVDRip.XviD-P0W4DVD.avi",
      "Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv", "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv",
      "one.tree.hill.s07e14.hdtv.xvid-fqm.avi", "gossip.girl.s03e15.hdtv.xvid-fqm.avi", "smallville.s09e14.hdtv.xvid-xii.avi",
      "smallville.s09e15.hdtv.xvid-2hd.avi", "the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv" };

  @Test
  public void testRegex() {
    for (int i = 0; i < testFilenames.length; i++) {
      Matcher matcher = TVRenamer.COMPILED_REGEX.matcher(testFilenames[i]);
      System.out.print(testFilenames[i] + " -> ");
      if (matcher.matches()) {
        System.out.println(TVRenamer.replacePunctuation(matcher.group(1)).trim() + " [" + Integer.parseInt(matcher.group(2)) + "x"
            + new DecimalFormat("00").format(Integer.parseInt(matcher.group(3))) + "]");
      } else {
        System.out.println("no match");
      }
    }
  }
}
