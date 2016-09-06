package com.google.code.tvrenamer;

import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;
import java.util.regex.Matcher;

import org.junit.Test;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.controller.util.StringUtils;

public class FilenameRegexTest {
    private final String[] testFilenames = {
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
    };

	@Test
	public void testRegex() {
		for (int i = 0; i < testFilenames.length; i++) {
			Matcher matcher = TVRenamer.COMPILED_REGEX[1].matcher(testFilenames[i]);
			System.out.print(testFilenames[i] + " -> ");
			if (matcher.matches()) {
				System.out.println(StringUtils.replacePunctuation(matcher.group(1)).trim() + " ["
					+ Integer.parseInt(matcher.group(2)) + "x"
					+ new DecimalFormat("00").format(Integer.parseInt(matcher.group(3))) + "]");
			} else {
				System.out.println("no match");
			}
		}
	}

  @Test
  public void testSanitizeTitle() {
    assertEquals("Microsoft - Windows", StringUtils.sanitiseTitle("Microsoft / Windows"));
    assertEquals("Microsoft - Windows", StringUtils.sanitiseTitle("Microsoft | Windows"));
    assertEquals("Microsoft - Windows", StringUtils.sanitiseTitle("Microsoft \\ Windows"));
    assertEquals("Microsoft  - Windows", StringUtils.sanitiseTitle("Microsoft : Windows"));
    assertEquals("Microsoft ' Windows", StringUtils.sanitiseTitle("Microsoft ` Windows"));
    assertEquals("Microsoft ' Windows", StringUtils.sanitiseTitle("Microsoft \" Windows"));
    assertEquals("Microsoft  Windows", StringUtils.sanitiseTitle("Microsoft * Windows"));
    assertEquals("Microsoft  Windows", StringUtils.sanitiseTitle("Microsoft ? Windows"));
    assertEquals("Microsoft  Windows", StringUtils.sanitiseTitle("Microsoft < Windows"));
    assertEquals("Microsoft  Windows", StringUtils.sanitiseTitle("Microsoft > Windows"));
  }

  @Test
  public void testReplacePunctuation() {
    assertEquals("Marvels Agents of SHIELD", StringUtils.replacePunctuation("Marvel's Agents of S.H.I.E.L.D."));
    assertEquals("Star Trek The Next Generation", StringUtils.replacePunctuation("Star Trek: The Next Generation"));
    assertEquals("Monty Pythons Flying Circus", StringUtils.replacePunctuation("Monty Python's Flying Circus"));
    assertEquals("Married with Children", StringUtils.replacePunctuation("Married... with Children"));
    assertEquals("God The Devil and Bob", StringUtils.replacePunctuation("God, The Devil and Bob"));
    assertEquals("Whats Happening", StringUtils.replacePunctuation("What's Happening!!"));
    assertEquals("Brooklyn Nine Nine", StringUtils.replacePunctuation("Brooklyn Nine-Nine"));
    assertEquals("Murder She Wrote", StringUtils.replacePunctuation("Murder, She Wrote"));
    assertEquals("Andy Barker PI", StringUtils.replacePunctuation("Andy Barker, P.I."));
    assertEquals("Sit Down Shut Up", StringUtils.replacePunctuation("Sit Down, Shut Up"));
    assertEquals("The Real ONeals", StringUtils.replacePunctuation("The Real O'Neals"));
    assertEquals("The Office US", StringUtils.replacePunctuation("The Office (US)"));
    assertEquals("That 70s Show", StringUtils.replacePunctuation("That '70s Show"));
    assertEquals("Eerie Indiana", StringUtils.replacePunctuation("Eerie, Indiana"));
    assertEquals("American Dad", StringUtils.replacePunctuation("American Dad!"));
    assertEquals("Bobs Burgers", StringUtils.replacePunctuation("Bob's Burgers"));
    assertEquals("Man vs Wild", StringUtils.replacePunctuation("Man vs. Wild"));
    assertEquals("The X Files", StringUtils.replacePunctuation("The X-Files"));
    assertEquals("Myth Busters", StringUtils.replacePunctuation("MythBusters"));
    assertEquals("Blackish", StringUtils.replacePunctuation("Black-ish"));
    assertEquals("Mr Robot", StringUtils.replacePunctuation("Mr. Robot"));
    assertEquals("Starving", StringUtils.replacePunctuation("Star-ving"));
  }
}
