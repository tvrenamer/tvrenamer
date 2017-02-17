package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

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
    private static final String FILE_SEPARATOR_STRING = java.io.File.separator;

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

    private final String filenameShow;
    private final String queryString;
    private final int seasonNumber;
    private final int episodeNumber;
    private final String episodeResolution;
    private Path path;

    private UserPreferences userPrefs = UserPreferences.getInstance();
    private EpisodeStatus status;

    public FileEpisode(String name, int season, int episode, String resolution, Path p) {
        filenameShow = name;
        queryString = StringUtils.makeQueryString(name);
        seasonNumber = season;
        episodeNumber = episode;
        episodeResolution = resolution;
        path = p;
        status = EpisodeStatus.ADDED;
    }

    public String getQueryString() {
        return queryString;
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

    public String getFilepath() {
        return path.toAbsolutePath().toString();
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path p) {
        path = p;
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public void setStatus(EpisodeStatus newStatus) {
        status = newStatus;
    }

    private String getDestinationDirectoryName() {
        String dirname = ShowStore.getShow(queryString).getDirName();
        String destPath = userPrefs.getDestinationDirectoryName();
        destPath = destPath + FILE_SEPARATOR_STRING + dirname;

        String seasonPrefix = userPrefs.getSeasonPrefix();
        // Defect #50: Only add the 'season #' folder if set, otherwise put files in showname root
        if (StringUtils.isNotBlank(seasonPrefix)) {
            String padding = (userPrefs.isSeasonPrefixLeadingZero() && seasonNumber < 9 ? "0" : "");
            String seasonFolderName = seasonPrefix + padding + seasonNumber;
            destPath = destPath + FILE_SEPARATOR_STRING + seasonFolderName;
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
                    return path.getFileName().toString();
                }

                String showName = "";
                String seasonNum = "";
                String titleString = "";
                LocalDate airDate = null;

                try {
                    Show show = ShowStore.getShow(queryString);
                    showName = show.getName();

                    Season season = show.getSeason(seasonNumber);
                    if (season == null) {
                        seasonNum = String.valueOf(seasonNumber);
                        logger.log(Level.SEVERE, "Season #" + seasonNumber + " not found for show '"
                            + filenameShow + "'");
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
                    showName = filenameShow;
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

                String fileBaseName = path.getFileName().toString();
                String resultingFilename = newFilename.concat(StringUtils.getExtension(fileBaseName));
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
            return getDestinationDirectoryName() + FILE_SEPARATOR_STRING + filename;
        }
        return filename;
    }

    @Override
    public String toString() {
        return "FileEpisode { title:" + filenameShow + ", season:" + seasonNumber + ", episode:" + episodeNumber
            + ", file:" + path.getFileName() + " }";
    }

}
