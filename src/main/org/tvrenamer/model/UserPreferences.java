package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.UserPreferencesChangeListener;
import org.tvrenamer.controller.UserPreferencesPersistence;
import org.tvrenamer.controller.util.FileUtilities;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.view.UIUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserPreferences extends Observable {
    private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

    private String destDir;
    private String preloadFolder;
    private String seasonPrefix;
    private boolean seasonPrefixLeadingZero;
    private boolean moveEnabled;
    private boolean renameEnabled;
    private boolean removeEmptiedDirectories;
    private String renameReplacementMask;
    private ProxySettings proxy;
    private boolean checkForUpdates;
    private boolean recursivelyAddFolders;
    private List<String> ignoreKeywords;

    private static final UserPreferences INSTANCE = load();

    /**
     * Create the directory if it doesn't exist and we need it.
     */
    public void ensureDestDir() {
        if (!moveEnabled) {
            // It doesn't matter if the directory exists or not if move is not enabled.
            return;
        }

        Path destPath = Paths.get(destDir);

        String errorMessage;
        if (Files.exists(destPath)) {
            if (Files.isDirectory(destPath)) {
                // destPath already exists; we're all set.
                return;
            }

            // destDir exists but is not a directory.
            errorMessage = "Destination path exists but is not a directory: '"
                + destDir + "'. Move is now disabled";
            // fall through to failure at bottom
        } else if (FileUtilities.mkdirs(destPath)) {
            // we have successfully created the destination directory
            return;
        } else {
            errorMessage = "Couldn't create path: '"
                + destDir + "'. Move is now disabled";
            // fall through to failure
        }

        moveEnabled = false;
        logger.warning(errorMessage);
        UIUtils.showMessageBox(SWTMessageBoxType.ERROR, ERROR_LABEL, errorMessage);
    }

    /**
     * UserPreferences constructor which uses the defaults from {@link Constants}
     */
    private UserPreferences() {
        super();

        destDir = DEFAULT_DESTINATION_DIRECTORY.toString();
        preloadFolder = null;
        seasonPrefix = DEFAULT_SEASON_PREFIX;
        seasonPrefixLeadingZero = false;
        moveEnabled = false;
        renameEnabled = true;
        removeEmptiedDirectories = true;
        renameReplacementMask = DEFAULT_REPLACEMENT_MASK;
        proxy = new ProxySettings();
        checkForUpdates = true;
        recursivelyAddFolders = true;
        ignoreKeywords = new ArrayList<>();
        ignoreKeywords.add(DEFAULT_IGNORED_KEYWORD);

        ensureDestDir();
    }

    public static UserPreferences getInstance() {
        return INSTANCE;
    }

    /**
     * Deal with legacy files and set up
     */
    public static void initialize() {
        Path temp = null;
        logger.fine("configuration directory = " + CONFIGURATION_DIRECTORY.toAbsolutePath().toString());
        if (Files.exists(CONFIGURATION_DIRECTORY)) {
            // Older versions used the same name as a preferences file
            if (!Files.isDirectory(CONFIGURATION_DIRECTORY)) {
                try {
                    temp = Files.createTempDirectory(APPLICATION_NAME);
                } catch (Exception ioe) {
                    temp = null;
                }
                if ((temp == null) || Files.notExists(temp)) {
                    throw new RuntimeException("Could not create temp file");
                }
                try {
                    Files.delete(temp);
                    Files.move(CONFIGURATION_DIRECTORY, temp);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        if (Files.notExists(CONFIGURATION_DIRECTORY)) {
            try {
                Path created = Files.createDirectories(CONFIGURATION_DIRECTORY);
            } catch (Exception e) {
                throw new RuntimeException("Could not create configuration directory");
            }
        }
        if (temp != null) {
            try {
                Files.move(temp, PREFERENCES_FILE);
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                throw new RuntimeException("Could not rename old prefs file from "
                                           + temp + " to " + PREFERENCES_FILE);
            }
        }
        if (Files.exists(PREFERENCES_FILE_LEGACY)) {
            if (Files.exists(PREFERENCES_FILE)) {
                throw new RuntimeException("Found two legacy preferences files!!");
            } else {
                try {
                    Files.move(PREFERENCES_FILE_LEGACY, PREFERENCES_FILE);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        if (Files.notExists(OVERRIDES_FILE)) {
            if (Files.exists(OVERRIDES_FILE_LEGACY)) {
                try {
                    Files.move(OVERRIDES_FILE_LEGACY, OVERRIDES_FILE);
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                    throw new RuntimeException("Could not rename old overrides file from "
                                               + OVERRIDES_FILE_LEGACY + " to " + OVERRIDES_FILE);
                }
            } else {
                // Previously the GlobalOverrides class was hard-coded to write some
                // overrides to the file.  I don't think that's right, but to try to
                // preserve the default behavior, if the user doesn't have any other
                // overrides file, we'll try to copy one from the source code into
                // place.  If it doesn't work, so be it.
                Path defOver = Paths.get(DEVELOPER_DEFAULT_OVERRIDES_FILENAME);
                if (Files.exists(defOver)) {
                    try {
                        Files.copy(defOver, OVERRIDES_FILE);
                    } catch (IOException ioe) {
                        logger.info("unable to copy default overrides file.");
                    }
                }
            }
        }
    }

    /**
     * Load preferences from xml file
     */
    public static UserPreferences load() {
        initialize();

        // retrieve from file and update in-memory copy
        UserPreferences prefs = UserPreferencesPersistence.retrieve(PREFERENCES_FILE);

        if (prefs != null) {
            logger.fine("Sucessfully read preferences from: " + PREFERENCES_FILE.toAbsolutePath());
            logger.fine("Sucessfully read preferences: " + prefs.toString());
        } else {
            prefs = new UserPreferences();
        }

        // apply the proxy configuration
        if (prefs.getProxy() != null) {
            prefs.getProxy().apply();
        }

        prefs.ensureDestDir();

        // add observer
        // TODO: why do we do this?
        prefs.addObserver(new UserPreferencesChangeListener());

        return prefs;
    }

    public static void store(UserPreferences prefs) {
        UserPreferencesPersistence.persist(prefs, PREFERENCES_FILE);
        logger.fine("Sucessfully saved/updated preferences");
    }

    private void preferenceChanged(UserPreference preference) {
        setChanged();
        notifyObservers(preference);
        clearChanged();
    }


    /**
     * Simply the complement of equals(), but with the specific purpose of detecting
     * if the value of a preference has been changed.
     *
     * @param originalValue the value of the UserPreference before the dialog was opened
     * @param newValue the value of the UserPreference as set in the dialog
     * @return true if the values are different
     */
    private boolean valuesAreDifferent(Object originalValue, Object newValue) {
        return !originalValue.equals(newValue);
    }

    /**
     * Sets the directory to move renamed files to. Must be an absolute path, and the entire path will be created if it
     * doesn't exist.
     *
     * @param dir the path to the directory
     */
    public void setDestinationDirectory(String dir) throws TVRenamerIOException {
        if (valuesAreDifferent(destDir, dir)) {
            destDir = dir;
            ensureDestDir();

            preferenceChanged(UserPreference.DEST_DIR);
        }
    }

    /**
     * Gets the name of the directory to preload into the table.
     *
     * @return String naming the directory.
     */
    public String getPreloadFolder() {
        return preloadFolder;
    }

    /**
     * Gets the directory set to move renamed files to.
     *
     * @return name of the directory.
     */
    public String getDestinationDirectoryName() {
        return destDir;
    }

    public void setMoveEnabled(boolean moveEnabled) {
        if (valuesAreDifferent(this.moveEnabled, moveEnabled)) {
            this.moveEnabled = moveEnabled;

            preferenceChanged(UserPreference.MOVE_ENABLED);
        }
    }

    /**
     * Get the status of of move support
     *
     * @return true if selected destination exists, false otherwise
     */
    public boolean isMoveEnabled() {
        return moveEnabled;
    }

    public void setRenameEnabled(boolean renameEnabled) {
        if (valuesAreDifferent(this.renameEnabled, renameEnabled)) {
            this.renameEnabled = renameEnabled;

            preferenceChanged(UserPreference.RENAME_ENABLED);
        }
    }

    /**
     * Get the status of of rename support
     *
     * @return true if selected destination exists, false otherwise
     */
    public boolean isRenameEnabled() {
        return renameEnabled;
    }

    /**
     * Sets whether or not we want the FileMover to delete directories when their last
     * remaining contents have been moved away.
     *
     * @param removeEmptiedDirectories whether or not we want the FileMover to delete
     *               directories when their last remaining contents have been moved away.
     */
    public void setRemoveEmptiedDirectories(boolean removeEmptiedDirectories) {
        if (valuesAreDifferent(this.removeEmptiedDirectories, removeEmptiedDirectories)) {
            this.removeEmptiedDirectories = removeEmptiedDirectories;

            preferenceChanged(UserPreference.REMOVE_EMPTY);
        }
    }

    /**
     * Get whether or not we want the FileMover to delete directories when their last
     * remaining contents have been moved away.
     *
     * @return true if we want the FileMover to delete directories when their last
     *         remaining contents have been moved away.
     */
    public boolean isRemoveEmptiedDirectories() {
        return removeEmptiedDirectories;
    }

    public void setRecursivelyAddFolders(boolean recursivelyAddFolders) {
        if (valuesAreDifferent(this.recursivelyAddFolders, recursivelyAddFolders)) {
            this.recursivelyAddFolders = recursivelyAddFolders;

            preferenceChanged(UserPreference.ADD_SUBDIRS);
        }
    }

    /**
     * Get the status of recursively adding files within a directory
     *
     * @return true if adding subdirectories, false otherwise
     */
    public boolean isRecursivelyAddFolders() {
        return recursivelyAddFolders;
    }

    public void setIgnoreKeywords(List<String> ignoreKeywords) {
        if (valuesAreDifferent(this.ignoreKeywords, ignoreKeywords)) {
            this.ignoreKeywords.clear();
            for (String ignorable : ignoreKeywords) {
                // Be careful not to allow empty string as a "keyword."
                if (ignorable.length() > 1) {
                    // TODO: Convert commas into pipes for proper regex, remove periods
                    this.ignoreKeywords.add(ignorable);
                } else {
                    logger.warning("keywords to ignore must be at least two characters.");
                    logger.warning("not adding \"" + ignorable + "\"");
                }
            }

            preferenceChanged(UserPreference.IGNORE_REGEX);
        }
    }

    public List<String> getIgnoreKeywords() {
        return ignoreKeywords;
    }

    public void setSeasonPrefix(String prefix) {
        // Remove the displayed "
        prefix = prefix.replaceAll("\"", "");

        if (valuesAreDifferent(seasonPrefix, prefix)) {
            // TODO: rather than silently sanitising, we should probably
            // reject any text that has an illegal character in it.
            seasonPrefix = StringUtils.sanitiseTitle(prefix);

            preferenceChanged(UserPreference.SEASON_PREFIX);
        }
    }

    public String getSeasonPrefix() {
        return seasonPrefix;
    }

    public String getSeasonPrefixForDisplay() {
        return ("\"" + seasonPrefix + "\"");
    }

    public boolean isSeasonPrefixLeadingZero() {
        return seasonPrefixLeadingZero;
    }

    public void setSeasonPrefixLeadingZero(boolean seasonPrefixLeadingZero) {
        if (valuesAreDifferent(this.seasonPrefixLeadingZero, seasonPrefixLeadingZero)) {
            this.seasonPrefixLeadingZero = seasonPrefixLeadingZero;

            preferenceChanged(UserPreference.LEADING_ZERO);

        }
    }

    public void setRenameReplacementString(String renameReplacementMask) {
        if (valuesAreDifferent(this.renameReplacementMask, renameReplacementMask)) {
            this.renameReplacementMask = renameReplacementMask;

            preferenceChanged(UserPreference.REPLACEMENT_MASK);
        }
    }

    public String getRenameReplacementString() {
        return renameReplacementMask;
    }

    public ProxySettings getProxy() {
        return proxy;
    }

    public void setProxy(ProxySettings proxy) {
        if (valuesAreDifferent(this.proxy, proxy)) {
            this.proxy = proxy;
            proxy.apply();

            preferenceChanged(UserPreference.PROXY);
        }
    }

    /**
     * @return the checkForUpdates
     */
    public boolean checkForUpdates() {
        return checkForUpdates;
    }

    /**
     * @param checkForUpdates the checkForUpdates to set
     */
    public void setCheckForUpdates(boolean checkForUpdates) {
        if (valuesAreDifferent(this.checkForUpdates, checkForUpdates)) {
            this.checkForUpdates = checkForUpdates;

            preferenceChanged(UserPreference.UPDATE_CHECK);
        }
    }

    @Override
    public String toString() {
        return "UserPreferences [destDir=" + destDir + ", seasonPrefix=" + seasonPrefix
            + ", moveEnabled=" + moveEnabled + ", renameEnabled=" + renameEnabled
            + ", renameReplacementMask=" + renameReplacementMask + ", proxy=" + proxy
            + ", checkForUpdates=" + checkForUpdates + ", setRecursivelyAddFolders=" + recursivelyAddFolders + "]";
    }
}
