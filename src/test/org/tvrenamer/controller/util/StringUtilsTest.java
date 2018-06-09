package org.tvrenamer.controller.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testSanitiseTitleBackslash() {
        assertEquals("Test-", StringUtils.sanitiseTitle("Test\\"));
        assertEquals("Test-", StringUtils.replaceIllegalCharacters("Test\\"));
    }

    @Test
    public void testSanitiseTitleForwardSlash() {
        assertEquals("Test-", StringUtils.sanitiseTitle("Test/"));
        assertEquals("Test-", StringUtils.replaceIllegalCharacters("Test/"));
    }

    @Test
    public void testSanitiseTitleColon() {
        assertEquals("Test-", StringUtils.sanitiseTitle("Test:"));
        assertEquals("Test-", StringUtils.replaceIllegalCharacters("Test:"));
    }

    @Test
    public void testSanitiseTitlePipe() {
        assertEquals("Test-", StringUtils.sanitiseTitle("Test|"));
        assertEquals("Test-", StringUtils.replaceIllegalCharacters("Test|"));
    }

    @Test
    public void testSanitiseTitleAsterisk() {
        assertEquals("Test-", StringUtils.sanitiseTitle("Test*"));
        assertEquals("Test-", StringUtils.replaceIllegalCharacters("Test*"));
    }

    @Test
    public void testSanitiseTitleQuestionMark() {
        assertEquals("Test", StringUtils.sanitiseTitle("Test?"));
        assertEquals("Test", StringUtils.replaceIllegalCharacters("Test?"));
    }

    @Test
    public void testSanitiseTitleGreaterThan() {
        assertEquals("Test", StringUtils.sanitiseTitle("Test>"));
        assertEquals("Test", StringUtils.replaceIllegalCharacters("Test>"));
    }

    @Test
    public void testSanitiseTitleLessThan() {
        assertEquals("Test", StringUtils.sanitiseTitle("Test<"));
        assertEquals("Test", StringUtils.replaceIllegalCharacters("Test<"));
    }

    @Test
    public void testSanitiseTitleDoubleQuote() {
        assertEquals("Test'", StringUtils.sanitiseTitle("Test\""));
        assertEquals("Test'", StringUtils.replaceIllegalCharacters("Test\""));
    }

    @Test
    public void testSanitiseTitleTilde() {
        assertEquals("Test'", StringUtils.sanitiseTitle("Test`"));
        assertEquals("Test'", StringUtils.replaceIllegalCharacters("Test`"));
    }

    @Test
    public void testSanitiseTitleTrim() {
        assertEquals("Test", StringUtils.sanitiseTitle("  <Test> \n"));
        assertEquals("  Test \n", StringUtils.replaceIllegalCharacters("  <Test> \n"));
    }

    @Test
    public void testSanitiseTitleOnlyTrim() {
        // The whitespace in between the words should NOT be removed.
        assertEquals("Test Two", StringUtils.sanitiseTitle(" \t<Test Two> "));
        assertEquals(" \tTest Two ", StringUtils.replaceIllegalCharacters(" \t<Test Two> "));
    }

    @Test
    public void testSanitiseTitleEmpty() {
        assertEquals("", StringUtils.sanitiseTitle(""));
        assertEquals("", StringUtils.replaceIllegalCharacters(""));
    }

    @Test
    public void testSanitiseTitleBlank() {
        assertEquals("", StringUtils.sanitiseTitle("   "));
        assertEquals("   ", StringUtils.replaceIllegalCharacters("   "));
    }

    @Test
    public void testUnquoteStringNormal() {
        assertEquals("Season ", StringUtils.unquoteString("\"Season \""));
    }

    @Test
    public void testUnquoteStringUnbalanced() {
        assertEquals("Season ", StringUtils.unquoteString("Season \""));
        assertEquals("Season ", StringUtils.unquoteString("\"Season "));
    }

    @Test
    public void testUnquoteStringNoQuotes() {
        assertEquals("Season ", StringUtils.unquoteString("Season "));
    }

    @Test
    public void testUnquoteStringShort() {
        assertEquals("", StringUtils.unquoteString(""));
        assertEquals(" ", StringUtils.unquoteString(" "));
        assertEquals("s", StringUtils.unquoteString("s"));
    }

    @Test
    public void testUnquoteStringWeird() {
        assertEquals("", StringUtils.unquoteString("\""));
        assertEquals("", StringUtils.unquoteString("\"\""));
        assertEquals("\"foo", StringUtils.unquoteString("\"\"foo"));
        assertEquals("foo\"", StringUtils.unquoteString("\"foo\"\""));
    }

    @Test
    public void testZeroPad() {
        assertEquals("00", StringUtils.zeroPadTwoDigits(0));
        assertEquals("08", StringUtils.zeroPadTwoDigits(8));
        assertEquals("09", StringUtils.zeroPadTwoDigits(9));
        assertEquals("10", StringUtils.zeroPadTwoDigits(10));
        assertEquals("11", StringUtils.zeroPadTwoDigits(11));
        assertEquals("100", StringUtils.zeroPadTwoDigits(100));
    }

    @Test
    public void testRemoveLast() {
        // Straightforward removal; note does not remove punctuation/separators
        assertEquals("foo..baz", StringUtils.removeLast("foo.bar.baz", "bar"));

        // Implementation detail, but the match is required to be all lower-case,
        // while the input doesn't
        assertEquals("Foo..Baz", StringUtils.removeLast("Foo.Bar.Baz", "bar"));

        // Like the name says, the method only removes the last instance
        assertEquals("bar.foo..baz", StringUtils.removeLast("bar.foo.bar.baz", "bar"));

        // Doesn't have to be delimited
        assertEquals("emassment", StringUtils.removeLast("embarassment", "bar"));

        // Doesn't necessarily replace anything
        assertEquals("Foo.Schmar.baz", StringUtils.removeLast("Foo.Schmar.baz", "bar"));

        // This frankly is probably a bug, but this is currently the expected behavior.
        // If the match is not all lower-case to begin with, nothing will be matched.
        assertEquals("Foo.Bar.Baz", StringUtils.removeLast("Foo.Bar.Baz", "Bar"));
    }

    @Test
    public void testGetExtension() {
        assertEquals(".mkv", StringUtils.getExtension("dexter.407.720p.hdtv.x264-sys.mkv"));
        String shield = "Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].avi";
        assertEquals(".avi", StringUtils.getExtension(shield));
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
        assertEquals("Helen.Keller!The.Musical",
                     StringUtils.makeDotTitle("Helen Keller! The Musical"));
        assertEquals("And.in.Case.I.Don't.See.Ya",
                     StringUtils.makeDotTitle("And in Case I Don't See Ya"));
        assertEquals("Are.You.There.God.It's.Me,Jesus",
                     StringUtils.makeDotTitle("Are You There God It's Me, Jesus"));
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
        assertEquals("Ro\\$e.Love\\$Mile\\$",
                     StringUtils.makeDotTitle("Ro\\$e Love\\$ Mile\\$"));
        assertEquals("Believe.it.or.Not,Joe's.Walking.on.Air",
                     StringUtils.makeDotTitle("Believe it or Not, Joe's Walking on Air"));
        assertEquals("Eek,A.Penis!", StringUtils.makeDotTitle("Eek, A Penis!"));
        assertEquals("I.Love.You,Donna.Karan(1)",
                     StringUtils.makeDotTitle("I Love You, Donna Karan (1)"));
    }

    @Test
    public void testReplacePunctuation() {
        assertEquals("Marvels Agents of SHIELD",
                     StringUtils.replacePunctuation("Marvel's.Agents.of.S.H.I.E.L.D."));
        assertEquals("Marvels Agents of SHIELD",
                     StringUtils.replacePunctuation("Marvel's Agents of S.H.I.E.L.D."));
        assertEquals("Marvels Agents of SHIELD",
                     StringUtils.replacePunctuation("Marvel's Agents of SHIELD"));
        assertEquals("Star Trek The Next Generation",
                     StringUtils.replacePunctuation("Star Trek: The Next Generation"));
        assertEquals("Monty Pythons Flying Circus",
                     StringUtils.replacePunctuation("Monty Python's Flying Circus"));
        assertEquals("Married with Children",
                     StringUtils.replacePunctuation("Married... with Children"));
        assertEquals("God The Devil and Bob",
                     StringUtils.replacePunctuation("God, The Devil and Bob"));
        assertEquals("Whats Happening",
                     StringUtils.replacePunctuation("What's Happening!!"));
        assertEquals("Brooklyn Nine Nine",
                     StringUtils.replacePunctuation("Brooklyn Nine-Nine"));
        assertEquals("Murder She Wrote", StringUtils.replacePunctuation("Murder, She Wrote"));
        assertEquals("Murder She Wrote", StringUtils.replacePunctuation("Murder-She-Wrote"));
        assertEquals("Andy Barker PI", StringUtils.replacePunctuation("Andy Barker, P.I."));
        assertEquals("Laverne & Shirley", StringUtils.replacePunctuation("Laverne & Shirley"));
        assertEquals("Sit Down Shut Up", StringUtils.replacePunctuation("Sit Down, Shut Up"));
        assertEquals("The Real ONeals", StringUtils.replacePunctuation("The Real O'Neals"));
        assertEquals("The Office (US)", StringUtils.replacePunctuation("The Office (US)"));
        assertEquals("That 70s Show", StringUtils.replacePunctuation("That '70s Show"));
        assertEquals("Eerie Indiana", StringUtils.replacePunctuation("Eerie, Indiana"));
        assertEquals("American Dad", StringUtils.replacePunctuation("American Dad!"));
        assertEquals("Bobs Burgers", StringUtils.replacePunctuation("Bob's Burgers"));
        assertEquals("Man vs Wild", StringUtils.replacePunctuation("Man vs. Wild"));
        assertEquals("The X Files", StringUtils.replacePunctuation("The X-Files"));
        assertEquals("Myth Busters", StringUtils.replacePunctuation("MythBusters"));
        assertEquals("Blackish", StringUtils.replacePunctuation("Black-ish"));
        assertEquals("30 Rock", StringUtils.replacePunctuation("30Rock"));
        assertEquals("Mr Robot", StringUtils.replacePunctuation("Mr. Robot"));
        assertEquals("Starving", StringUtils.replacePunctuation("Star-ving"));
        assertEquals("big bang theory", StringUtils.replacePunctuation("big-bang-theory"));
        assertEquals("american dad", StringUtils.replacePunctuation("american-dad"));
        assertEquals("Cosmos A Space Time Odyssey",
                     StringUtils.replacePunctuation("Cosmos.A.Space.Time.Odyssey."));
        assertEquals("How I Met Your Mother",
                     StringUtils.replacePunctuation("How.I.Met.Your.Mother."));
    }

    @Test
    public void testReplacePunctuation2() {
        // The apostrophe (single quote) is treated specially: simply removed
        assertEquals("New Girl", StringUtils.replacePunctuation("Ne'w Girl"));
        // Parentheses and ampersand are left alone
        assertEquals("New (Girl)", StringUtils.replacePunctuation("New (Girl)"));
        assertEquals("New & Girl", StringUtils.replacePunctuation("New & Girl"));
        // Other punctuation gets replaced by a space
        assertEquals("New Girl", StringUtils.replacePunctuation("New\\Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New\"Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New!Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New#Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New$Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New%Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New*Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New+Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New,Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New-Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New.Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New/Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New:Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New;Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New<Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New=Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New>Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New?Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New@Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New[Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New]Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New^Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New_Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New`Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New{Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New|Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New}Girl"));
        assertEquals("New Girl", StringUtils.replacePunctuation("New~Girl"));
    }
}
