// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV show.
//
// This is a very mutable class.  It is initially created with just a path,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.FilenameParser;
import org.tvrenamer.controller.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class FileEpisode {
    private static final Logger logger = Logger.getLogger(FileEpisode.class.getName());

    /**
     * A status for how much we know about the filename.
     *
     * <ul>
     * <li>PARSED means that we believe we have extracted all the required information from
     *     the filename</li>
     * <li>BAD_PARSE means we're not going to try to query for this FileEpisode, because
     *     we could not find the show name.</li>
     * <li>UNPARSED means we haven't yet finished examining the filename.</li>
     * </ul>
     *
     */
    private enum ParseStatus {
        PARSED,
        BAD_PARSE,
        UNPARSED
    }

    /**
     * A status for how much we know about the Series and its listings.
     *
     * These are essentially in order, from most complete to least complete.
     *
     * <ul>
     * <li>GOT_LISTINGS means we have matched this FileEpisode to an actual Episode,
     *     based on the season and episode information we extracted from the filename</li>
     * <li>NO_MATCH means we resolved the Show and downloaded the listings, but did
     *     not find a match for the season and episode information</li>
     * <li>NO_LISTINGS means something went wrong trying to download the Show's listings,
     *     and we don't have any episode information</li>
     * <li>GOT_SHOW is exactly the same state of information as NO_LISTINGS; the difference
     *     is, GOT_SHOW means we are in the process of trying to download listings, whereas
     *     NO_LISTINGS means we tried and have given up</li>
     * <li>UNFOUND means we were unable to map the supposed show name that we found in the
     *     filename, to an actual show from the provider</li>
     * <li>NOT_STARTED means we have not started to query for information about the show.
     *     In this case, to know more about what's going on, we need to look at the parse
     *     status.  Refer to its comments for elaboration.</li>
     * </ul>
     */
    private enum SeriesStatus {
        GOT_LISTINGS,
        NO_MATCH,
        NO_LISTINGS,
        GOT_SHOW,
        UNFOUND,
        NOT_STARTED
    }

    private enum FileStatus {
        UNCHECKED,
        NO_FILE,
        ORIGINAL,
        MOVING,
        RENAMED,
        FAIL_TO_MOVE
    }

    private static final String FILE_SEPARATOR_STRING = java.io.File.separator;
    private static final long NO_FILE_SIZE = -1L;

    // Allow titles long enough to include this one:
    // "The One With The Thanksgiving Flashbacks (a.k.a. The One With All The Thanksgivings)"
    private static final int MAX_TITLE_LENGTH = 85;

    // This is the one final field in this class; it's the one thing that should never
    // change in a FileEpisode.  It could be the empty string (though it would be unusual).
    // If the file does actually have a suffix, this variable *includes* the leading dot.
    private final String filenameSuffix;

    // These four fields reflect the information derived from the filename.  In particular,
    // filenameShow is based on the part of the filename we "guessed" represented the name
    // of the show, and which we use to query the provider.  Note that the actual show name
    // that we get back from the provider will likely differ from what we have here.
    private String filenameShow = "";
    private String filenameSeason = "";
    private String filenameEpisode = "";
    private String filenameResolution = "";

    // These integers are meant to represent the indices into the Show's catalog that "we"
    // (the combination of the program and the user) think is right.  In many cases, there
    // are alternate and ambiguous numbering schemes.  There's not necessarily one true
    // answer.  But these variables are meant to hold the answer the user wants.
    //
    // Initially the placement is set to the result of the parse (i.e., whatever is found
    // in the filename), and actually as of this writing, there is nothing in the program
    // that would change them, but there could/should be, in future versions.
    private EpisodePlacement placement = null;

    // Information about the file on disk.  The only way the UI allows you to enter names
    // to be processed is by selecting a file on disk, so they obviously should exist.
    // It's always possible they could be moved or deleted out from under us, though.
    // It's also true that for testing, we create FileEpisodes that refer to nonexistent
    // files.  There's really no reason why the file has to exist, at least, not until
    // we actually try to move it.  If we just want to parse information and look it up
    // in the show's catalog, the file does not need to actually be present.
    private Path pathObj;
    private String fileNameString;
    private long fileSize = NO_FILE_SIZE;

    // After we've looked up the filenameShow from the provider, we try to get a Show from
    // the provider.  If we do not find any options, or if there is any kind of error
    // trying to download show information, we don't get a Show and this value remains null.
    private Show actualShow = null;

    // This class represents a file on disk, with fields that indicate which episode we
    // believe it refers to, based on the filename.  The "Episode" class represents
    // information about an actual episode of a show, based on listings from the provider.
    // Once we have the listings, we should be able to map this instance to an Episode.
    private Episode actualEpisode = null;

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private final UserPreferences userPrefs = UserPreferences.getInstance();

    // The state of this object, not the state of the actual TV episode.
    private ParseStatus parseStatus = ParseStatus.UNPARSED;
    private SeriesStatus seriesStatus = SeriesStatus.NOT_STARTED;
    @SuppressWarnings("unused")
    private FileStatus fileStatus = FileStatus.UNCHECKED;

    // This is the basic part of what we would rename the file to.  That is, we would
    // rename it to destinationFolder + baseForRename + filenameSuffix.
    private String baseForRename = null;

    // Initially we create the FileEpisode with nothing more than the path.
    // Other information will flow in.
    public FileEpisode(Path p) {
        if (p == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        pathObj = p;
        final Path justNamePath = pathObj.getFileName();
        if (justNamePath == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        fileNameString = justNamePath.toString();
        filenameSuffix = StringUtils.getExtension(fileNameString);
        checkFile(true);
        FilenameParser.parseFilename(this);
    }

    // Create FileEpisode with String; only for testing
    public FileEpisode(String filename) {
        if (filename == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        pathObj = Paths.get(filename);
        final Path justNamePath = pathObj.getFileName();
        if (justNamePath == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        fileNameString = justNamePath.toString();
        filenameSuffix = StringUtils.getExtension(fileNameString);
        checkFile(false);
    }

    public String getFilenameShow() {
        return filenameShow;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
    }

    public String getFilenameSeason() {
        return filenameSeason;
    }

    public String getFilenameEpisode() {
        return filenameEpisode;
    }

    public EpisodePlacement getEpisodePlacement() {
        return placement;
    }

    public void setEpisodePlacement(String filenameSeason, String filenameEpisode) {
        this.filenameSeason = filenameSeason;
        this.filenameEpisode = filenameEpisode;
        int seasonNum = Show.NO_SEASON;
        int episodeNum = Show.NO_EPISODE;
        try {
            seasonNum = Integer.parseInt(filenameSeason);
        } catch (Exception e) {
            logger.fine("unable to parse season number: " + filenameSeason);
        }
        try {
            episodeNum = Integer.parseInt(filenameEpisode);
        } catch (Exception e) {
            logger.fine("unable to parse episode number: " + filenameEpisode);
        }
        placement = new EpisodePlacement(seasonNum, episodeNum);
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

    public String getFilenameSuffix() {
        return filenameSuffix;
    }

    public Path getPath() {
        return pathObj;
    }

    private void checkFile(boolean mustExist) {
        if (Files.exists(pathObj)) {
            try {
                fileStatus = FileStatus.ORIGINAL;
                fileSize = Files.size(pathObj);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "couldn't get size of " + pathObj, ioe);
                fileStatus = FileStatus.NO_FILE;
                fileSize = NO_FILE_SIZE;
            }
        } else {
            if (mustExist) {
                logger.warning("creating FileEpisode for nonexistent path, " + pathObj);
            }
            fileStatus = FileStatus.NO_FILE;
            fileSize = NO_FILE_SIZE;
        }
    }

    public void setPath(Path p) {
        if (p == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        pathObj = p;
        final Path justNamePath = pathObj.getFileName();
        if (justNamePath == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        fileNameString = justNamePath.toString();
        String newSuffix = StringUtils.getExtension(fileNameString);
        if (!filenameSuffix.equals(newSuffix)) {
            throw new IllegalStateException("suffix of a FileEpisode may not change!");
        }
        checkFile(true);
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFilepath() {
        return pathObj.toAbsolutePath().toString();
    }

    public boolean wasParsed() {
        return (parseStatus == ParseStatus.PARSED);
    }

    public boolean isReady() {
        return (actualEpisode != null);
    }

    public void setParsed() {
        parseStatus = ParseStatus.PARSED;
    }

    public void setFailToParse() {
        parseStatus = ParseStatus.BAD_PARSE;
    }

    public void setMoving() {
        fileStatus = FileStatus.MOVING;
    }

    public void setRenamed() {
        fileStatus = FileStatus.RENAMED;
    }

    public void setFailToMove() {
        fileStatus = FileStatus.FAIL_TO_MOVE;
    }

    public void setDoesNotExist() {
        fileStatus = FileStatus.NO_FILE;
    }

    private String getShowNamePlaceholder() {
        return "<" + actualShow.getName() + ">";
    }

    private String getNoMatchPlaceholder() {
        return EPISODE_NOT_FOUND + " <" + actualShow.getName() + " / "
            + actualShow.getIdString() + ">: " + " season " + placement.season
            + ", episode " + placement.episode + " not found";
    }

    private String getNoListingsPlaceholder() {
        return " <" + actualShow.getName() + " / "
            + actualShow.getIdString() + ">: " + DOWNLOADING_FAILED;
    }

    private String getNoShowPlaceholder() {
        ShowName showName = ShowName.lookupShowName(filenameShow);
        String queryString = showName.getQueryString();
        return BROKEN_PLACEHOLDER_FILENAME + " for \""
            + StringUtils.decodeSpecialCharacters(queryString)
            + "\"";
    }

    /**
     * Set the actualShow of this object to be an instance of a Show.  From there, we
     * can get the actual episode.
     *
     * Also serves as a way to communicate that we were unable to get the Show.  That's
     * why it makes sense to call this method with null, even though actualShow is
     * already null.  It differentiates between "null, we don't know yet" and "null,
     * we couldn't get the show".
     *
     * @param show
     *    the Show that should be associated with this FileEpisode, or null if no Show
     *    could be created/found
     */
    public void setEpisodeShow(Show show) {
        actualShow = show;
        if (actualShow == null) {
            seriesStatus = SeriesStatus.UNFOUND;
        } else {
            seriesStatus = SeriesStatus.GOT_SHOW;
        }
    }

    /**
     *
     * @return true if the episode associated with this item is found in the listings
     */
    public boolean listingsComplete() {
        if (actualShow == null) {
            logger.warning("error: should not get listings, do not have show!");
            seriesStatus = SeriesStatus.NOT_STARTED;
            return false;
        }

        if (!actualShow.hasEpisodes()) {
            seriesStatus = SeriesStatus.NO_LISTINGS;
            return false;
        }

        actualEpisode = actualShow.getEpisode(placement);
        if (actualEpisode == null) {
            logger.info("Season #" + placement.season + ", Episode #"
                        + placement.episode + " not found for show '"
                        + filenameShow + "'");
            seriesStatus = SeriesStatus.NO_MATCH;
            return false;
        }

        // Success!!!
        seriesStatus = SeriesStatus.GOT_LISTINGS;
        return true;
    }

    /**
     *
     * @param err
     *    an exception that may have occurred while trying to get the listings
     *    (could be null)
     */
    public void listingsFailed(Exception err) {
        seriesStatus = SeriesStatus.NO_LISTINGS;
        if (err != null) {
            logger.log(Level.WARNING, "failed to get listings for " + this, err);
        }
        if (actualShow == null) {
            logger.warning("error: should not have tried to get listings, do not have show!");
        }
    }

    /**
     * Return the name of the directory to which the file should be moved.
     *
     * We try to make sure that a term means the same thing throughout the program.
     * The "destination directory" is the *top-level* directory that the user has
     * specified we should move all the files into.  But the files don't necessarily
     * go directly into the "destination directory".  They will go into a sub-directory
     * naming the show and possibly the season.  That final directory is what we refer
     * to as the "move-to directory".
     *
     * @return the name of the directory into which this file (the Path encapsulated
     *         within this FileEpisode) should be moved
     */
    private String getMoveToDirectory() {
        String destPath = userPrefs.getDestinationDirectoryName();
        if (actualShow == null) {
            logger.warning("error: should not get move-to directory, do not have show!");
        } else {
            String dirname = actualShow.getDirName();
            destPath = destPath + FILE_SEPARATOR_STRING + dirname;

            // Now we might append the "season" directory, if the user requested it in
            // the preferences.  But, only if we actually *have* season information.
            if (placement.season > Show.NO_SEASON) {
                String seasonPrefix = userPrefs.getSeasonPrefix();
                // Defect #50: Only add the 'season #' folder if set,
                // otherwise put files in showname root
                if (StringUtils.isNotBlank(seasonPrefix)) {
                    String seasonString = userPrefs.isSeasonPrefixLeadingZero()
                        ? StringUtils.zeroPadTwoDigits(placement.season)
                        : String.valueOf(placement.season);
                    destPath = destPath + FILE_SEPARATOR_STRING + seasonPrefix + seasonString;
                }
            } else {
                logger.fine("maybe should not get move-to directory, do not have season");
            }
        }
        return destPath;
    }

    /**
     * @return the new Path into which this file would be moved, based on the information
     *         we've gathered, and the user's preferences
     */
    public Path getMoveToPath() {
        if (userPrefs.isMoveEnabled()) {
            return Paths.get(getMoveToDirectory());
        } else {
            return pathObj.toAbsolutePath().getParent();
        }
    }

    private static String formatDate(final LocalDate date, final String format) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
        return dateFormat.format(date);
    }

    private static String plugInInformation(final String replacementTemplate, final String showName,
                                            final String episodeTitle, final String resolution,
                                            final EpisodePlacement placement, final LocalDate airDate)
    {
        String newFilename = replacementTemplate
            .replaceAll(ReplacementToken.SEASON_NUM.getToken(),
                        String.valueOf(placement.season))
            .replaceAll(ReplacementToken.SEASON_NUM_LEADING_ZERO.getToken(),
                        StringUtils.zeroPadTwoDigits(placement.season))
            .replaceAll(ReplacementToken.EPISODE_NUM.getToken(),
                        StringUtils.formatDigits(placement.episode))
            .replaceAll(ReplacementToken.EPISODE_NUM_LEADING_ZERO.getToken(),
                        StringUtils.zeroPadThreeDigits(placement.episode))
            .replaceAll(ReplacementToken.SHOW_NAME.getToken(),
                        Matcher.quoteReplacement(showName))
            .replaceAll(ReplacementToken.EPISODE_TITLE.getToken(),
                        Matcher.quoteReplacement(episodeTitle))
            .replaceAll(ReplacementToken.EPISODE_TITLE_NO_SPACES.getToken(),
                        Matcher.quoteReplacement(StringUtils.makeDotTitle(episodeTitle)))
            .replaceAll(ReplacementToken.EPISODE_RESOLUTION.getToken(),
                        resolution);

        // Date and times
        if (airDate == null) {
            newFilename = newFilename
                .replaceAll(ReplacementToken.DATE_DAY_NUM.getToken(), "")
                .replaceAll(ReplacementToken.DATE_DAY_NUMLZ.getToken(), "")
                .replaceAll(ReplacementToken.DATE_MONTH_NUM.getToken(), "")
                .replaceAll(ReplacementToken.DATE_MONTH_NUMLZ.getToken(), "")
                .replaceAll(ReplacementToken.DATE_YEAR_FULL.getToken(), "")
                .replaceAll(ReplacementToken.DATE_YEAR_MIN.getToken(), "");
        } else {
            newFilename = newFilename
                .replaceAll(ReplacementToken.DATE_DAY_NUM.getToken(),
                            formatDate(airDate, "d"))
                .replaceAll(ReplacementToken.DATE_DAY_NUMLZ.getToken(),
                            formatDate(airDate, "dd"))
                .replaceAll(ReplacementToken.DATE_MONTH_NUM.getToken(),
                            formatDate(airDate, "M"))
                .replaceAll(ReplacementToken.DATE_MONTH_NUMLZ.getToken(),
                            formatDate(airDate, "MM"))
                .replaceAll(ReplacementToken.DATE_YEAR_FULL.getToken(),
                            formatDate(airDate, "yyyy"))
                .replaceAll(ReplacementToken.DATE_YEAR_MIN.getToken(),
                            formatDate(airDate, "yy"));
        }

        return StringUtils.sanitiseTitle(newFilename);
    }

    String getRenamedBasename() {
        String showName;
        if (actualShow == null) {
            logger.warning("should not be renaming without an actual Show.");
            showName = filenameShow;
        } else {
            showName = actualShow.getName();
        }

        String titleString = "";
        LocalDate airDate = null;
        if (actualEpisode != null) {
            titleString = actualEpisode.getTitle();
            int len = titleString.length();
            if (len > MAX_TITLE_LENGTH) {
                logger.fine("truncating episode title " + titleString);
                titleString = titleString.substring(0, MAX_TITLE_LENGTH);
            }
            airDate = actualEpisode.getAirDate();
            if (airDate == null) {
                logger.log(Level.WARNING, "Episode air date not found for '" + this + "'");
            }
        }

        // Note, this is an instance variable, not a local variable.
        baseForRename = plugInInformation(userPrefs.getRenameReplacementString(), showName,
                                          titleString, filenameResolution, placement, airDate);
        return baseForRename;
    }

    public String getDestinationBasename() {
        if (userPrefs.isRenameEnabled()) {
            return getRenamedBasename();
        } else {
            return StringUtils.removeLast(fileNameString, filenameSuffix);
        }
    }

    /**
     * @return the new full file path (for table display) using {@link #getRenamedBasename()} and
     *          the destination directory
     */
    public String getReplacementText() {
        switch (seriesStatus) {
            case GOT_LISTINGS: {
                if (userPrefs.isRenameEnabled()) {
                    String newFilename = getRenamedBasename() + filenameSuffix;

                    if (userPrefs.isMoveEnabled()) {
                        return getMoveToDirectory() + FILE_SEPARATOR_STRING + newFilename;
                    } else {
                        return newFilename;
                    }
                } else if (userPrefs.isMoveEnabled()) {
                    return getMoveToDirectory() + FILE_SEPARATOR_STRING + fileNameString;
                } else {
                    // This setting is prohibited, and both the UI and the UserPreferences class are set
                    // up to prevent it.  But, if it somehow happens, we always want to fail gracefully.
                    logger.severe("apparently both rename and move are disabled! This is not allowed!");
                    return fileNameString;
                }
            }
            case NO_MATCH: {
                return getNoMatchPlaceholder();
            }
            case NO_LISTINGS: {
                return getNoListingsPlaceholder();
            }
            case GOT_SHOW: {
                return getShowNamePlaceholder();
            }
            case UNFOUND: {
                return getNoShowPlaceholder();
            }
            default: {
                if (seriesStatus != SeriesStatus.NOT_STARTED) {
                    logger.warning("internal error, seriesStatus check apparently not exhaustive: "
                                   + seriesStatus);
                }
                switch (parseStatus) {
                    case UNPARSED: {
                        return EMPTY_STRING;
                    }
                    case BAD_PARSE: {
                        return BAD_PARSE_MESSAGE;
                    }
                    default: {
                        return ADDED_PLACEHOLDER_FILENAME;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        String val = "FileEpisode {file: " + fileNameString + ", show: ";
        String name = (actualShow == null) ? filenameShow : actualShow.getName();
        String plc = (placement == null) ? ", no placement"
            : ", season: " + placement.season + ", episode: " + placement.episode;
        return val + name + plc + " }";
    }
}
