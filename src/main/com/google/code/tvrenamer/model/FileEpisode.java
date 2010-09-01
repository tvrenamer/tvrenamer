package com.google.code.tvrenamer.model;

import java.io.File;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;

public class FileEpisode {
	private final String showName;
	private final int seasonNumber;
	private final int episodeNumber;
	private File file;

	public FileEpisode(String name, int season, int episode, File f) {
		showName = name;
		seasonNumber = season;
		episodeNumber = episode;
		file = f;
	}

	public String getShowName() {
		return showName;
	}

	public int getSeasonNumber() {
		return seasonNumber;
	}

	public int getEpisodeNumber() {
		return episodeNumber;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File f) {
		file = f;
	}

	public File getDestinationDirectory(UserPreferences prefs) throws TVRenamerIOException {
		String show = ShowStore.getShow(showName.toLowerCase()).getName();
		String destPath = prefs.getDestinationDirectory().getAbsolutePath() + Constants.FILE_SEPARATOR;
		destPath = destPath + StringUtils.sanitiseTitle(show) + Constants.FILE_SEPARATOR;
		destPath = destPath + prefs.getSeasonPrefix() + this.seasonNumber + Constants.FILE_SEPARATOR;
		File dir = new File(destPath);
		if (!dir.mkdirs() && !dir.exists()) {
			throw new TVRenamerIOException("Couldn't create path: '" + dir.getAbsolutePath() + "'");
		}
		return dir;
	}

	@Override
	public String toString() {
		return "FileEpisode { title:" + showName + ", season:" + seasonNumber + ", episode:" + episodeNumber + ", file:"
		                + file.getName() + " }";
	}

}
