// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV show.
//
// This is a very mutable class.  It is initially created with just a filename,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final int NO_SEASON = -1;
    private static final int NO_EPISODE = 0;

    // These four fields reflect the information derived from the filename.  In particular,
    // filenameShow is based on the part of the filename we "guessed" represented the name
    // of the show, and which we use to query the provider.  Note that the actual show name
    // that we get back from the provider will likely differ from what we have here.
    private String filenameShow = "";
    private String filenameSeason = "";
    private String filenameEpisode = "";
    private String filenameResolution = "";
    private String queryString = "";

    private String baseForRename = null;
    private String filenameSuffix = null;

    private int seasonNum = NO_SEASON;
    private int episodeNum = NO_EPISODE;

    private Path path;

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private UserPreferences userPrefs = UserPreferences.getInstance();
    private EpisodeStatus status;

    // Initially we create the FileEpisode with nothing more than the path.
    // Other information will flow in.
    public FileEpisode(Path p) {
        path = p;
        status = EpisodeStatus.UNPARSED;
    }

    public FileEpisode(String filename) {
        this(Paths.get(filename));
    }

    public String getQueryString() {
        return queryString;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
        queryString = StringUtils.makeQueryString(filenameShow);
    }

    public int getSeasonNum() {
        return seasonNum;
    }

    public String getFilenameSeason() {
        return filenameSeason;
    }

    public void setSeasonNum(int seasonNum) {
        this.seasonNum = seasonNum;
    }

    public void setFilenameSeason(String filenameSeason) {
        this.filenameSeason = filenameSeason;
        try {
            seasonNum = Integer.parseInt(filenameSeason);
        } catch (Exception e) {
            seasonNum = NO_SEASON;
        }
    }

    public int getEpisodeNum() {
        return episodeNum;
    }

    public String getFilenameEpisode() {
        return filenameEpisode;
    }

    public void setEpisodeNum(int episodeNum) {
        this.episodeNum = episodeNum;
    }

    public void setFilenameEpisode(String filenameEpisode) {
        this.filenameEpisode = filenameEpisode;
        try {
            episodeNum = Integer.parseInt(filenameEpisode);
        } catch (Exception e) {
            episodeNum = NO_EPISODE;
        }
    }

    public String getFilenameResolution() {
        return filenameResolution;
    }

    public void setFilenameResolution(String filenameResolution) {
        if (filenameResolution == null) {
            this.filenameResolution = "";
        } else {
            this.filenameResolution = filenameResolution;
        }
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
            String padding = (userPrefs.isSeasonPrefixLeadingZero() && seasonNum < 9 ? "0" : "");
            String seasonFolderName = seasonPrefix + padding + seasonNum;
            destPath = destPath + FILE_SEPARATOR_STRING + seasonFolderName;
        }
        return destPath;
    }

    private String formatDate(LocalDate date, String format) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
        return dateFormat.format(date);
    }

    private String getRenamedFilename() {
        String showName = "";
        String titleString = "";
        LocalDate airDate = null;

        try {
            Show show = ShowStore.getShow(queryString);
            showName = show.getName();

            Season season = show.getSeason(seasonNum);
            if (season == null) {
                logger.log(Level.SEVERE, "Season #" + seasonNum + " not found for show '"
                           + filenameShow + "'");
            } else {
                try {
                    titleString = season.getTitle(episodeNum);
                    airDate = season.getAirDate(episodeNum);
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

        // Make whatever modifications are required
        String episodeNumberString = DIGITS.get().format(episodeNum);
        String episodeNumberWithLeadingZeros = TWO_OR_THREE.get().format(episodeNum);
        String episodeTitleNoSpaces = Matcher.quoteReplacement(StringUtils.makeDotTitle(titleString));
        String seasonNumberWithLeadingZero = TWO_DIGITS.get().format(seasonNum);

        titleString = Matcher.quoteReplacement(titleString);

        newFilename = newFilename.replaceAll(ReplacementToken.SHOW_NAME.getToken(), showName);
        newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM.getToken(),
                                             String.valueOf(seasonNum));
        newFilename = newFilename.replaceAll(ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken(),
                                             seasonNumberWithLeadingZero);
        newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM.getToken(),
                                             episodeNumberString);
        newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken(),
                                             episodeNumberWithLeadingZeros);
        newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE.getToken(), titleString);
        newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken(),
                                             episodeTitleNoSpaces);
        newFilename = newFilename.replaceAll(ReplacementToken.EPISODE_RESOLUTION.getToken(),
                                             filenameResolution);

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

        // Note, these are instance variables, not local variables.
        baseForRename = StringUtils.sanitiseTitle(newFilename);
        filenameSuffix = StringUtils.getExtension(path.getFileName().toString());

        return baseForRename + filenameSuffix;
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
                return getRenamedFilename();
            }
            case UNPARSED:
            case BROKEN:
            default:
                return BROKEN_PLACEHOLDER_FILENAME;
        }
    }

    /**
     * @return the new full file path (for table display) using {@link #getNewFilename()} and
     *          the destination directory
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
        return "FileEpisode { title:" + filenameShow + ", season:" + seasonNum + ", episode:" + episodeNum
            + ", file:" + path.getFileName() + " }";
    }
}
