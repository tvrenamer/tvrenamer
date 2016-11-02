package org.tvrenamer.controller.util;

import java.util.logging.Logger;

public class StringUtils {
    private static Logger logger = Logger.getLogger(StringUtils.class.getName());

    public static String sanitiseTitle(String title) {
        // anything that's not valid in Windows will be replaced
        // this list is: \ / : * ? " < > |
        // see http://msdn.microsoft.com/en-us/library/aa365247%28VS.85%29.aspx for more information

        title = title.replace('\\', '-'); // replace '\' with '-'
        title = title.replace('/', '-'); // replace '/' with '-'
        title = title.replace(":", " -"); // replace ':' with ' -'
        title = title.replace('|', '-'); // replace '|' with '-'
        title = title.replace("*", ""); // replace '*' with ''
        title = title.replace("?", ""); // replace '?' with ''
        title = title.replace("<", ""); // replace '<' with ''
        title = title.replace(">", ""); // replace '>' with ''
        title = title.replace("\"", "'"); // replace '"' with "'"
        title = title.replace("`", "'"); // replace '`' with "'"
        return title;
    }

    public static String replacePunctuation(String s) {
        String rval = s;

        // condenses acronyms (S.H.I.E.L.D. -> SHIELD)
        rval = rval.replaceAll("(\\p{Upper})[.]", "$1");

        // The apostrophe is kind of different, because it's often found within a word, including
        // in show titles: "Bob's Burgers", "The Real O'Neals", "What's Happening", "Don't Trust..."
        // For these, replacing the apostrophe with a space confuses the database; it's much better
        // to simply remove the apostrophe.
        rval = rval.replaceAll("'", "");

        // A hyphen in the middle of a word also should not be broken up into two words
        rval = rval.replaceAll("(\\p{Lower})-(\\p{Lower})", "$1$2");

        // replaces remaining punctuation (",", ".", etc) with spaces
        rval = rval.replaceAll("\\p{Punct}", " ");

        // transform "CamelCaps" => "Camel Caps"
        rval = rval.replaceAll("(\\p{Lower})(\\p{Upper})", "$1 $2");

        // get rid of superfluous whitespace
        rval = rval.replaceAll(" [ ]+", " ").trim();

        return rval;
    }

    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return filename.substring(dot + 1);
    }

    /**
     * Replaces unsafe HTML Characters with HTML Entities
     *
     * @param input
     *            string to encode
     * @return HTML safe representation of input
     */
    public static String encodeSpecialCharacters(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }

        // TODO: determine other characters that need to be replaced (eg "'", "-")
        logger.finest("Input before encoding: [" + input + "]");
        input = input.replaceAll("& ", "&amp; ");

        // Don't encode string within xml data strings
        if(!input.startsWith("<?xml")) {
            input = input.replaceAll(" ", "%20");
        }
        logger.finest("Input after encoding: [" + input + "]");
        return input;
    }

    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     * Copied from
     * <a href="http://commons.apache.org/lang/api-2.5/org/apache/commons/lang/StringUtils.html#isBlank(java.lang.String)">
     *   Apache Commons Lang StringUtils
     * </a>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
     * Copied from
     * <a href="http://commons.apache.org/lang/api-2.5/org/apache/commons/lang/StringUtils.html#isNotBlank(java.lang.String)">
     *   Apache Commons Lang StringUtils
     * </a>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(String str) {
        return !StringUtils.isBlank(str);
    }
}
