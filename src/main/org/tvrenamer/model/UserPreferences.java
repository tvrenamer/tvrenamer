package org.tvrenamer.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.logging.Logger;

import org.tvrenamer.controller.UserPreferencesChangeEvent;
import org.tvrenamer.controller.UserPreferencesChangeListener;
import org.tvrenamer.controller.UserPreferencesPersistence;
import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.util.Constants;
import org.tvrenamer.view.UIUtils;

public class UserPreferences extends Observable {
    private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

    public static File prefsFile = new File(System.getProperty("user.home") + File.separatorChar
                                            + Constants.PREFERENCES_FILE);

    private File destDir;
    private String seasonPrefix;
    private boolean seasonPrefixLeadingZero;
    private boolean moveEnabled;
    private boolean renameEnabled;
    private String renameReplacementMask;
    private ProxySettings proxy;
    private boolean checkForUpdates;
    private boolean recursivelyAddFolders;
    private List<String> ignoreKeywords;

    private final static UserPreferences INSTANCE = load();

    /**
     * UserPreferences constructor which uses the defaults from {@link Constants}
     */
    private UserPreferences() {
        super();

        this.destDir = new File(Constants.DEFAULT_DESTINATION_DIRECTORY);
        this.seasonPrefix = Constants.DEFAULT_SEASON_PREFIX;
        this.seasonPrefixLeadingZero = false;
        this.moveEnabled = false;
        this.renameEnabled = true;
        this.renameReplacementMask = Constants.DEFAULT_REPLACEMENT_MASK;
        this.proxy = new ProxySettings();
        this.checkForUpdates = true;
        this.recursivelyAddFolders = true;
        this.ignoreKeywords = new ArrayList<String>();
        this.ignoreKeywords.add("sample");

        ensurePath();
    }

    public static UserPreferences getInstance() {
        return INSTANCE;
    }

    /**
     * Load preferences from xml file
     */
    public static UserPreferences load() {
        // retrieve from file and update in-memory copy
        UserPreferences prefs = UserPreferencesPersistence.retrieve(prefsFile);

        if (prefs != null) {
            logger.finer("Sucessfully read preferences from: " + prefsFile.getAbsolutePath());
            logger.info("Sucessfully read preferences: " + prefs.toString());
        } else {

            // Look in the legacy location, if not, create new
            File legacyPrefsFile = new File(System.getProperty("user.home") + File.separatorChar
                                            + Constants.PREFERENCES_FILE_LEGACY);

            prefs = UserPreferencesPersistence.retrieve(legacyPrefsFile);

            if( prefs != null ) {
                logger.finer("Sucessfully read legacy preferences from: " + prefsFile.getAbsolutePath());
                logger.info("Sucessfully read legacy preferences: " + prefs.toString());

                // Delete the old file, then store into the new file
                legacyPrefsFile.delete();
                store(prefs);

                logger.info("Deleted legacy prefs file in favour of the new file");

            } else {
                prefs = new UserPreferences();
            }
        }

        // apply the proxy configuration
        if (prefs.getProxy() != null) {
            prefs.getProxy().apply();
        }

        prefs.ensurePath();

        // add observer
        prefs.addObserver(new UserPreferencesChangeListener());

        return prefs;
    }

    public static void store(UserPreferences prefs) {
        UserPreferencesPersistence.persist(prefs, prefsFile);
        logger.fine("Sucessfully saved/updated preferences");
    }

