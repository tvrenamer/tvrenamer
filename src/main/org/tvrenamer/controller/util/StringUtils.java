package org.tvrenamer.controller.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.logging.Logger;

public class StringUtils {
    private static Logger logger = Logger.getLogger(StringUtils.class.getName());

    public static final ThreadLocal<DecimalFormat> DIGITS =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("##0");
            }
        };

    public static final ThreadLocal<DecimalFormat> TWO_OR_THREE =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#00");
            }
        };

    public static final ThreadLocal<DecimalFormat> KB_FORMAT =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#.# kB");
            }
        };

    public static final ThreadLocal<DecimalFormat> MB_FORMAT =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#.# MB");
            }
        };

    public static String makeDotTitle(String titleString) {
        String pass1 = titleString.replaceAll("(\\w)\\s+(\\w)", "$1.$2");
        String pass2 = pass1.replaceAll("(\\w)\\s+(\\w)", "$1.$2");
        String pass3 = pass2.replaceAll("\\s", "");
        return pass3;
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
     * @param s1
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
