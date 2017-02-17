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

    /**
     * Transform a string which we believe represents a show name, to the string we will
     * use for the query.
     *
     * For the internal data structures used by this class, the keys should always be
     * the result of this method.  It's not up to callers to worry about; they can pass
     * in show names however they have them, and the methods here will be sure to
     * normalize them in preparation for querying.
     *
     * @param text
     *            the String that we want to normalize; we assume it is:
     *            the substring of the file path that we think represents the show name
     * @return a version of the show name that is more suitable for a query; this may
     *         include case normalization, removal of superfluous whitepsace and
     *         punctuation, etc.
     */
    public static String makeQueryString(String text) {
        return replacePunctuation(text).toLowerCase();
    }

    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            return filename.substring(dot);
        }
        return "";
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
        if (!input.startsWith("<?xml")) {
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
