package com.google.code.tvrenamer.model;

import java.io.File;
import java.util.Observable;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.XMLPersistence;
import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.view.UIUtils;

public class UserPreferences extends Observable {	
	private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

	private File destDir;
	private String seasonPrefix;
	private boolean moveEnabled = false;
	private String renameReplacementMask;
	private static File prefsFile = new File(System.getProperty("user.dir") + File.separatorChar
	                                         + Constants.PREFERENCES_FILE);
	private ProxySettings proxy;

	/**
	 * UserPreferences constructor which uses the defaults from {@link Constants}
	 */
	public UserPreferences() {
		super();

		this.destDir = new File(Constants.DEFAULT_DESTINATION_DIRECTORY);
		this.seasonPrefix = Constants.DEFAULT_SEASON_PREFIX;
		this.renameReplacementMask = Constants.DEFAULT_REPLACEMENT_MASK;
		this.proxy = new ProxySettings();

		ensurePath();

		this.addObserver(new UserPreferencesListener());
	}

	/**
	 * Load preferences from xml file
	 */
	public UserPreferences load() {
		// retrieve from file and update in-memory copy
		UserPreferences prefs = XMLPersistence.retrieve(prefsFile);

		// apply the proxy configuration
		if(prefs.proxy != null) {
			prefs.proxy.apply();
		}		

		if (prefs != null) {
			logger.finer("Sucessfully read preferences from: " + prefsFile.getAbsolutePath());
			logger.info("Sucessfully read preferences: " + prefs.toString());
		}

		ensurePath();

		return prefs;
	}

	public void store() {
		XMLPersistence.persist(this, prefsFile);
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
		if(hasChanged(this.destDir.getAbsolutePath(), dir)) {
			setChanged();
			notifyObservers(new UserPreferencesChangeEvent("destDir", dir));

			this.destDir = new File(dir);
			ensurePath();
		}
	}

	/**
	 * Sets the directory to move renamed files to. The entire path will be created if it doesn't exist.
	 * 
	 * @param dir
	 * @return True if the path was created successfully, false otherwise.
	 */
	public void setDestinationDirectory(File dir) throws TVRenamerIOException {
		if(hasChanged(this.destDir, dir)) {
			setChanged();
			notifyObservers(new UserPreferencesChangeEvent("destDir", dir));

			this.destDir = dir;
			ensurePath();
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
		if(hasChanged(this.moveEnabled, moveEnabled)) {
			setChanged();
			notifyObservers(new UserPreferencesChangeEvent("moveEnabled", moveEnabled));

			this.moveEnabled = moveEnabled;
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

	public void setSeasonPrefix(String prefix) {
		// Remove the displayed "
		prefix = prefix.replaceAll("\"", "");

		if(hasChanged(this.seasonPrefix, prefix)) {
			setChanged();
			notifyObservers(new UserPreferencesChangeEvent("prefix", prefix));

			this.seasonPrefix = StringUtils.sanitiseTitle(prefix);
		}
	}

	public String getSeasonPrefix() {
		return this.seasonPrefix;
	}

	public String getSeasonPrefixForDisplay() {
		return ("\"" + this.seasonPrefix + "\"");
	}

	public void setRenameReplacementString(String renameReplacementMask) {
		if(hasChanged(this.renameReplacementMask, renameReplacementMask)) {
			setChanged();
			notifyObservers(new UserPreferencesChangeEvent("renameReplacementMask", renameReplacementMask));

			this.renameReplacementMask = renameReplacementMask;
		}
	}

	public String getRenameReplacementString() {
		return renameReplacementMask;
	}

	public ProxySettings getProxy() {
		return proxy;
	}

	public void setProxy(ProxySettings proxy) {
		if(hasChanged(this.proxy, proxy)) {
			setChanged();
			notifyObservers(new UserPreferencesChangeEvent("proxy", proxy));

			this.proxy = proxy;
		}
	}

	/**
	 * Create the directory if it doesn't exist.
	 */
	private void ensurePath() {
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
		return "UserPreferences [destDir=" + destDir + ", seasonPrefix=" + seasonPrefix + ", moveEnabled="
		+ moveEnabled + ", renameReplacementMask=" + renameReplacementMask + ", proxy=" + proxy + "]";
	}

	private boolean hasChanged(Object originalValue, Object newValue) {
		if(originalValue.equals(newValue)) {
			return false;
		}
		return true;
	}
}
