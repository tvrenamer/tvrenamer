package org.tvrenamer.model.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.tvrenamer.model.util.VersionNumber.isNewer;
import static org.tvrenamer.model.util.VersionNumber.parse;

import org.junit.Test;

public class VersionNumberTest {

    @Test
    public void testBetaIsOlderThanItsRelease() {
        assertTrue(isNewer("1.0", "1.0b5"));
        assertFalse(isNewer("1.0b5", "1.0"));
    }

    @Test
    public void testBetasAreOrderedByNumber() {
        assertTrue(isNewer("1.0b5", "1.0b4"));
        assertFalse(isNewer("1.0b4", "1.0b5"));
    }

    @Test
    public void testBetaNumbersCompareAsNumbers() {
        assertTrue(isNewer("1.0b10", "1.0b9"));
        assertFalse(isNewer("1.0b9", "1.0b10"));
    }

    @Test
    public void testComponentsCompareAsNumbers() {
        assertTrue(isNewer("0.10", "0.9"));
        assertFalse(isNewer("0.9", "0.10"));
    }

    @Test
    public void testOrderingOfEveryPublishedVersion() {
        // Every release this project has shipped, oldest first. Each has to be
        // newer than the one before it.
        final String[] published = {
            "0.6-beta-1", "0.6-beta-2", "0.6",
            "0.7b1", "0.7b2", "0.7b3", "0.7",
            "0.7.1",
            "0.7.2b1", "0.7.2",
            "0.8b1", "0.8b2", "0.8b3", "0.8",
            "1.0b1", "1.0b2", "1.0b3", "1.0b4", "1.0b5",
            "1.0"
        };
        for (int i = 1; i < published.length; i++) {
            final String older = published[i - 1];
            final String newer = published[i];
            assertTrue(newer + " should be newer than " + older,
                       isNewer(newer, older));
            assertFalse(older + " should not be newer than " + newer,
                        isNewer(older, newer));
        }
    }

    @Test
    public void testTrailingZeroesDoNotChangeAVersion() {
        assertEquals(parse("1.0"), parse("1.0.0"));
        assertEquals(parse("1.0").hashCode(), parse("1.0.0").hashCode());
        assertFalse(isNewer("1.0.0", "1.0"));
        assertFalse(isNewer("1.0", "1.0.0"));
    }

    @Test
    public void testSameVersionIsNotAnUpdate() {
        assertFalse(isNewer("1.0b5", "1.0b5"));
    }

    @Test
    public void testSurroundingWhitespaceIsIgnored() {
        // The server answers with a file, so the body arrives with a newline.
        assertTrue(isNewer("1.0b5\n", "1.0b4"));
        assertFalse(isNewer("1.0b4\n", "1.0b5"));
    }

    @Test
    public void testBetaMarkerIsCaseInsensitive() {
        assertTrue(isNewer("1.0B5", "1.0b4"));
    }

    @Test
    public void testUnreadableVersionsAreNotUpdates() {
        assertNull(parse("not a version"));
        assertNull(parse(""));
        assertNull(parse(null));
        assertNull(parse("1.0-rc1"));
        // A garbled or redirected response must not prompt an update.
        assertFalse(isNewer("<html>404</html>", "1.0b5"));
        assertFalse(isNewer(null, "1.0b5"));
        // Nor may an unreadable running version make everything look newer.
        assertFalse(isNewer("1.0", "who knows"));
    }

    @Test
    public void testOversizedComponentIsRejectedRatherThanWrapped() {
        assertNull(parse("99999999999"));
    }
}