    /**
     * Sets the directory to move renamed files to. Must be an absolute path, and the entire path will be created if it
     * doesn't exist.
     *
     * @param dir
     * @return True if the path was created successfully, false otherwise.
     */
    public void setDestinationDirectory(String dir) throws TVRenamerIOException {
        if (hasChanged(this.destDir.getAbsolutePath(), dir)) {
            this.destDir = new File(dir);
            ensurePath();

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("destDir", dir));
        }
    }

    /**
     * Sets the directory to move renamed files to. The entire path will be created if it doesn't exist.
     *
     * @param dir
     * @return True if the path was created successfully, false otherwise.
     */
    public void setDestinationDirectory(File dir) throws TVRenamerIOException {
        if (hasChanged(this.destDir, dir)) {
            this.destDir = dir;
            ensurePath();

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("destDir", dir));
        }
    }

    /**
     * Gets the directory set to move renamed files to.
     *
     * @return File object representing the directory.
     */
    public File getDestinationDirectory() {
        return this.destDir;
    }

    public void setMovedEnabled(boolean moveEnabled) {
        if (hasChanged(this.moveEnabled, moveEnabled)) {
            this.moveEnabled = moveEnabled;

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("moveEnabled", moveEnabled));
        }
    }

    /**
     * Get the status of of move support
     *
     * @return true if selected destination exists, false otherwise
     */
    public boolean isMovedEnabled() {
        return this.moveEnabled;
    }

    public void setRenameEnabled(boolean renameEnabled) {
        if (hasChanged(this.renameEnabled, renameEnabled)) {
            this.renameEnabled = renameEnabled;

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("renameEnabled", renameEnabled));
        }
    }

    /**
     * Get the status of of rename support
     *
     * @return true if selected destination exists, false otherwise
     */
    public boolean isRenameEnabled() {
        return this.renameEnabled;
    }

    public void setRecursivelyAddFolders(boolean recursivelyAddFolders) {
        if(hasChanged(this.recursivelyAddFolders, recursivelyAddFolders)) {
            this.recursivelyAddFolders = recursivelyAddFolders;

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("recursivelyAddFolders", recursivelyAddFolders));
        }
    }

    /**
     * Get the status of recursively adding files within a directory
     *
     * @return true if adding subdirectories, false otherwise
     */
    public boolean isRecursivelyAddFolders() {
        return this.recursivelyAddFolders;
    }

    public void setIgnoreKeywords(List<String> ignoreKeywords) {
        if (hasChanged(this.ignoreKeywords, ignoreKeywords)) {
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

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("ignoreKeywordsRegex", ignoreKeywords));
        }
    }

    public List<String> getIgnoreKeywords() {
        return this.ignoreKeywords;
    }

    public void setSeasonPrefix(String prefix) {
        // Remove the displayed "
        prefix = prefix.replaceAll("\"", "");

        if (hasChanged(this.seasonPrefix, prefix)) {
            this.seasonPrefix = StringUtils.sanitiseTitle(prefix);

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("prefix", prefix));
        }
    }

    public String getSeasonPrefix() {
        return this.seasonPrefix;
    }

    public String getSeasonPrefixForDisplay() {
        return ("\"" + this.seasonPrefix + "\"");
    }

    public boolean isSeasonPrefixLeadingZero() {
        return this.seasonPrefixLeadingZero;
    }

    public void setSeasonPrefixLeadingZero(boolean seasonPrefixLeadingZero) {
        if (hasChanged(this.seasonPrefixLeadingZero, seasonPrefixLeadingZero)) {
            this.seasonPrefixLeadingZero = seasonPrefixLeadingZero;

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("seasonPrefixLeadingZero", seasonPrefixLeadingZero));
        }
    }

    public void setRenameReplacementString(String renameReplacementMask) {
        if (hasChanged(this.renameReplacementMask, renameReplacementMask)) {
            this.renameReplacementMask = renameReplacementMask;

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("renameReplacementMask", renameReplacementMask));
        }
    }

    public String getRenameReplacementString() {
        return renameReplacementMask;
    }

    public ProxySettings getProxy() {
        return proxy;
    }

    public void setProxy(ProxySettings proxy) {
        if (hasChanged(this.proxy, proxy)) {
            this.proxy = proxy;
            proxy.apply();

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("proxy", proxy));
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
        if (hasChanged(this.checkForUpdates, checkForUpdates)) {
            this.checkForUpdates = checkForUpdates;

            setChanged();
            notifyObservers(new UserPreferencesChangeEvent("checkForUpdates", checkForUpdates));
        }
    }

    /**
     * Create the directory if it doesn't exist.
     */
    public void ensurePath() {
        if (this != null && this.moveEnabled && !this.destDir.mkdirs()) {
            if (!this.destDir.exists()) {
                this.moveEnabled = false;
                String message = "Couldn't create path: '" + this.destDir.getAbsolutePath() + "'. Move is now disabled";
                logger.warning(message);
                UIUtils.showMessageBox(SWTMessageBoxType.ERROR, "Error", message);
            }
        }
    }

    @Override
    public String toString() {
        return "UserPreferences [destDir=" + destDir + ", seasonPrefix=" + seasonPrefix
            + ", moveEnabled=" + moveEnabled + ", renameEnabled=" + renameEnabled
            + ", renameReplacementMask=" + renameReplacementMask + ", proxy=" + proxy
            + ", checkForUpdates=" + checkForUpdates + ", setRecursivelyAddFolders=" + recursivelyAddFolders + "]";
    }

    private boolean hasChanged(Object originalValue, Object newValue) {
        return !originalValue.equals(newValue);
    }
}
