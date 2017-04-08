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

    public static final String ERROR_LABEL = "Error";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";
    public static final String DEFAULT_IGNORED_KEYWORD = "sample";

    private static final String CONFIGURATION_DIRECTORY_NAME = ".tvrenamer";
    private static final String PREFERENCES_FILENAME = "prefs.xml";
    private static final String OVERRIDES_FILENAME = "overrides.xml";

    public static final String DEVELOPER_DEFAULT_OVERRIDES_FILENAME = "etc/default-overrides.xml";

    public static final Path USER_HOME_DIR = Paths.get(Environment.USER_HOME);
    public static final Path WORKING_DIRECTORY = Paths.get(Environment.USER_DIR);

    public static final Path DEFAULT_DESTINATION_DIRECTORY = USER_HOME_DIR.resolve("TV");
    public static final Path CONFIGURATION_DIRECTORY = USER_HOME_DIR.resolve(CONFIGURATION_DIRECTORY_NAME);
    public static final Path PREFERENCES_FILE = CONFIGURATION_DIRECTORY.resolve(PREFERENCES_FILENAME);
    public static final Path OVERRIDES_FILE = CONFIGURATION_DIRECTORY.resolve(OVERRIDES_FILENAME);

    public static final Path PREFERENCES_FILE_LEGACY = USER_HOME_DIR.resolve("tvrenamer.preferences");
    public static final Path OVERRIDES_FILE_LEGACY = USER_HOME_DIR.resolve(".tvrenameroverrides");

    public static final String EMPTY_STRING = "";
}
