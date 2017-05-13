package org.tvrenamer.controller.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.logging.Logger;

public class StringUtils {
    private static final Logger logger = Logger.getLogger(StringUtils.class.getName());

    private static final Locale THIS_LOCALE = Locale.getDefault();

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

    public static String makeDotTitle(String titleString) {
        String pass1 = titleString.replaceAll("(\\w)\\s+(\\w)", "$1.$2");
        String pass2 = pass1.replaceAll("(\\w)\\s+(\\w)", "$1.$2");
        @SuppressWarnings("UnnecessaryLocalVariable")
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
        return title.trim();
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

    @SuppressWarnings("WeakerAccess")
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
     *         include case normalization, removal of superfluous whitepsace and
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
