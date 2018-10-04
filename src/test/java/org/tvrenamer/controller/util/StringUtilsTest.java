package org.tvrenamer.controller.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.tvrenamer.controller.util.StringUtils.*;

import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testSanitiseTitleBackslash() {
        assertEquals("Test-", sanitiseTitle("Test\\"));
        assertEquals("Test-", replaceIllegalCharacters("Test\\"));
    }

    @Test
    public void testSanitiseTitleForwardSlash() {
        assertEquals("Test-", sanitiseTitle("Test/"));
        assertEquals("Test-", replaceIllegalCharacters("Test/"));
    }

    @Test
    public void testSanitiseTitleColon() {
        assertEquals("Test-", sanitiseTitle("Test:"));
        assertEquals("Test-", replaceIllegalCharacters("Test:"));
    }

    @Test
    public void testSanitiseTitlePipe() {
        assertEquals("Test-", sanitiseTitle("Test|"));
        assertEquals("Test-", replaceIllegalCharacters("Test|"));
    }

    @Test
    public void testSanitiseTitleAsterisk() {
        assertEquals("Test-", sanitiseTitle("Test*"));
        assertEquals("Test-", replaceIllegalCharacters("Test*"));
    }

    @Test
    public void testSanitiseTitleQuestionMark() {
        assertEquals("Test", sanitiseTitle("Test?"));
        assertEquals("Test", replaceIllegalCharacters("Test?"));
    }

    @Test
    public void testSanitiseTitleGreaterThan() {
        assertEquals("Test", sanitiseTitle("Test>"));
        assertEquals("Test", replaceIllegalCharacters("Test>"));
    }

    @Test
    public void testSanitiseTitleLessThan() {
        assertEquals("Test", sanitiseTitle("Test<"));
        assertEquals("Test", replaceIllegalCharacters("Test<"));
    }

    @Test
    public void testSanitiseTitleDoubleQuote() {
        assertEquals("Test'", sanitiseTitle("Test\""));
        assertEquals("Test'", replaceIllegalCharacters("Test\""));
    }

    @Test
    public void testSanitiseTitleTilde() {
        assertEquals("Test'", sanitiseTitle("Test`"));
        assertEquals("Test'", replaceIllegalCharacters("Test`"));
    }

    @Test
    public void testSanitiseTitleTrim() {
        assertEquals("Test", sanitiseTitle("  <Test> \n"));
        assertEquals("  Test \n", replaceIllegalCharacters("  <Test> \n"));
    }

    @Test
    public void testSanitiseTitleOnlyTrim() {
        // The whitespace in between the words should NOT be removed.
        assertEquals("Test Two", sanitiseTitle(" \t<Test Two> "));
        assertEquals(" \tTest Two ", replaceIllegalCharacters(" \t<Test Two> "));
    }

    @Test
    public void testSanitiseTitleEmpty() {
        assertEquals("", sanitiseTitle(""));
        assertEquals("", replaceIllegalCharacters(""));
    }

    @Test
    public void testSanitiseTitleBlank() {
        assertEquals("", sanitiseTitle("   "));
        assertEquals("   ", replaceIllegalCharacters("   "));
    }

    @Test
    public void testUnquoteStringNormal() {
        assertEquals("Season ", unquoteString("\"Season \""));
    }

    @Test
    public void testUnquoteStringUnbalanced() {
        assertEquals("Season ", unquoteString("Season \""));
        assertEquals("Season ", unquoteString("\"Season "));
    }

    @Test
    public void testUnquoteStringNoQuotes() {
        assertEquals("Season ", unquoteString("Season "));
    }

    @Test
    public void testUnquoteStringShort() {
        assertEquals("", unquoteString(""));
        assertEquals(" ", unquoteString(" "));
        assertEquals("s", unquoteString("s"));
    }

    @Test
    public void testUnquoteStringWeird() {
        assertEquals("", unquoteString("\""));
        assertEquals("", unquoteString("\"\""));
        assertEquals("\"foo", unquoteString("\"\"foo"));
        assertEquals("foo\"", unquoteString("\"foo\"\""));
    }

    @Test
    public void testAccessibleMap() {
        assertEquals("-", SANITISE.get('/'));
    }

    @Test
    public void testUnmodifiableMap() {
        try {
            SANITISE.put('/', "_");
            fail("was able to modify map that is supposed to be unmodifiable");
        } catch (Exception e) {
            // expected result
        }
    }

    @Test
    public void testZeroPad() {
        assertEquals("00", zeroPadTwoDigits(0));
        assertEquals("08", zeroPadTwoDigits(8));
        assertEquals("09", zeroPadTwoDigits(9));
        assertEquals("10", zeroPadTwoDigits(10));
        assertEquals("11", zeroPadTwoDigits(11));
        assertEquals("100", zeroPadTwoDigits(100));
    }

    @Test
    public void testRemoveLast() {
        // Straightforward removal; note does not remove punctuation/separators
        assertEquals("foo..baz", removeLast("foo.bar.baz", "bar"));

        // Implementation detail, but the match is required to be all lower-case,
        // while the input doesn't
        assertEquals("Foo..Baz", removeLast("Foo.Bar.Baz", "bar"));

        // Like the name says, the method only removes the last instance
        assertEquals("bar.foo..baz", removeLast("bar.foo.bar.baz", "bar"));

        // Doesn't have to be delimited
        assertEquals("emassment", removeLast("embarassment", "bar"));

        // Doesn't necessarily replace anything
        assertEquals("Foo.Schmar.baz", removeLast("Foo.Schmar.baz", "bar"));

        // This frankly is probably a bug, but this is currently the expected behavior.
        // If the match is not all lower-case to begin with, nothing will be matched.
        assertEquals("Foo.Bar.Baz", removeLast("Foo.Bar.Baz", "Bar"));
    }

    @Test
    public void testGetExtension() {
        assertEquals(".mkv", getExtension("dexter.407.720p.hdtv.x264-sys.mkv"));
        String shield = "Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].avi";
        assertEquals(".avi", getExtension(shield));
        assertEquals(".mp4", getExtension("/TV/Dexter/S05E05 First Blood.mp4"));
        assertEquals("", getExtension("Supernatural"));
    }

    @Test
    public void testDotTitle() {
        // This is the simplest example of how a naive approach might fail
        assertEquals("If.I.Do", makeDotTitle("If I Do "));
        assertEquals("If.I.Do...I.Do", makeDotTitle("If I Do... I Do"));
        assertEquals("#HappyHolograms", makeDotTitle("#HappyHolograms"));
        assertEquals("'Twas.the.Nightmare.Before.Christmas",
                     makeDotTitle("'Twas the Nightmare Before Christmas"));
        assertEquals("1%", makeDotTitle("1%"));
        assertEquals("200(1)", makeDotTitle("200 (1)"));
        assertEquals("Helen.Keller!The.Musical",
                     makeDotTitle("Helen Keller! The Musical"));
        assertEquals("And.in.Case.I.Don't.See.Ya",
                     makeDotTitle("And in Case I Don't See Ya"));
        assertEquals("Are.You.There.God.It's.Me,Jesus",
                     makeDotTitle("Are You There God It's Me, Jesus"));
        assertEquals("The.Return.of.Dorothy's.Ex(a.k.a.Stan's.Return)",
                     makeDotTitle("The Return of Dorothy's Ex (a.k.a. Stan's Return)"));
        assertEquals("Girls.Just.Wanna.Have.Fun...Before.They.Die",
                     makeDotTitle("Girls Just Wanna Have Fun... Before They Die"));
        assertEquals("Terrance&Phillip.in'Not.Without.My.Anus'",
                     makeDotTitle("Terrance & Phillip in 'Not Without My Anus'"));
        assertEquals("B&B's.B'n.B", makeDotTitle("B & B's B'n B"));
        assertEquals("AWESOM-O", makeDotTitle("AWESOM-O"));
        assertEquals("Coon.2-Hindsight(1)", makeDotTitle("Coon 2 - Hindsight (1)"));
        assertEquals("Class.Pre-Union", makeDotTitle("Class Pre-Union"));
        assertEquals("D-Yikes!", makeDotTitle("D-Yikes!"));
        assertEquals("Ebbtide.VI-The.Wrath.of.Stan",
                     makeDotTitle("Ebbtide VI - The Wrath of Stan"));
        assertEquals("Goth.Kids.3-Dawn.of.the.Posers",
                     makeDotTitle("Goth Kids 3 - Dawn of the Posers"));
        assertEquals("Jerry-Portrait.of.a.Video.Junkie",
                     makeDotTitle("Jerry - Portrait of a Video Junkie"));
        assertEquals("Musso-a.Wedding", makeDotTitle("Musso - a Wedding"));
        assertEquals("Poetic.License-An.Ode.to.Holden.Caulfield",
                     makeDotTitle("Poetic License - An Ode to Holden Caulfield"));
        assertEquals("Sixteen.Candles.and.400-lb.Men",
                     makeDotTitle("Sixteen Candles and 400-lb. Men"));
        assertEquals("Slapsgiving.2-Revenge.of.the.Slap",
                     makeDotTitle("Slapsgiving 2 - Revenge of the Slap"));
        assertEquals("Valentine's.Day.4-Twisted.Sister",
                     makeDotTitle("Valentine's Day 4 - Twisted Sister"));
        assertEquals("Ro\\$e.Love\\$Mile\\$",
                     makeDotTitle("Ro\\$e Love\\$ Mile\\$"));
        assertEquals("Believe.it.or.Not,Joe's.Walking.on.Air",
                     makeDotTitle("Believe it or Not, Joe's Walking on Air"));
        assertEquals("Eek,A.Penis!", makeDotTitle("Eek, A Penis!"));
        assertEquals("I.Love.You,Donna.Karan(1)",
                     makeDotTitle("I Love You, Donna Karan (1)"));
    }

    @Test
    public void testReplacePunctuation() {
        assertEquals("Marvels Agents of SHIELD",
                     replacePunctuation("Marvel's.Agents.of.S.H.I.E.L.D."));
        assertEquals("Marvels Agents of SHIELD",
                     replacePunctuation("Marvel's Agents of S.H.I.E.L.D."));
        assertEquals("Marvels Agents of SHIELD",
                     replacePunctuation("Marvel's Agents of SHIELD"));
        assertEquals("Star Trek The Next Generation",
                     replacePunctuation("Star Trek: The Next Generation"));
        assertEquals("Monty Pythons Flying Circus",
                     replacePunctuation("Monty Python's Flying Circus"));
        assertEquals("Married with Children",
                     replacePunctuation("Married... with Children"));
        assertEquals("God The Devil and Bob",
                     replacePunctuation("God, The Devil and Bob"));
        assertEquals("Whats Happening",
                     replacePunctuation("What's Happening!!"));
        assertEquals("Brooklyn Nine Nine",
                     replacePunctuation("Brooklyn Nine-Nine"));
        assertEquals("Murder She Wrote", replacePunctuation("Murder, She Wrote"));
        assertEquals("Murder She Wrote", replacePunctuation("Murder-She-Wrote"));
        assertEquals("Andy Barker PI", replacePunctuation("Andy Barker, P.I."));
        assertEquals("Laverne & Shirley", replacePunctuation("Laverne & Shirley"));
        assertEquals("Sit Down Shut Up", replacePunctuation("Sit Down, Shut Up"));
        assertEquals("The Real ONeals", replacePunctuation("The Real O'Neals"));
        assertEquals("The Office (US)", replacePunctuation("The Office (US)"));
        assertEquals("That 70s Show", replacePunctuation("That '70s Show"));
        assertEquals("Eerie Indiana", replacePunctuation("Eerie, Indiana"));
        assertEquals("American Dad", replacePunctuation("American Dad!"));
        assertEquals("Bobs Burgers", replacePunctuation("Bob's Burgers"));
        assertEquals("Man vs Wild", replacePunctuation("Man vs. Wild"));
        assertEquals("The X Files", replacePunctuation("The X-Files"));
        assertEquals("Myth Busters", replacePunctuation("MythBusters"));
        assertEquals("Blackish", replacePunctuation("Black-ish"));
        assertEquals("30 Rock", replacePunctuation("30Rock"));
        assertEquals("Mr Robot", replacePunctuation("Mr. Robot"));
        assertEquals("Starving", replacePunctuation("Star-ving"));
        assertEquals("big bang theory", replacePunctuation("big-bang-theory"));
        assertEquals("american dad", replacePunctuation("american-dad"));
        assertEquals("Cosmos A Space Time Odyssey",
                     replacePunctuation("Cosmos.A.Space.Time.Odyssey."));
        assertEquals("How I Met Your Mother",
                     replacePunctuation("How.I.Met.Your.Mother."));
    }

    @Test
    public void testReplacePunctuation2() {
        // The apostrophe (single quote) is treated specially: simply removed
        assertEquals("New Girl", replacePunctuation("Ne'w Girl"));
        // Parentheses and ampersand are left alone
        assertEquals("New (Girl)", replacePunctuation("New (Girl)"));
        assertEquals("New & Girl", replacePunctuation("New & Girl"));
        // Other punctuation gets replaced by a space
        assertEquals("New Girl", replacePunctuation("New\\Girl"));
        assertEquals("New Girl", replacePunctuation("New\"Girl"));
        assertEquals("New Girl", replacePunctuation("New!Girl"));
        assertEquals("New Girl", replacePunctuation("New#Girl"));
        assertEquals("New Girl", replacePunctuation("New$Girl"));
        assertEquals("New Girl", replacePunctuation("New%Girl"));
        assertEquals("New Girl", replacePunctuation("New*Girl"));
        assertEquals("New Girl", replacePunctuation("New+Girl"));
        assertEquals("New Girl", replacePunctuation("New,Girl"));
        assertEquals("New Girl", replacePunctuation("New-Girl"));
        assertEquals("New Girl", replacePunctuation("New.Girl"));
        assertEquals("New Girl", replacePunctuation("New/Girl"));
        assertEquals("New Girl", replacePunctuation("New:Girl"));
        assertEquals("New Girl", replacePunctuation("New;Girl"));
        assertEquals("New Girl", replacePunctuation("New<Girl"));
        assertEquals("New Girl", replacePunctuation("New=Girl"));
        assertEquals("New Girl", replacePunctuation("New>Girl"));
        assertEquals("New Girl", replacePunctuation("New?Girl"));
        assertEquals("New Girl", replacePunctuation("New@Girl"));
        assertEquals("New Girl", replacePunctuation("New[Girl"));
        assertEquals("New Girl", replacePunctuation("New]Girl"));
        assertEquals("New Girl", replacePunctuation("New^Girl"));
        assertEquals("New Girl", replacePunctuation("New_Girl"));
        assertEquals("New Girl", replacePunctuation("New`Girl"));
        assertEquals("New Girl", replacePunctuation("New{Girl"));
        assertEquals("New Girl", replacePunctuation("New|Girl"));
        assertEquals("New Girl", replacePunctuation("New}Girl"));
        assertEquals("New Girl", replacePunctuation("New~Girl"));
    }

    /**
     * Test trimFoundShow.  It should trim separator characters (space, hyphen,
     * dot, underscore) from the beginning and end of the string, but not change
     * the middle, substantive part at all.
     *
     */
    @Test
    public void testTrimFoundShow() {
        assertEquals("Dr. Foo's Man-Pig", trimFoundShow("Dr. Foo's Man-Pig"));
        assertEquals("Dr. Foo's Man-Pig", trimFoundShow("Dr. Foo's Man-Pig "));
        assertEquals("Dr. Foo's_Man-Pig", trimFoundShow("Dr. Foo's_Man-Pig_"));
        assertEquals("Dr. Foo's_Man-Pig", trimFoundShow("  Dr. Foo's_Man-Pig_"));
    }

    /**
     * Helper method.  We used to take the substring produced by the parser (the
     * "filename show" or "found show") and pass it to makeQueryString to get the
     * string to send to the provider.  Now, we're inserting another step in there:
     * trimFoundShow.  But this is not intended to change the strings we send to
     * the provider, in anyway.  So this method validates that.  The result of
     * calling makeQueryString on the trimmed string, should be identical to calling
     * makeQueryString on the original string.
     *
     * @param input
     *   any String, but intended to be the part of a filename that we think
     *   represents the name of the show
     *
     */
    private void assertTrimSafe(String input) {
        assertEquals(makeQueryString(input),
                     makeQueryString(trimFoundShow(input)));
    }

    /**
     * Now that we have a method to verify that trimFoundShow is not changing the
     * results of makeQueryString, run it through all the sample data we used in
     * {@link #testReplacePunctuation} and {@link #testTrimFoundShow}.
     *
     */
    @Test
    public void testTrimForQueryString() {
        assertTrimSafe("Marvel's.Agents.of.S.H.I.E.L.D.");
        assertTrimSafe("Marvel's Agents of S.H.I.E.L.D.");
        assertTrimSafe("Marvel's Agents of SHIELD");
        assertTrimSafe("Star Trek: The Next Generation");
        assertTrimSafe("Monty Python's Flying Circus");
        assertTrimSafe("Married... with Children");
        assertTrimSafe("God, The Devil and Bob");
        assertTrimSafe("What's Happening!!");
        assertTrimSafe("Brooklyn Nine-Nine");
        assertTrimSafe("Murder, She Wrote");
        assertTrimSafe("Murder-She-Wrote");
        assertTrimSafe("Andy Barker, P.I.");
        assertTrimSafe("Laverne & Shirley");
        assertTrimSafe("Sit Down, Shut Up");
        assertTrimSafe("The Real O'Neals");
        assertTrimSafe("The Office (US)");
        assertTrimSafe("That '70s Show");
        assertTrimSafe("Eerie, Indiana");
        assertTrimSafe("American Dad!");
        assertTrimSafe("Bob's Burgers");
        assertTrimSafe("Man vs. Wild");
        assertTrimSafe("The X-Files");
        assertTrimSafe("MythBusters");
        assertTrimSafe("Black-ish");
        assertTrimSafe("30Rock");
        assertTrimSafe("Mr. Robot");
        assertTrimSafe("Star-ving");
        assertTrimSafe("big-bang-theory");
        assertTrimSafe("american-dad");
        assertTrimSafe("Cosmos.A.Space.Time.Odyssey.");
        assertTrimSafe("How.I.Met.Your.Mother.");
        assertTrimSafe("Dr. Foo's Man-Pig");
        assertTrimSafe("Dr. Foo's Man-Pig ");
        assertTrimSafe("Dr. Foo's_Man-Pig_");
        assertTrimSafe("  Dr. Foo's_Man-Pig_");
    }
}
