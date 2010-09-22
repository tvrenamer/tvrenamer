package com.google.code.tvrenamer.model;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.XMLPersistence;
import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.model.util.Constants.SWTMessageBoxType;
import com.google.code.tvrenamer.view.UIUtils;

public class UserPreferences {
	private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

	private File destDir;
	private String seasonPrefix;
	private boolean moveEnabled = true;
	private String renameReplacementMask;

	public static UserPreferences INSTANCE = new UserPreferences();

	/**
	 * Private singleton constructor.
	 */
	private UserPreferences() {
		this.destDir = new File(Constants.DEFAULT_DESTINATION_DIRECTORY);
		this.seasonPrefix = Constants.DEFAULT_SEASON_PREFIX;
		this.renameReplacementMask = Constants.DEFAULT_REPLACEMENT_MASK;
		ensurePath();
	}

	public static UserPreferences getInstance() {
		return INSTANCE;
	}

	/**
	 * Load preferences from xml file
	 */
	public static void loadPreferences() {
		File prefsFile = new File(System.getProperty("user.dir") + Constants.FILE_SEPARATOR
			+ Constants.PREFERENCES_FILE);
		try {
			// retrieve from file and update in-memory copy
			INSTANCE = XMLPersistence.retrieve(prefsFile);
		} catch (IOException e) {
			// failed to load, revert to in-memory
			try {
				INSTANCE = UserPreferences.getInstance();
				XMLPersistence.persist(INSTANCE, prefsFile);
			} catch (IOException e1) {
				// either failed to create (no moving),
				// or failed to save (in-memory prefs only)
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		if (INSTANCE != null) {
			logger.info("Initialised: " + INSTANCE.toString());
		}
	}

	/**
	 * Sets the directory to move renamed files to. Must be an absolute path,
	 * and the entire path will be created if it doesn't exist.
	 * 
	 * @param dir
	 * @return True if the path was created successfully, false otherwise.
	 */
	public void setDestinationDirectory(String dir) throws TVRenamerIOException {
		this.destDir = new File(dir);
		ensurePath();
	}

	/**
	 * Sets the directory to move renamed files to. The entire path will be
	 * created if it doesn't exist.
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
		this.seasonPrefix = StringUtils.sanitiseTitle(prefix);
	}

	public String getSeasonPrefix() {
		return this.seasonPrefix;
	}

	public void setRenameReplacementMask(String renameReplacementMask) {
		this.renameReplacementMask = renameReplacementMask;
	}

	public String getRenameReplacementMask() {
		return renameReplacementMask;
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
			+ moveEnabled + ", renameReplacementMask=" + renameReplacementMask + "]";
	}

}
