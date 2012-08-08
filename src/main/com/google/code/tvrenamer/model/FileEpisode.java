package com.google.code.tvrenamer.model;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
	
	private UserPreferences userPrefs = UserPreferences.getInstance();
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

	private File getDestinationDirectory() {
		String show = ShowStore.getShow(showName).getName();
		String destPath = userPrefs.getDestinationDirectory().getAbsolutePath() + File.separatorChar;
		destPath = destPath + StringUtils.sanitiseTitle(show) + File.separatorChar;
		
		// Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root 
		if(StringUtils.isNotBlank(userPrefs.getSeasonPrefix())) {
			destPath = destPath + userPrefs.getSeasonPrefix() + this.seasonNumber + File.separatorChar;
		}		
		return new File(destPath);
	}

	public String getNewFilename() {
		switch (this.status) {
			case ADDED: {
				return ADDED_PLACEHOLDER_FILENAME;
			}
			case DOWNLOADED:
			case RENAMED: {
				String showName = "";
				String seasonNum = "";
				String titleString = "";
				Calendar airDate = Calendar.getInstance();;

				try {					
					Show show = ShowStore.getShow(this.showName);
					showName = show.getName();

					try {
						Season season = show.getSeason(this.seasonNumber);
						seasonNum = String.valueOf(season.getNumber());

						try {
							titleString = season.getTitle(this.episodeNumber);
							airDate.setTime(season.getAirDate(this.episodeNumber));
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

				String newFilename = userPrefs.getRenameReplacementString();

				// Ensure that all special characters in the replacement are quoted
				showName = Matcher.quoteReplacement(showName);
				titleString = Matcher.quoteReplacement(titleString);
				
				// Make whatever modifications are required
				String episodeNumberString = new DecimalFormat("##0").format(this.episodeNumber);
				String episodeNumberWithLeadingZeros = new DecimalFormat("#00").format(this.episodeNumber);
				String episodeTitleNoSpaces = titleString.replaceAll(" ", ".");
				String seasonNumberWithLeadingZero = new DecimalFormat("00").format(this.seasonNumber);

				newFilename = newFilename.replaceAll(ReplacementToken.SHOW_NAME.getToken(), showName);
				newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM.getToken(), seasonNum);
				newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken(), seasonNumberWithLeadingZero);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM.getToken(), episodeNumberString);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken(), episodeNumberWithLeadingZeros);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE.getToken(), titleString);
				newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken(), episodeTitleNoSpaces);
				
				// Date and times
				newFilename = newFilename.replaceAll(ReplacementToken.DATE_DAY_NUM.getToken(), formatDate(airDate,"d"));
				newFilename = newFilename.replaceAll(ReplacementToken.DATE_DAY_NUMLZ.getToken(), formatDate(airDate,"dd"));
				newFilename = newFilename.replaceAll(ReplacementToken.DATE_MONTH_NUM.getToken(), formatDate(airDate,"M"));
				newFilename = newFilename.replaceAll(ReplacementToken.DATE_MONTH_NUMLZ.getToken(), formatDate(airDate,"MM"));
				newFilename = newFilename.replaceAll(ReplacementToken.DATE_YEAR_FULL.getToken(), formatDate(airDate,"yyyy"));
				newFilename = newFilename.replaceAll(ReplacementToken.DATE_YEAR_MIN.getToken(), formatDate(airDate,"yy"));

				String resultingFilename = newFilename.concat(".").concat(StringUtils.getExtension(file.getName()));
				return StringUtils.sanitiseTitle(resultingFilename);
			}
			case BROKEN:
			default:
				return BROKEN_PLACEHOLDER_FILENAME;
		}
	}
	
	private String formatDate (Calendar cal, String format) {
		SimpleDateFormat date_format = new SimpleDateFormat(format);
		return date_format.format(cal.getTime());
	}
	
	/**
	 * @param prefs the User Preferences
	 * @return the new full file path (for table display) using {@link #getNewFilename()} and the destination directory
	 */
	public String getNewFilePath() {
		String filename = getNewFilename();
		
		if (userPrefs.isMovedEnabled()) {
			return getDestinationDirectory().getAbsolutePath().concat(File.separator).concat(filename);
		}
		return filename;
	}

	@Override
	public String toString() {
		return "FileEpisode { title:" + showName + ", season:" + seasonNumber + ", episode:" + episodeNumber
		+ ", file:" + file.getName() + " }";
	}

}
