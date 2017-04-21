/**
 * Constants.java -- the most important reason for this class to exist is for pieces of
 *    information that are shared throughout the program, so that if they should ever
 *    have to change, they can be changed in one place.
 *
 * Even for values that are used only once, though, it can be advantageous to give them
 * symbolic names rather than inlining the values directly into the code.  And it can
 * be nicer to consolidate all such values in one place, rather than cluttering up the
 * top of every file with its own values.
 *
 * Putting them in one place also allows us to see all the values "at once."  If this were
 * an application being developed for a private company, we might have some non-developer
 * reviewing all the words we present to the user (as well as all the files we might read
 * or create, etc.)  With an open-source project... well, we can still review them,
 * ourselves.  :-)
 *
 * But there's another reason for moving stuff here, and that's as a midway point to some
 * non-code solution.  Particularly if we ever want to localize the application, having
 * strings hard-coded into methods makes it pretty much impossible.  We'd want to get the
 * strings from resource files.
 *
 * It is not, of course, necessary to have this intermediate step; we could go straight from
 * inlined strings to resource files.  This just makes it easier to do it incrementally.
 *
 */

package org.tvrenamer.model.util;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class Constants {

    public static final Locale THIS_LOCALE = Locale.getDefault();

    public static final Charset TVR_CHARSET = Charset.forName("ISO-8859-1");

    public static final String APPLICATION_NAME = "TVRenamer";

    public static final String VERSION_NUMBER = Environment.readVersionNumber();

    public static final String TVRENAMER_PROJECT_URL = "http://tvrenamer.org";
    public static final String TVRENAMER_DOWNLOAD_URL = TVRENAMER_PROJECT_URL + "/downloads";
    public static final String TVRENAMER_ISSUES_URL = TVRENAMER_PROJECT_URL + "/issues";
    public static final String TVRENAMER_VERSION_URL = TVRENAMER_PROJECT_URL + "/version";

    public static final String XML_SUFFIX = ".xml";
    public static final String LOGGING_PROPERTIES = "/logging.properties";
    public static final String DEVELOPER_DEFAULT_OVERRIDES_FILENAME = "etc/default-overrides.xml";

    public static final String ERROR_LABEL = "Error";

    public static final String ERROR_PARSING_XML = "Error parsing XML";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";
    public static final String DEFAULT_IGNORED_KEYWORD = "sample";
    public static final String DEFAULT_LANGUAGE = "en";

    private static final String CONFIGURATION_DIRECTORY_NAME = ".tvrenamer";
    private static final String PREFERENCES_FILENAME = "prefs.xml";
    private static final String OVERRIDES_FILENAME = "overrides.xml";

    public static final String IMDB_BASE_URL = "http://www.imdb.com/title/";

    public static final Path USER_HOME_DIR = Paths.get(Environment.USER_HOME);
    public static final Path WORKING_DIRECTORY = Paths.get(Environment.USER_DIR);
    public static final Path TMP_DIR = Paths.get(Environment.TMP_DIR_NAME);

    public static final Path DEFAULT_DESTINATION_DIRECTORY = USER_HOME_DIR.resolve("TV");
    public static final Path CONFIGURATION_DIRECTORY = USER_HOME_DIR.resolve(CONFIGURATION_DIRECTORY_NAME);
    public static final Path PREFERENCES_FILE = CONFIGURATION_DIRECTORY.resolve(PREFERENCES_FILENAME);
    public static final Path OVERRIDES_FILE = CONFIGURATION_DIRECTORY.resolve(OVERRIDES_FILENAME);

    public static final Path PREFERENCES_FILE_LEGACY = USER_HOME_DIR.resolve("tvrenamer.preferences");
    public static final Path OVERRIDES_FILE_LEGACY = USER_HOME_DIR.resolve(".tvrenameroverrides");

    public static final String EMPTY_STRING = "";
}
