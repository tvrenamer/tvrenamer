package org.tvrenamer.controller.util;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class StringUtils {
    private static final Logger logger = Logger.getLogger(StringUtils.class.getName());

    private static final Locale THIS_LOCALE = Locale.getDefault();

    public static final Map<Character, String> SANITISE
        = Collections.unmodifiableMap(new HashMap<Character, String>()
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
        });
    public static final Set<Character> ILLEGAL_CHARACTERS = SANITISE.keySet();

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

    /**
     * Simply returns the given String rendered in all lower-case letters.<p>
     *
     * The differences between this and just calling <code>toLowerCase</code> are:
     * <ul><li>this takes null and returns the empty String</li>
     *     <li>this provides the current locale for lower-casing</li></ul>
     *
     * @param orig
     *    the String to lower-case
     * @return
     *    the String, lower-cased in this locale
     */
    public static String toLower(String orig) {
        if (orig == null) {
            return "";
        }
        return orig.toLowerCase(THIS_LOCALE);
    }

    /**
     * Simply returns the given String rendered in all upper-case letters.<p>
     *
     * The differences between this and just calling <code>toUpperCase</code> are:
     * <ul><li>this takes null and returns the empty String</li>
     *     <li>this provides the current locale for upper-casing</li></ul>
     *
     * @param orig
     *    the String to upper-case
     * @return
     *    the String, upper-cased in this locale
     */
    @SuppressWarnings("unused")
    public static String toUpper(String orig) {
        if (orig == null) {
            return "";
        }
        return orig.toUpperCase(THIS_LOCALE);
    }

    /**
     * Creates an ASCII String from a byte array, without throwing an exception.<p>
     *
     * If an exception is thrown by the String constructor, catches it and simply
     * returns the empty string.
     *
     * @param buffer
     *    a byte array, where the bytes are to be interpreted as ASCII character codes
     * @return
     *    a String made from the character codes in the buffer, or the empty String
     *    if there was a problem (such as, not all the bytes are ASCII codes)
     */
    public static String makeString(byte[] buffer) {
        String rval;
        try {
            rval = new String(buffer, "ASCII");
        } catch (UnsupportedEncodingException uee) {
            rval = "";
        }
        return rval;
    }

    /**
     * Returns a string that uses dots, not whitespace, to separate words.<p>
     *
     * The original version of this functionality was to simply replace the space
     * character with the dot character.  That has the advantage of being very
     * simple and clear.  But it gave less than optimal results.<p>
     *
     * The input String may contain punctuation, and that punctuation should be
     * retained.  When whitespace is found around punctuation, it is better to just
     * remove it, than replace it with a dot.  The punctuation (which may be a dot
     * to begin with) serves as a separator.<p>
     *
     * For a real example, there was an episode of <i>Beavis and Butthead</i> called,
     * "<code>B &amp; B's B'n B</code>".  We don't want to turn that into
     * "<code>B.&amp;.B's.B'n.B</code>".  The dots around the ampersand don't help.
     * There may not be a perfect answer that pleases everyone, but this method turns it
     * into "<code>B&amp;B's.B'n.B</code>".<p>
     *
     * To sum it all up, we simply want to do this:<ul>
     *  <li>replace whitespace between words with a single dot</li>
     *  <li>remove any other whitespace (next to punctuation or at beginning/end)
     *  </li></ul><p>
     *
     * The trick is to use the "word boundary" regular expression, \b.  We do
     * two passes, essentially.  In the first pass, we replace any amount of
     * whitespace between two "word boundaries" with a single dot, and then
     * in the second pass, we simply replace any remaining whitespace with the
     * empty string (i.e., remove it).<p>
     *
     * If this still isn't clear, perhaps looking at the tests in testDotTitle,
     * in StringUtilsTest.java, will show what this is intended to do.
     *
     * @param titleString
     *   the String which uses whitespace to separate words
     * @return
     *   a version of the titleString which uses punctuation to separate words,
     *   inserting dots in cases where there previously was only whitespace
     */
    public static String makeDotTitle(final String titleString) {
        return titleString.replaceAll("\\b\\s+\\b", ".").replaceAll("\\s", "");
    }

    /**
     * Removes the last instance of one string, from a larger one.<p>
     *
     * Does not report an error or do anything else to raise an issue if the search
     * string is not found.  Simply returns the original string in that case.
     *
     * @param input
     *    the String to search and edit
     * @param match
     *    the String to search for and remove
     * @return
     *    a version of input that removes the last instance of match, if match was found
     */
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
     * Strip away double-quote characters at the beginning and end of a string.<p>
     *
     * Returns a string identical to the original, except that if the first and/or last
     * character of the original was a double-quote character, it is omitted.  The quotes
     * do not have to be balanced.  If there is one at the beginning but not the end, it's
     * removed.  Or the end but not the beginning, or both.<p>
     *
     * Any double-quote characters that may occur in the middle of the string are untouched.
     * This would even include a situation where the string begins with two double quotes.
     * It is not analogous to String.trim() in that sense.  It strips at most one character
     * from the beginning, and at most one from the end.<p>
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
     * Return whether or not the given character is legal in filenames.
     *
     * @param ch the character to check
     * @return true if the character is ok to include in filenames, false if it is not
     */
    public static boolean isLegalFilenameCharacter(final char ch) {
        return !ILLEGAL_CHARACTERS.contains(ch);
    }

    /**
     * Trim a string, presumably a "foundShow" from a filename, of extraneous characters.
     *
     * @param extracted
     *    the String to trim
     * @return a version of the given string with all leading and trailing separator
     *    characters (space, underscore, dot, hyphen) removed.
     */
    public static String trimFoundShow(final String extracted) {
        return extracted.replaceFirst("^[ _.-]+", "").replaceFirst("[ _.-]+$", "");
    }

    /*
     * See javadoc for public, 1-arg-version, below.
     *
     * This helper method operates only on the specified portion of the string, and ignores
     * (strips away) anything that comes before the start or after the end.
     *
     * @param start the index of the first character to consider
     * @param end the index of the last character to consider
     * @return a version of the substring, from start to end, of the original string,
     *    which contains no illegal characters
     */
    private static String replaceIllegalCharacters(final String title, final int start, final int end) {
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

    /**
     * Replace characters which are not permitted in file paths.<p>
     *
     * Certain characters cannot be included in file or folder names.  We create files and folders
     * based on both information the user provides, and data about the actual episode.  It's likely
     * that sometimes illegal characters will occur.  This method takes a String that may have
     * illegal characters, and returns one that is similar but has no illegal characters.<p>
     *
     * How illegal characters are handled actually depends on the particular character.  Some are
     * simply stripped away, others are replaced with a hyphen or apostrophe.<p>
     *
     * @param title the original string, which may contain illegal characters
     * @return a version of the original string which contains no illegal characters
     */
    public static String replaceIllegalCharacters(final String title) {
        return replaceIllegalCharacters(title, 0, title.length() - 1);
    }

    /**
     * Transform a String into something suitable for a filename.<p>
     *
     * Uses {@link #replaceIllegalCharacters}, to get rid of problematic characters; but does a bit
     * more.  We also want to "trim" the string of whitespace at the front and back, but not in the
     * middle.  We'll accomplish this by finding the limits of the non-whitespace characters before
     * we even create the StringBuilder, and use those as the limits of the string.
     *
     * @param title
     *    the proposed String to be used for a filename
     * @return
     *    a version of the given string that is more suitable as a filename
     * @see #replaceIllegalCharacters(String)
     */
    public static String sanitiseTitle(String title) {
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
     * purely of entirely-lower-case words separated by hyphens.<p>
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
     * </ul><p>
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

    /**
     * Transforms a substring from a filename into something more like what a person
     * would type.<p>
     *
     * The limitations that file system put on filenames often mean that, while the
     * show is identified in the filename of a video file, it may be compressed,
     * abbreviated, or otherwise modified, in a way that makes it harder for the
     * database APIs to match the modified string to an actual show.<p>
     *
     * (The name is not very accurate, in that it does a lot more than just replace
     * punctuation.)<p>
     *
     * Perhaps the best way to understand what this does, and what it tries to do,
     * is to look at testReplacePunctuation() in StringUtilsTest.java.<p>
     *
     * @param s
     *   a String, presumably representing the part of a filename that we have identified as
     *   naming a show
     * @return
     *   a version of the string that is (we hope) much more suitable for feeding to a query API
     */
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
    @SuppressWarnings("unused")
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
     * use for the query.<p>
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
        return toLower(replacePunctuation(text));
    }

    /**
     * Given a String representing a filename, extract and return the portion
     * that represents the "file extension", also known as the "suffix".<p>
     *
     * This method expects to receive just the filename, i.e., not the path, just
     * the name of the actual file.  However, as long as the filename does have an
     * extension, it would work fine with a path, as well.<p>
     *
     * In our implementation, the "extension" includes the leading "dot" character.
     * There's no purpose to obtaining the extension without the dot. The only place
     * we use the extension is when we're constructing a new filename, and in that
     * case, we would need to keep the dot, so we'd strip it away just to re-add it
     * later. What would be the point of doing so?<p>
     *
     * Beyond that, in a general sense, it's nice to think of file paths as composed
     * of three parts: the parent folder, the basename and the extension. If the
     * extension doesn't contain the dot, then we either think of it as four parts
     * (parent, basename, dot, extension) or that the basename contains the dot,
     * which clearly doesn't make any sense.<p>
     *
     * After the leading dot, a file extension cannot contain another dot.  In some
     * contexts, people may perceive the "extension" differently.  For example, in
     * "foo.tar.gz", some might say that the extension is ".tar.gz".  But we have no
     * reason to expect any justifiably double- suffixed files, and in our
     * filenames, the dot is a commonly used separator character
     * ("show.1x01.title.avi").  So we clearly must use only the last part.<p>
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
     * Formats the given file size into a nice string (123 Bytes, 10.6 kB, 1.2 MB).<p>
     *
     * Copied from gjt I/O library.
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
     * Reverse the effect of {@link #encodeSpecialCharacters}
     *
     * @param input
     *            string to decode
     * @return human-friendly representation of input
     */
    @SuppressWarnings("unused")
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
     * Checks if a String is whitespace, empty ("") or null.<p>
     *
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
     * Checks if a String is not empty (""), not null and not whitespace only.<p>
     *
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
