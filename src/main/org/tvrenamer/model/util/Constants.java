package org.tvrenamer.model.util;

import java.nio.file.Path;
import java.nio.file.Paths;

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
public class Constants {

    public static final String APPLICATION_NAME = "TVRenamer";
    public static final String ABOUT_LABEL = "About " + APPLICATION_NAME;
    public static final String TVRENAMER_DESCRIPTION = APPLICATION_NAME
        + " is a Java GUI utility to rename TV episodes from TV listings";

    public static final String VERSION_NUMBER = Environment.readVersionNumber();
    public static final String VERSION_LABEL = "Version: " + VERSION_NUMBER;

    public static final String TVRENAMER_PROJECT_URL = "http://tvrenamer.org";
    public static final String TVRENAMER_DOWNLOAD_URL = TVRENAMER_PROJECT_URL + "/downloads";
    public static final String TVRENAMER_ISSUES_URL = TVRENAMER_PROJECT_URL + "/issues";
    public static final String TVRENAMER_VERSION_URL = TVRENAMER_PROJECT_URL + "/version";
    public static final String TVRENAMER_REPOSITORY_URL = TVRENAMER_PROJECT_URL + "/source";
    public static final String TVRENAMER_SUPPORT_EMAIL = "support@tvrenamer.org";
    public static final String TVRENAMER_LICENSE_URL = "http://www.gnu.org/licenses/gpl-2.0.html";

    public static final String EMAIL_LINK = "mailto:" + TVRENAMER_SUPPORT_EMAIL;

    public static final String LICENSE_TEXT_1 = "Licensed under the ";
    public static final String LICENSE_TEXT_2 = "GNU General Public License v2";
    public static final String PROJECT_PAGE = "Project Page";
    public static final String ISSUE_TRACKER = "Issue Tracker";
    public static final String SEND_SUPPORT_EMAIL = "Send support email";
    public static final String SOURCE_CODE_LINK = "Source Code";

    public static final String XML_SUFFIX = ".xml";
    public static final String ICON_PARENT_DIRECTORY = "res";
    public static final String TVRENAMER_ICON_PATH = "/icons/tvrenamer.png";
    public static final String TVRENAMER_ICON_DIRECT_PATH =
        ICON_PARENT_DIRECTORY + TVRENAMER_ICON_PATH;
    public static final String LOGGING_PROPERTIES = "/logging.properties";
    public static final String DEVELOPER_DEFAULT_OVERRIDES_FILENAME = "etc/default-overrides.xml";

    public static final String QUIT_LABEL = "Quit";
    public static final String CANCEL_LABEL = "Cancel";
    public static final String SAVE_LABEL = "Save";
    public static final String ERROR_LABEL = "Error";
    public static final String EXIT_LABEL = "Exit";
    public static final String OK_LABEL = "OK";
    public static final String PREFERENCES_LABEL = "Preferences";
    public static final String FILE_MOVE_THREAD_LABEL = "MoveRunnerThread";
    public static final String RENAME_LABEL = "Rename Selected";
    public static final String JUST_MOVE_LABEL = "Move Selected";
    public static final String RENAME_AND_MOVE = "Rename && Move Selected";
    public static final String MOVE_HEADER = "Proposed File Path";
    public static final String RENAME_HEADER = "Proposed File Name";
    public static final String REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX = "(.*) :.*";
    public static final String IGNORE_WORDS_SPLIT_REGEX = "\\s*,\\s*";
    public static final String GENERAL_LABEL = "General";
    public static final String RENAMING_LABEL = "Renaming";
    public static final String MOVE_ENABLED_TEXT = "Move Enabled [?]";
    public static final String DEST_DIR_TEXT = "TV Directory [?]";
    public static final String DEST_DIR_BUTTON_TEXT = "Select directory";
    public static final String DIR_DIALOG_TEXT = "Please select a directory and click OK";
    public static final String SEASON_PREFIX_TEXT = "Season Prefix [?]";
    public static final String SEASON_PREFIX_ZERO_TEXT = "Season Prefix Leading Zero [?]";
    public static final String IGNORE_LABEL_TEXT = "Ignore files containing [?]";
    public static final String RECURSE_FOLDERS_TEXT = "Recursively add shows in subdirectories";
    public static final String CHECK_UPDATES_TEXT = "Check for Updates at startup";
    public static final String RENAME_TOKEN_TEXT = "Rename Tokens [?]";
    public static final String RENAME_FORMAT_TEXT = "Rename Format [?]";
    public static final String RENAME_ENABLED_TOOLTIP = "Whether the 'rename' functionality is enabled.\n"
        + "You can move a file into a folder based on its show\n"
        + "without actually renaming the file";
    public static final String HELP_TOOLTIP = "Hover mouse over [?] to get help";
    public static final String GENERAL_TOOLTIP = " - TVRenamer will automatically move the files "
        + "to your 'TV' folder if you want it to.  \n"
        + " - It will move the file to <tv directory>/<series name>/<season prefix> #/ \n"
        + " - Once enabled, set the location below.";
    public static final String MOVE_ENABLED_TOOLTIP = "Whether the "
        + "'move to TV location' functionality is enabled";
    public static final String DEST_DIR_TOOLTIP = "The location of your 'TV' folder";
    public static final String PREFIX_TOOLTIP = " - The prefix of the season when renaming and "
        + "moving the file.  It is usually \"Season \" or \"s'\".\n - If no value is entered "
        + "(or \"\"), the season folder will not be created, putting all files in the series name "
        + "folder\n - The \" will not be included, just displayed here to show whitespace";
    public static final String SEASON_PREFIX_ZERO_TOOLTIP = "Whether to have a leading zero "
        + "in the season prefix";
    public static final String IGNORE_LABEL_TOOLTIP = "Provide comma separated list of words "
        + "that will cause a file to be ignored if they appear in the file's path or name.";
    public static final String RENAME_TOKEN_TOOLTIP = " - These are the possible tokens to "
        + " make up the 'Rename Format' below.\n"
        + " - You can drag and drop tokens to the 'Rename Format' text box below";
    public static final String RENAME_FORMAT_TOOLTIP = "The result of the rename, with the "
        + "tokens being replaced by the meaning above";
    public static final String CANT_CREATE_DEST = "Unable to create the destination directory";
    public static final String MOVE_NOW_DISABLED = "Move is now disabled.";
    public static final String MOVE_INTRO = "Clicking this button will ";
    public static final String AND_RENAME = "rename and ";
    public static final String INTRO_MOVE_DIR = "move the selected files to the directory "
        + "set in preferences (currently ";
    public static final String FINISH_MOVE_DIR = ").";
    public static final String RENAME_TOOLTIP = "Clicking this button will rename the selected "
        + "files but leave them where they are.";
    public static final String NO_ACTION_TOOLTIP = "You have selected not to move files, "
        + "and not to rename them, either.  Therefore, there's no action to be taken.  "
        + "Open the Preferences dialog and enable \"Move\", \"Rename\", or both, in order "
        + "to take some action.";
    public static final String UNKNOWN_EXCEPTION = "An error occurred, please check "
        + "the console output to see any errors:";

