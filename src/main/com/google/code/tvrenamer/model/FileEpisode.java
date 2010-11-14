package com.google.code.tvrenamer.model;

import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.util.Constants;

public class FileEpisode {
	private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

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
		String show = ShowStore.getShow(showName).getName();
		String destPath = prefs.getDestinationDirectory().getAbsolutePath() + File.separatorChar;
		destPath = destPath + StringUtils.sanitiseTitle(show) + File.separatorChar;
		destPath = destPath + prefs.getSeasonPrefix() + this.seasonNumber + File.separatorChar;
		return new File(destPath);
	}

	public String getNewFilename(UserPreferences prefs) {
		switch (this.status) {
			case ADDED: {
				return ADDED_PLACEHOLDER_FILENAME;
			}
			case DOWNLOADED:
			case RENAMED: {
				String showName = "";
				String seasonNum = "";
				String titleString = "";

				try {
					Show show = ShowStore.getShow(this.showName);
					showName = show.getName();

					try {
						Season season = show.getSeason(this.seasonNumber);
						seasonNum = String.valueOf(season.getNumber());
						
						try {
							String title = season.getTitle(this.episodeNumber);
							titleString = StringUtils.sanitiseTitle(title);
						} catch (EpisodeNotFoundException e) {
							logger.log(Level.SEVERE, "Episode not found for '" + this.toString() + "'", e);
						}
						
					} catch (SeasonNotFoundException e) {
						seasonNum = String.valueOf(this.seasonNumber);
						logger.log(Level.SEVERE, "Season not found for '" + this.toString() + "'", e);
					}
					
				} catch (ShowNotFoundException e) {
					showName = this.showName;
					logger.log(Level.SEVERE, "Show not found for '" + this.toString() + "'", e);
				}


				String newFilename = prefs.getRenameReplacementString();
				newFilename = newFilename.replaceAll("%S", showName);
				newFilename = newFilename.replaceAll("%s", seasonNum);
				newFilename = newFilename.replaceAll("%e", new DecimalFormat("#00").format(this.episodeNumber));
				newFilename = newFilename.replaceAll("%E", new DecimalFormat("##0").format(this.episodeNumber));
				newFilename = newFilename.replaceAll("%t", titleString);
				newFilename = newFilename.replaceAll("%T", titleString.replaceAll(" ", "."));

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
