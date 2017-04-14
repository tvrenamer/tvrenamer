package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.io.File;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class FileEpisode {
    private static Logger logger = Logger.getLogger(FileEpisode.class.getName());

    private static final String ADDED_PLACEHOLDER_FILENAME = "Downloading ...";
    private static final String BROKEN_PLACEHOLDER_FILENAME = "Unable to download show information";

    public static final ThreadLocal<DecimalFormat> TWO_DIGITS =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("00");
            }
        };

    public static final ThreadLocal<DecimalFormat> DIGITS =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("##0");
            }
        };

    public static final ThreadLocal<DecimalFormat> TWO_OR_THREE =
        new ThreadLocal<DecimalFormat>() {
            @Override
            protected DecimalFormat initialValue() {
                return new DecimalFormat("#00");
            }
        };

    private final String showName;
    private final int seasonNumber;
    private final int episodeNumber;
    private final String episodeResolution;
    private File file;

    private UserPreferences userPrefs = UserPreferences.getInstance();
    private EpisodeStatus status;

    public FileEpisode(String name, int season, int episode, String resolution, File f) {
        showName = name;
        seasonNumber = season;
        episodeNumber = episode;
        episodeResolution = resolution;
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

    public String getEpisodeResolution() {
        return episodeResolution;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File f) {
        file = f;
    }

    public Path getPath() {
        return file.toPath();
    }

    public void setPath(Path p) {
        file = p.toFile();
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public void setStatus(EpisodeStatus newStatus) {
        status = newStatus;
    }

    private String getDestinationDirectoryName() {
        String show = ShowStore.getShow(showName).getName();
        String sanitised = StringUtils.sanitiseTitle(show);
        String destPath = userPrefs.getDestinationDirectoryName();
        destPath = destPath + File.separatorChar + sanitised;

        String seasonPrefix = userPrefs.getSeasonPrefix();
        // Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root
        if (StringUtils.isNotBlank(seasonPrefix)) {
            String padding = (userPrefs.isSeasonPrefixLeadingZero() && seasonNumber < 9 ? "0" : "");
            String seasonFolderName = seasonPrefix + padding + seasonNumber;
            destPath = destPath + File.separatorChar + seasonFolderName;
        }
        return destPath;
    }

    public String getNewFilename() {
        switch (status) {
            case ADDED: {
                return ADDED_PLACEHOLDER_FILENAME;
            }
            case DOWNLOADED:
            case RENAMED: {
                if (!userPrefs.isRenameEnabled()) {
                    return file.getName();
                }

                String showName = "";
                String seasonNum = "";
                String titleString = "";
                LocalDate airDate = null;

                try {
                    Show show = ShowStore.getShow(this.showName);
                    showName = show.getName();

                    Season season = show.getSeason(seasonNumber);
                    if (season == null) {
                        seasonNum = String.valueOf(seasonNumber);
                        logger.log(Level.SEVERE, "Season #" + seasonNumber + " not found for show '"
                            + this.showName + "'");
                    } else {
                        seasonNum = String.valueOf(season.getNumber());

                        try {
                            titleString = season.getTitle(episodeNumber);
                            airDate = season.getAirDate(episodeNumber);
                            if (airDate == null) {
                                logger.log(Level.WARNING, "Episode air date not found for '" + toString() + "'");
                            }
                        } catch (EpisodeNotFoundException e) {
                            logger.log(Level.SEVERE, "Episode not found for '" + toString() + "'", e);
                        }
                    }

                } catch (ShowNotFoundException e) {
                    showName = this.showName;
                    logger.log(Level.SEVERE, "Show not found for '" + toString() + "'", e);
                }

                String newFilename = userPrefs.getRenameReplacementString();

                // Ensure that all special characters in the replacement are quoted
                showName = Matcher.quoteReplacement(showName);
                showName = GlobalOverrides.getInstance().getShowName(showName);
                titleString = Matcher.quoteReplacement(titleString);

                // Make whatever modifications are required
                String episodeNumberString = DIGITS.get().format(episodeNumber);
                String episodeNumberWithLeadingZeros = TWO_OR_THREE.get().format(episodeNumber);
                String episodeTitleNoSpaces = titleString.replaceAll(" ", ".");
                String seasonNumberWithLeadingZero = TWO_DIGITS.get().format(seasonNumber);

                newFilename = newFilename.replaceAll(ReplacementToken.SHOW_NAME.getToken(), showName);
                newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM.getToken(), seasonNum);
                newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken(),
                                                     seasonNumberWithLeadingZero);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM.getToken(), episodeNumberString);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken(),
                                                     episodeNumberWithLeadingZeros);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE.getToken(), titleString);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken(),
                                                     episodeTitleNoSpaces);
                newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_RESOLUTION.getToken(),
                                                     episodeResolution);

                // Date and times
                newFilename = newFilename
                    .replaceAll(ReplacementToken.DATE_DAY_NUM.getToken(), formatDate(airDate, "d"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_DAY_NUMLZ.getToken(),
                                                     formatDate(airDate, "dd"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_MONTH_NUM.getToken(),
                                                     formatDate(airDate, "M"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_MONTH_NUMLZ.getToken(),
                                                     formatDate(airDate, "MM"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_YEAR_FULL.getToken(),
                                                     formatDate(airDate, "yyyy"));
                newFilename = newFilename.replaceAll(ReplacementToken.DATE_YEAR_MIN.getToken(),
                                                     formatDate(airDate, "yy"));

                String resultingFilename = newFilename.concat(".").concat(StringUtils.getExtension(file.getName()));
                return StringUtils.sanitiseTitle(resultingFilename);
            }
            case BROKEN:
            default:
                return BROKEN_PLACEHOLDER_FILENAME;
        }
    }

    private String formatDate(LocalDate date, String format) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
        return dateFormat.format(date);
    }

    /**
     * @return the new full file path (for table display) using {@link #getNewFilename()} and the destination directory
     */
    public String getNewFilePath() {
        String filename = getNewFilename();

        if (userPrefs.isMoveEnabled()) {
            return getDestinationDirectoryName() + File.separator + filename;
        }
        return filename;
    }

    @Override
    public String toString() {
        return "FileEpisode { title:" + showName + ", season:" + seasonNumber + ", episode:" + episodeNumber
            + ", file:" + file.getName() + " }";
    }

}
