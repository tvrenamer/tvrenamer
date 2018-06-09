package org.tvrenamer.controller.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

public class StringUtils {
    private static final Logger logger = Logger.getLogger(StringUtils.class.getName());

    private static final Locale THIS_LOCALE = Locale.getDefault();

    public static final HashMap<Character, String> SANITISE
        = new HashMap<Character, String>()
        {
            // provide a replacement for anything that's not valid in Windows
            // this list is: \ / : * ? " < > |
            // see http://msdn.microsoft.com/en-us/library/aa365247%28VS.85%29.aspx for more information
            {
                put('\\', "-"); // replace backslash with hyphen
                put('/', "-");  // replace forward slash with hyphen
                put(':', "-");  // replace colon with a hyphen
                put('|', "-");  // replace vertical bar with hyphen
                put('*', "-");  // replace asterisk with hyphen; for example,
                                // the episode "C**tgate" of Veep should become "C--tgate", not "Ctgate"
                put('?', "");   // remove question marks
                put('<', "");   // remove less-than symbols
                put('>', "");   // remove greater-than symbols
                put('"', "'");  // replace double quote with apostrophe
                put('`', "'");  // replace backquote with apostrophe
            }
        };


    private static final ThreadLocal<DecimalFormat> DIGITS =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("##0");
            }
        };

    private static final ThreadLocal<DecimalFormat> TWO_OR_THREE =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#00");
            }
        };

    private static final ThreadLocal<DecimalFormat> KB_FORMAT =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#.# kB");
            }
        };

    private static final ThreadLocal<DecimalFormat> MB_FORMAT =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#.# MB");
            }
        };

    public static String toLower(String orig) {
        if (orig == null) {
            return "";
        }
        return orig.toLowerCase(THIS_LOCALE);
    }

    public static String toUpper(String orig) {
        if (orig == null) {
            return "";
        }
        return orig.toUpperCase(THIS_LOCALE);
    }


    public static String makeString(byte[] buffer) {
        String rval;
        try {
            rval = new String(buffer, "ASCII");
        } catch (UnsupportedEncodingException uee) {
            rval = "";
        }
        return rval;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static String makeDotTitle(String titleString) {
        String pass1 = titleString.replaceAll("(\\w)\\s+(\\w)", "$1.$2");
        String pass2 = pass1.replaceAll("(\\w)\\s+(\\w)", "$1.$2");
        String pass3 = pass2.replaceAll("\\s", "");
        return pass3;
    }

    public static String removeLast(String input, String match) {
        int idx = toLower(input).lastIndexOf(match);
        if (idx > 0) {
            input = input.substring(0, idx)
                + input.substring(idx + match.length(), input.length());
        }
        return input;
    }

    public static String makeQuotedString(final String original) {
        return ("\"" + original + "\"");
    }

    /**
     * Strip away double-quote characters at the beginning and end of a string.
     *
     * Returns a string identical to the original, except that if the first and/or last
     * character of the original was a double-quote character, it is omitted.  The quotes
     * do not have to be balanced.  If there is one at the beginning but not the end, it's
     * removed.  Or the end but not the beginning, or both.
     *
     * Any double-quote characters that may occur in the middle of the string are untouched.
     * This would even include a situation where the string begins with two double quotes.
     * It is not analagous to String.trim() in that sense.  It strips at most one character
     * from the beginning, and at most one from the end.
     *
     * @param original the String to trim double quotes from
     * @return the original, stripped of an opening double-quote, if it was present, and
     *   stripped of a closing double-quote, if it was present.
     *
     */
    public static String unquoteString(final String original) {
        int start = 0;
        int end = original.length();

        // Remove surrounding double quotes, if present;
        // any other double quotes should not be removed.
        if ((end >= 1) && (original.charAt(0) == '"')) {
            start++;
        }
        if ((end >= 2) && (original.charAt(end - 1) == '"')) {
            end--;
        }

        return original.substring(start, end);
    }

    /**
     * Certain characters cannot be included in file or folder names.  We create files and folders
     * based on both information the user provides, and data about the actual episode.  It's likely
     * that sometimes illegal characters will occur.  This method takes a String that may have
     * illegal characters, and returns one that is similar but has no illegal characters.
     *
     * How illegal characters are handled actually depends on the particular character.  Some are
     * simply stripped away, others are replaced with a hyphen or apostrophe.
     *
     * This method operates only on the specified portion of the string, and ignores (strips away)
     * anything that comes before the start or after the end.
     *
     * @param title the original string, which may contain illegal characters
     * @param start the index of the first character to consider
     * @param end the index of the last character to consider
     * @return a version of the substring, from start to end, of the original string,
     *    which contains no illegal characters
     */
    public static String replaceIllegalCharacters(final String title, final int start, final int end) {
        StringBuilder sanitised = new StringBuilder(end + 1);
        for (int i = start; i <= end; i++) {
            char c = title.charAt(i);
            String replace = SANITISE.get(c);
            if (replace == null) {
                sanitised.append(c);
            } else {
                sanitised.append(replace);
            }
        }
        return sanitised.toString();
    }

    public static String replaceIllegalCharacters(final String title) {
        return replaceIllegalCharacters(title, 0, title.length() - 1);
    }

    public static String sanitiseTitle(String title) {
        // We don't only replace illegal characters; we also want to "trim" the string of whitespace
        // at the front and back, but not in the middle.  We'll accomplish this by finding the limits
        // of the non-whitespace characters before we even create the StringBuilder, and use those as
        // the limits of the string.
        int end = title.length() - 1;
        while ((end > 0) && Character.isWhitespace(title.charAt(end))) {
            end--;
        }

        int i = 0;
        while ((i <= end) && Character.isWhitespace(title.charAt(i))) {
            i++;
        }

        return replaceIllegalCharacters(title, i, end);
    }

    /**
     * This method is intended to return true if the given string is composed
     * purely of entirely-lower-case words separated by hyphens.
     *
     * To do this, we say the following:<ul>
     * <li>it must contain a hyphen</li>
     * <li>it must not contain a capital letter (must be all lower case)</li>
     * <li>it must not contain whitespace
     * <ul><li>if there's whitespace, then it doesn't purely use hyphens
     *         as separators</li></ul></li>
     * <li>it must not contain the dot character
     * <ul><li>if there's a dot, then it probably doesn't purely use hyphens
     *         as separators</li></ul></li>
     * </ul>
     *
     * This is already pretty involved for such a simple thing, and it's still
     * not perfect.  But it's a pretty good heuristic.
     *
     * @param s
     *    the String to analyze
     * @return true if it's composed purely of entirely-lower-case words
     *    separated by hyphens; false otherwise
     */
    private static boolean isLowerCaseWithHyphens(String s) {
        boolean status = false;
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isUpperCase(c)) {
                return false;
            }
            if (c == '.') {
                return false;
            }
            if (Character.isWhitespace(c)) {
                return false;
            }
            if (c == '-') {
                status = true;
            }
        }
        return status;
    }

    public static String replacePunctuation(String s) {
        String rval = s;

        // The apostrophe is kind of unique, because it's usually found within a word, including
        // in show titles: "Bob's Burgers", "The Real O'Neals", "What's Happening", "Don't Trust..."
        // For these, replacing the apostrophe with a space confuses the database; it's much better
        // to simply remove the apostrophe.
        rval = rval.replaceAll("'", "");

        // A hyphen in the middle of a word also should not be broken up into two words.
        // But there's an exception; see doc of isLowerCaseWithHyphens.
        boolean allLower = isLowerCaseWithHyphens(s);
        if (allLower) {
            rval = rval.replaceAll("(\\p{Lower})-(\\p{Lower})", "$1 $2");
        } else {
            rval = rval.replaceAll("(\\p{Lower})-(\\p{Lower})", "$1$2");
        }

        // transform "CamelCaps" => "Camel Caps"
        rval = rval.replaceAll("(\\p{Lower})([\\p{Upper}\\p{Digit}])", "$1 $2");

        // example: "30Rock" => "30 Rock"
        rval = rval.replaceAll("(\\p{Digit})([\\p{Upper}])", "$1 $2");

        // borrowed from http://stackoverflow.com/a/17099039
        // condenses acronyms (".S.H.I.E.L.D." -> " SHIELD")
        rval = rval.replaceAll("(?<=(^|[. ])[\\S&&\\D])[.](?=[\\S&&\\D]([.]|$))", "");

        // Replaces most remaining punctuation with spaces

        // The first few characters in the character class (hyphen, dot, underscore)
        // are likely to be used as separator characters.  The colon, question mark,
        // and exclamation point seem reasonable to appear in show names.  Most of the
        // rest are very unlikely to appear, and probably don't need to be handled at
        // all.  But this is basically the longstanding behavior, so let's just leave
        // it like this unless and until we have a specific reason to change.
        rval = rval.replaceAll("[-._!?$\\[:,;\\\\#%=@`\"\\]}{~><^/+|*]", " ");

        // Note, punctuation NOT modified, just left in place: parentheses, ampersand

        // get rid of superfluous whitespace
        rval = rval.replaceAll(" [ ]+", " ").trim();

        return rval;
    }

    /**
     * Reverse the effect of encodeUrlCharacters
     *
     * @param input
     *            string to decode
     * @return human-friendly representation of input
     */
    public static String decodeUrlCharacters(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }

        String rval = input.replaceAll("%20", " ");
        rval = rval.replaceAll("%25", "&");

        return rval;
    }

    /**
     * Replaces URL metacharacters with ASCII hex codes
     *
     * @param input
     *            string to encode
     * @return URL-safe representation of input
     */
    public static String encodeUrlCharacters(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }

        String rval = input.replaceAll(" ", "%20");
        rval = rval.replaceAll("&", "%25");

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
     *         include case normalization, removal of superfluous whitespace and
     *         punctuation, etc.
     */
    public static String makeQueryString(String text) {
        return toLower(encodeUrlCharacters(replacePunctuation(text)));
    }

    /**
     * Given a String representing a filename, extract and return the portion
     * that represents the "file extension", also known as the "suffix".
     *
     * This method expects to receive just the filename, i.e., not the path, just
     * the name of the actual file.  However, as long as the filename does have an
     * extension, it would work fine with a path, as well.
     *
     * In our implementation, the "extension" includes the leading "dot" character.
     * There's no purpose to obtaining the extension without the dot. The only place
     * we use the extension is when we're constructing a new filename, and in that
     * case, we would need to keep the dot, so we'd strip it away just to re-add it
     * later. What would be the point of doing so?
     *
     * Beyond that, in a general sense, it's nice to think of file paths as composed
     * of three parts: the parent folder, the basename and the extension. If the
     * extension doesn't contain the dot, then we either think of it as four parts
     * (parent, basename, dot, extension) or that the basename contains the dot,
     * which clearly doesn't make any sense.
     *
     * After the leading dot, a file extension cannot contain another dot.  In some
     * contexts, people may perceive the "extension" differently.  For example, in
     * "foo.tar.gz", some might say that the extension is ".tar.gz".  But we have no
     * reason to expect any justifiably double- suffixed files, and in our
     * filenames, the dot is a commonly used separator character
     * ("show.1x01.title.avi").  So we clearly must use only the last part.
     *
     * In the case of a filename without a dot, this simply returns the empty string.
     *
     * @param filename
     *            the String giving the filename; should be just the last element of
     *            the path, but not strictly necessary.  Does not need to refer to
     *            an existing file.
     * @return the "file extension": the last dot found, plus any text that comes
     *         after it.
     */
    public static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) {
            return filename.substring(dot);
        }
        return "";
    }

    /**
     * Formats the given file size into a nice string (123 Bytes, 10.6 kB,
     * 1.2 MB).  Copied from gjt I/O library.
     *
     * @param length The size
     * @return a formatted string
     * @since jEdit 4.4pre1
     */
    public static String formatFileSize(long length) {
        if (length < 1024) {
            return length + " Bytes";
        } else if (length < 1024 << 10) {
            return KB_FORMAT.get().format((double)length / 1024);
        } else {
            return MB_FORMAT.get().format((double)length / 1024 / 1024);
        }
    }

    public static String zeroPadTwoDigits(int number) {
        return String.format("%02d", number);
    }

    public static String zeroPadThreeDigits(int number) {
        return TWO_OR_THREE.get().format(number);
    }

    public static String formatDigits(int number) {
        return DIGITS.get().format(number);
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
     * Reverse the effect of encodeSpecialCharacters
     *
     * @param input
     *            string to decode
     * @return human-friendly representation of input
     */
    public static String decodeSpecialCharacters(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }

        input = input.replaceAll("&amp; ", "& ");

        // Don't encode string within xml data strings
        if (!input.startsWith("<?xml")) {
            input = input.replaceAll("%20", " ");
        }
        return input;
    }

    /**
     * Compares two strings, considering null equal to null.
     *
     * @param s1
     *            first string
     * @param s2
     *            second string
     * @return true if the strings are equal
     */
    public static boolean stringsAreEqual(String s1, String s2) {
        if (s1 == null) {
            return (s2 == null);
        }
        return s1.equals(s2);
    }

    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     * Copied from
     * <a href="http://preview.tinyurl.com/lzx3gzj">Apache Commons Lang StringUtils</a>
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
            //noinspection PointlessBooleanExpression
            if ((Character.isWhitespace(str.charAt(i)) == false)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
     * Copied from
     * <a href="http://preview.tinyurl.com/kvhh8oa">Apache Commons Lang StringUtils</a>
     *
     * @param str  the String to check, may be null
     * @return <code>true</code> if the String is not empty and not null and not whitespace
     */
    public static boolean isNotBlank(String str) {
        return !StringUtils.isBlank(str);
    }

    /**
     * Parses a numeric string and returns an integer value or null.
     *
     * @param strValue
     *            numeric string to parse
     * @return integer value of string, or null
     */
    public static Integer stringToInt(String strValue) {
        if (isNotBlank(strValue)) {
            try {
                BigDecimal bd = new BigDecimal(strValue);
                return bd.intValueExact();
            } catch (ArithmeticException | NumberFormatException e) {
                // not an integer
            }
        }
        return null;
    }
}
