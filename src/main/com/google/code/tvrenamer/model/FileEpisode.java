package com.google.code.tvrenamer.model;

import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import com.google.code.tvrenamer.controller.util.StringUtils;

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

	private File getDestinationDirectory(UserPreferences prefs) {
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

				// Ensure that all special characters in the replacement are quoted
				showName = Matcher.quoteReplacement(showName);
				titleString = Matcher.quoteReplacement(titleString);
				
				// Make whatever modifications are required
				String episodeNumberString = new DecimalFormat("#00").format(this.episodeNumber);
				String episodeNumberNoLeadingZeros = new DecimalFormat("##0").format(this.episodeNumber);
				String episodeTitleNoSpaces = titleString.replaceAll(" ", ".");
				String seasonNumberWithLeadingZero = new DecimalFormat("00").format(this.seasonNumber);

				newFilename = newFilename.replaceAll(ReplacementToken.SHOW_NAME.getToken(), showName);
				newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM.getToken(), seasonNum);
				newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken(), seasonNumberWithLeadingZero);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM.getToken(), episodeNumberString);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM_NO_LEADING_ZERO.getToken(), episodeNumberNoLeadingZeros);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE.getToken(), titleString);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken(), episodeTitleNoSpaces);

				return newFilename.concat(".").concat(StringUtils.getExtension(this.file.getName()));
			}
			case BROKEN:
			default:
				return BROKEN_PLACEHOLDER_FILENAME;
		}
	}
	
	/**
	 * @param prefs the User Preferences
	 * @return the new full file path (for table display) using {@link #getNewFilename(UserPreferences)} and the destination directory
	 */
	public String getNewFilePath(UserPreferences prefs) {
		String filename = getNewFilename(prefs);
		
		if (prefs != null && prefs.isMovedEnabled()) {
			return getDestinationDirectory(prefs).getAbsolutePath().concat(File.separator).concat(filename);
		}
		return filename;
	}

	@Override
	public String toString() {
		return "FileEpisode { title:" + showName + ", season:" + seasonNumber + ", episode:" + episodeNumber
		+ ", file:" + file.getName() + " }";
	}

}
