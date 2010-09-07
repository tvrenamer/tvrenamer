package com.google.code.tvrenamer.model;

import java.io.File;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;

public class UserPreferences {
	private static Logger logger = Logger.getLogger(UserPreferences.class.getName());

	private File          destDir;
	private String        seasonPrefix;

	public UserPreferences() throws TVRenamerIOException {
		this.destDir = new File(Constants.DEFAULT_DESTINATION_DIRECTORY);
		if (!ensurePath()) {
			throw new TVRenamerIOException("Couldn't create path: '" + this.destDir.getAbsolutePath() + "'");
		}
		this.seasonPrefix = Constants.DEFAULT_SEASON_PREFIX;
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
		if (!ensurePath()) {
			throw new TVRenamerIOException("Couldn't create path: '" + this.destDir.getAbsolutePath() + "'");
		}
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
		if (!ensurePath()) {
			throw new TVRenamerIOException("Couldn't create path: '" + this.destDir.getAbsolutePath() + "'");
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

	public void setSeasonPrefix(String prefix) {
		this.seasonPrefix = StringUtils.sanitiseTitle(prefix);
	}

	public String getSeasonPrefix() {
		return this.seasonPrefix;
	}

	private boolean ensurePath() {
		if (!this.destDir.mkdirs()) {
			return this.destDir.exists();
		}
		return true;
	}

	@Override
	public String toString() {
		return "UserPreferences [destDir='" + destDir + "', seasonPrefix='" + seasonPrefix + "']";
	}
}
