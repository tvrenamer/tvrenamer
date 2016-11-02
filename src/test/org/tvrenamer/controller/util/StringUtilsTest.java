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
