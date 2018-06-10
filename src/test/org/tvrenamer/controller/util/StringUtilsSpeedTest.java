package org.tvrenamer.controller.util;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Random;

public class StringUtilsSpeedTest {

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz\\/:|*?<>\"`0123456789     ";
    private static final int NUM_CHARS = CHARACTERS.length();
    private static final int STRING_LENGTH = 10;
    private static final char[] buf = new char[STRING_LENGTH];
    private static final int NUM_TEST_STRINGS = 3000000;

    private static final Random random = new Random();

    public static String[] testString = new String[NUM_TEST_STRINGS];
    public static String[] sanitized = new String[NUM_TEST_STRINGS];

    /**
     * Generate a random string.
     */
    private static String nextString() {
        for (int i = 0; i < STRING_LENGTH; i++) {
            buf[i] = CHARACTERS.charAt(random.nextInt(NUM_CHARS));
        }
        return new String(buf);
    }

    @BeforeClass
    public static void makeTestString() {
        for (int i = 0; i < NUM_TEST_STRINGS; i++) {
            testString[i] = nextString();
        }
    }

    @Test
    public void testSanitiseMillions() {
        for (int i = 0; i < NUM_TEST_STRINGS; i++) {
            sanitized[i] = StringUtils.sanitiseTitle(testString[i]);
        }
        assertEquals(StringUtils.sanitiseTitle("Test"), "Test");
    }
}