    public static final String UPDATE_TEXT = "Check for Updates...";
    public static final String NEW_VERSION_TITLE = "New Version Available!";
    public static final String NEW_VERSION_AVAILABLE = "There is a new version available!\n\n"
        + "You are currently running " + VERSION_NUMBER + ", but there is an update available\n\n"
        + "Please visit " + TVRENAMER_PROJECT_URL + " to download the new version.";
    public static final String NO_NEW_VERSION_TITLE = "No New Version Available";
    public static final String NO_NEW_VERSION_AVAILABLE = "There is no new version available\n\n"
        + "Please check the website (" + TVRENAMER_PROJECT_URL + ") for any news or check back later.";
    public static final String TO_DOWNLOAD = "Please visit " + TVRENAMER_PROJECT_URL
        + " to download the new version.";
    public static final String GET_UPDATE_MESSAGE = "This version of TVRenamer is no longer "
        + "functional.  There is a new version available, which should work. "
        + TO_DOWNLOAD;
    public static final String NEED_UPDATE = "This version of TVRenamer is no longer "
        + "functional.  There is a not currently a new version available, but please "
        + "check " + TVRENAMER_PROJECT_URL + " to see when one comes available.";
    public static final String API_DISCONTINUED = "API apparently discontinued";

    public static final String ERROR_PARSING_XML = "Error parsing XML";
    public static final String ERROR_PARSING_NUMBERS = ERROR_PARSING_XML
        + ": a field expected to be a number was not";
    public static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    public static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to find show information";
    public static final String DOWNLOADING_FAILED = "Downloading show listings failed";
    public static final String DOWNLOADING_FAILED_MESSAGE = DOWNLOADING_FAILED
        + ".  Check internet connection";
    public static final String FILE_EPISODE_NEEDS_PATH = "cannot create FileEpisode with no path";

    public static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%0e] %t";
    public static final String DEFAULT_SEASON_PREFIX = "Season ";
    public static final String DEFAULT_IGNORED_KEYWORD = "sample";
    public static final String VERSION_SEPARATOR_STRING = "~";
    public static final String DUPLICATES_DIRECTORY = "versions";
    public static final String DEFAULT_LANGUAGE = "en";

    private static final String CONFIGURATION_DIRECTORY_NAME = ".tvrenamer";
    private static final String PREFERENCES_FILENAME = "prefs.xml";
    private static final String OVERRIDES_FILENAME = "overrides.xml";

    public static final String IMDB_BASE_URL = "http://www.imdb.com/title/";

    @SuppressWarnings("WeakerAccess")
    public static final Path USER_HOME_DIR = Paths.get(Environment.USER_HOME);
    public static final Path WORKING_DIRECTORY = Paths.get(Environment.USER_DIR);
    public static final Path TMP_DIR = Paths.get(Environment.TMP_DIR_NAME);

    public static final Path DEFAULT_DESTINATION_DIRECTORY = USER_HOME_DIR.resolve("TV");
    public static final Path CONFIGURATION_DIRECTORY = USER_HOME_DIR.resolve(CONFIGURATION_DIRECTORY_NAME);
    public static final Path PREFERENCES_FILE = CONFIGURATION_DIRECTORY.resolve(PREFERENCES_FILENAME);
    public static final Path OVERRIDES_FILE = CONFIGURATION_DIRECTORY.resolve(OVERRIDES_FILENAME);

    public static final Path PREFERENCES_FILE_LEGACY = USER_HOME_DIR.resolve("tvrenamer.preferences");
    public static final Path OVERRIDES_FILE_LEGACY = USER_HOME_DIR.resolve(".tvrenameroverrides");

    @SuppressWarnings("unused")
    public static final String EMPTY_STRING = "";
}
