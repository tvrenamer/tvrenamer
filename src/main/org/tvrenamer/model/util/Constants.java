package org.tvrenamer.model.util;

import java.util.logging.Logger;

public class Constants {
    private static Logger logger = Logger.getLogger(Constants.class.getName());

    public static final String APPLICATION_NAME = "TVRenamer";

    public static final String VERSION_NUMBER = Environment.readVersionNumber();

    public static final String PREFERENCES_FILE = ".tvrenamer";

    public static final String PREFERENCES_FILE_LEGACY = "tvrenamer.preferences";

    public static final String OVERRIDES_FILE = ".tvrenameroverrides";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";

    public static final String DEFAULT_DESTINATION_DIRNAME = System.getProperty("user.home") + "/TV";

    public static final String DEFAULT_SEASON_PREFIX = "Season ";
}
