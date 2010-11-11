package com.google.code.tvrenamer.model;

import java.io.File;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.code.tvrenamer.controller.JAXBPersistence;
import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;
import com.google.code.tvrenamer.view.UIUtils;

@XmlRootElement
public class UserPreferences {
	private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

	private File destDir;
	private String seasonPrefix;
	private boolean moveEnabled = false;
	private String renameReplacementMask;
	private static File prefsFile = new File(System.getProperty("user.dir") + Constants.FILE_SEPARATOR
		+ Constants.PREFERENCES_FILE);
	private ProxySettings proxy;

	public UserPreferences() throws TVRenamerIOException {
		this.destDir = new File(Constants.DEFAULT_DESTINATION_DIRECTORY);
		this.seasonPrefix = Constants.DEFAULT_SEASON_PREFIX;
		this.renameReplacementMask = Constants.DEFAULT_REPLACEMENT_MASK;
		this.proxy = new ProxySettings();

		ensurePath();
	}

	/**
	 * Load preferences from xml file
	 */
	public static UserPreferences load() {
		UserPreferences prefs = JAXBPersistence.retrieve(prefsFile);
		logger.fine(prefs.toString());
		
		// Apply the proxy settings
		prefs.getProxy().apply();

		return prefs;
	}

	public void store() {
		JAXBPersistence.persist(this, prefsFile);
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
		this.destDir = new File(dir);
		ensurePath();
	}

	/**
	 * Sets the directory to move renamed files to. The entire path will be created if it doesn't exist.
	 * 
	 * @param dir
	 * @return True if the path was created successfully, false otherwise.
	 */
	public void setDestinationDirectory(File dir) throws TVRenamerIOException {
		this.destDir = dir;
		ensurePath();
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
		this.moveEnabled = moveEnabled;
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
		this.seasonPrefix = StringUtils.sanitiseTitle(prefix);
	}

	public String getSeasonPrefix() {
		return this.seasonPrefix;
	}

	public String getSeasonPrefixForDisplay() {
		return ("\"" + this.seasonPrefix + "\"");
	}

	public void setRenameReplacementString(String renameReplacementMask) {
		this.renameReplacementMask = renameReplacementMask;
	}

	public String getRenameReplacementString() {
		return renameReplacementMask;
	}

	public ProxySettings getProxy() {
		return proxy;
	}

	public void setProxy(ProxySettings proxy) {
		this.proxy = proxy;
	}

	/**
	 * Create the directory if it doesn't exist.
	 */
	private void ensurePath() {
		if (!this.destDir.mkdirs()) {
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
}
