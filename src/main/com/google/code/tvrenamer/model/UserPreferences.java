package com.google.code.tvrenamer.model;

import java.io.File;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;

public class UserPreferences {
	private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

	private File destDir;
	private String seasonPrefix;
	private boolean moveEnabled = true;

	public UserPreferences() throws TVRenamerIOException {
		this.destDir = new File(Constants.DEFAULT_DESTINATION_DIRECTORY);
		this.seasonPrefix = Constants.DEFAULT_SEASON_PREFIX;
		ensurePath();
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

	private void ensurePath() throws TVRenamerIOException {
		if (!this.destDir.mkdirs()) {
			if (!this.destDir.exists()) {
				this.moveEnabled = false;
				throw new TVRenamerIOException("Couldn't create path: '" + this.destDir.getAbsolutePath() + "'");
			}
		}
	}

	@Override
	public String toString() {
		return "UserPreferences [destDir='" + destDir + "', seasonPrefix='" + seasonPrefix + "']";
	}
}
