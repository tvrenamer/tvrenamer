package org.tvrenamer.controller.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testSanitiseTitleBackslash() {
        assertEquals(StringUtils.sanitiseTitle("Test\\"), "Test-");
    }

    @Test
    public void testSanitiseTitleForwardslash() {
        assertEquals(StringUtils.sanitiseTitle("Test/"), "Test-");
    }

    @Test
    public void testSanitiseTitleColon() {
        assertEquals(StringUtils.sanitiseTitle("Test:"), "Test -");
    }

    @Test
    public void testSanitiseTitlePipe() {
        assertEquals(StringUtils.sanitiseTitle("Test|"), "Test-");
    }

    @Test
    public void testSanitiseTitleAsterisk() {
        assertEquals(StringUtils.sanitiseTitle("Test*"), "Test");
    }

    @Test
    public void testSanitiseTitleQuestionMark() {
        assertEquals(StringUtils.sanitiseTitle("Test?"), "Test");
    }

    @Test
    public void testSanitiseTitleGreaterThan() {
        assertEquals(StringUtils.sanitiseTitle("Test>"), "Test");
    }

    @Test
    public void testSanitiseTitleLessThan() {
        assertEquals(StringUtils.sanitiseTitle("Test<"), "Test");
    }

    @Test
    public void testSanitiseTitleDoubleQuote() {
        assertEquals(StringUtils.sanitiseTitle("Test\""), "Test'");
    }

    @Test
    public void testSanitiseTitleTilde() {
        assertEquals(StringUtils.sanitiseTitle("Test`"), "Test'");
    }

    @Test
    public void testGetExtension() {
        assertEquals(".mkv", StringUtils.getExtension("dexter.407.720p.hdtv.x264-sys.mkv"));
        assertEquals(".avi", StringUtils.getExtension("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].avi"));
        assertEquals(".mp4", StringUtils.getExtension("/TV/Dexter/S05E05 First Blood.mp4"));
        assertEquals("", StringUtils.getExtension("Supernatural"));
    }

    @Test
    public void testDotTitle() {
        assertEquals("#HappyHolograms", StringUtils.makeDotTitle("#HappyHolograms"));
        assertEquals("'Twas.the.Nightmare.Before.Christmas",
                     StringUtils.makeDotTitle("'Twas the Nightmare Before Christmas"));
        assertEquals("1%", StringUtils.makeDotTitle("1%"));
        assertEquals("200(1)", StringUtils.makeDotTitle("200 (1)"));

        assertEquals("Helen.Keller!The.Musical", StringUtils.makeDotTitle("Helen Keller! The Musical"));

        assertEquals("And.in.Case.I.Don't.See.Ya", StringUtils.makeDotTitle("And in Case I Don't See Ya"));

        assertEquals("Are.You.There.God.It's.Me,Jesus", StringUtils.makeDotTitle("Are You There God It's Me, Jesus"));

        assertEquals("The.Return.of.Dorothy's.Ex(a.k.a.Stan's.Return)",
                     StringUtils.makeDotTitle("The Return of Dorothy's Ex (a.k.a. Stan's Return)"));
        assertEquals("Girls.Just.Wanna.Have.Fun...Before.They.Die",
                     StringUtils.makeDotTitle("Girls Just Wanna Have Fun...Before They Die"));
        assertEquals("Terrance&Phillip.in'Not.Without.My.Anus'",
                     StringUtils.makeDotTitle("Terrance & Phillip in 'Not Without My Anus'"));

        assertEquals("B&B's.B'n.B", StringUtils.makeDotTitle("B & B's B'n B"));

        assertEquals("AWESOM-O", StringUtils.makeDotTitle("AWESOM-O"));
        assertEquals("Coon.2-Hindsight(1)", StringUtils.makeDotTitle("Coon 2 - Hindsight (1)"));
        assertEquals("Class.Pre-Union", StringUtils.makeDotTitle("Class Pre-Union"));
        assertEquals("D-Yikes!", StringUtils.makeDotTitle("D-Yikes!"));
        assertEquals("Ebbtide.VI-The.Wrath.of.Stan",
                     StringUtils.makeDotTitle("Ebbtide VI - The Wrath of Stan"));
        assertEquals("Goth.Kids.3-Dawn.of.the.Posers",
                     StringUtils.makeDotTitle("Goth Kids 3 - Dawn of the Posers"));
        assertEquals("Jerry-Portrait.of.a.Video.Junkie",
                     StringUtils.makeDotTitle("Jerry - Portrait of a Video Junkie"));
        assertEquals("Musso-a.Wedding", StringUtils.makeDotTitle("Musso - a Wedding"));
        assertEquals("Poetic.License-An.Ode.to.Holden.Caulfield",
                     StringUtils.makeDotTitle("Poetic License - An Ode to Holden Caulfield"));
        assertEquals("Sixteen.Candles.and.400-lb.Men",
                     StringUtils.makeDotTitle("Sixteen Candles and 400-lb. Men"));
        assertEquals("Slapsgiving.2-Revenge.of.the.Slap",
                     StringUtils.makeDotTitle("Slapsgiving 2 - Revenge of the Slap"));
        assertEquals("Valentine's.Day.4-Twisted.Sister",
                     StringUtils.makeDotTitle("Valentine's Day 4 - Twisted Sister"));

        assertEquals("Ro\\$e.Love\\$Mile\\$", StringUtils.makeDotTitle("Ro\\$e Love\\$ Mile\\$"));

        assertEquals("Believe.it.or.Not,Joe's.Walking.on.Air",
                     StringUtils.makeDotTitle("Believe it or Not, Joe's Walking on Air"));
        assertEquals("Eek,A.Penis!", StringUtils.makeDotTitle("Eek, A Penis!"));
        assertEquals("I.Love.You,Donna.Karan(1)", StringUtils.makeDotTitle("I Love You, Donna Karan (1)"));
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
