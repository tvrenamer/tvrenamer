package com.google.code.tvrenamer.model;

import java.io.File;
import java.text.DecimalFormat;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;

public class FileEpisode {
	private static final String ADDED_PLACEHOLDER_FILENAME = "downloading ...";
	private static final String BROKEN_PLACEHOLDER_FILENAME = "unable to download information";
	private final String showName;
	private final int seasonNumber;
	private final int episodeNumber;
	private File file;

	private EpisodeStatus status;

	public FileEpisode(String name, int season, int episode, File f) {
		showName = name;
		seasonNumber = season;
		episodeNumber = episode;
		file = f;
		status = EpisodeStatus.ADDED;
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

	public EpisodeStatus getStatus() {
		return this.status;
	}

	public void setStatus(EpisodeStatus newStatus) {
		this.status = newStatus;
	}

	public File getDestinationDirectory(UserPreferences prefs) {
		String show = ShowStore.getShow(showName.toLowerCase()).getName();
		String destPath = prefs.getDestinationDirectory().getAbsolutePath() + Constants.FILE_SEPARATOR;
		destPath = destPath + StringUtils.sanitiseTitle(show) + Constants.FILE_SEPARATOR;
		destPath = destPath + prefs.getSeasonPrefix() + this.seasonNumber + Constants.FILE_SEPARATOR;
		return new File(destPath);
	}

	public String getNewFilename() {
		switch (this.status) {
			case ADDED: {
				return ADDED_PLACEHOLDER_FILENAME;
			}
			case DOWNLOADED:
			case RENAMED: {
				String showName = "Show not found";
				String seasonNum = "Season not found";
				String titleString = "Episode not found";

				Show show = ShowStore.getShow(this.showName.toLowerCase());
				showName = show.getName();

				Season season = show.getSeason(this.seasonNumber);
				seasonNum = String.valueOf(season.getNumber());

				String title = season.getTitle(this.episodeNumber);
				titleString = StringUtils.sanitiseTitle(title);

				String newFilename = Constants.DEFAULT_REPLACEMENT_MASK;
				newFilename = newFilename.replaceAll("%S", showName);
				newFilename = newFilename.replaceAll("%s", seasonNum);
				newFilename = newFilename.replaceAll("%e", new DecimalFormat("00").format(this.episodeNumber));
				newFilename = newFilename.replaceAll("%t", titleString);

				return newFilename + "." + StringUtils.getExtension(this.file.getName());
			}
			case BROKEN:
			default:
				return BROKEN_PLACEHOLDER_FILENAME;
		}
	}

	@Override
	public String toString() {
		return "FileEpisode { title:" + showName + ", season:" + seasonNumber + ", episode:" + episodeNumber
			+ ", file:" + file.getName() + " }";
	}

}
