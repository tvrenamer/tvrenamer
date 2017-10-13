// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV show.
//
// This is a very mutable class.  It is initially created with just a path,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.TVRenamer;
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

    private enum ParseStatus {
        UNPARSED,
        PARSED,
        BAD_PARSE
    }

    private enum SeriesStatus {
        NOT_STARTED,
        GOT_SHOW,
        UNFOUND,
        GOT_LISTINGS,
        NO_LISTINGS
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
    // change in a FileEpisode.
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
    // Initially these values are set to the result of the parse (i.e., whatever is found
    // in the filename), and actually as of this writing, there is nothing in the program
    // that would change them, but there could/should be, in future versions.
    private int seasonNum = Show.NO_SEASON;
    private int episodeNum = Show.NO_EPISODE;

    // Information about the file on disk.  The only way the UI allows you to enter names
    // to be processed is by selecting a file on disk, so they obviously should exist.
    // It's always possible they could be moved or deleted out from under us, though.
    // It's also true that for testing, we create FileEpisodes that refer to nonexistent
    // files.  There's really no reason why the file has to exist, at least, not until
    // we actually try to move it.  If we just want to parse information and look it up
    // in the show's catalog, the file does not need to actually be present.
    private Path path;
    private String fileNameString;
    @SuppressWarnings("unused")
    private boolean exists = false;
    private long fileSize = NO_FILE_SIZE;

    // After we've looked up the filenameShow from the provider, we should get back an
    // actual Show object.  This is true even if the show was not found; in that case,
    // we should get an instance of a FailedShow.
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
    @SuppressWarnings("FieldCanBeLocal")
    private String baseForRename = null;

    // Initially we create the FileEpisode with nothing more than the path.
    // Other information will flow in.
    public FileEpisode(Path p) {
        if (p == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        path = p;
        fileNameString = p.getFileName().toString();
        filenameSuffix = StringUtils.getExtension(fileNameString);
        checkFile(true);
        TVRenamer.parseFilename(this);
    }

    // Create FileEpisode with String; only for testing
    @SuppressWarnings("WeakerAccess")
    public FileEpisode(String filename) {
        if (filename == null) {
            logger.severe(FILE_EPISODE_NEEDS_PATH);
            throw new IllegalArgumentException(FILE_EPISODE_NEEDS_PATH);
        }
        path = Paths.get(filename);
        fileNameString = path.getFileName().toString();
        filenameSuffix = StringUtils.getExtension(fileNameString);
        checkFile(false);
    }

    // Create FileEpisode with no path; only for testing
    @SuppressWarnings("WeakerAccess")
    public FileEpisode() {
        // We do not provide any way to create a FileEpisode with a null path
        // via the UI -- why would we?  Ultimately the program is to rename and
        // move files, and if there's no file, there's no point.  However, that's
        // not as true for testing.  There's a lot we do with a FileEpisode before
        // we ever get around to renaming it, and we shouldn't need to create
        // actual files on the file system just to test the functionality.
        fileNameString = "";
        filenameSuffix = "";
        // now do what setPath() would do
        exists = false;
        fileStatus = FileStatus.NO_FILE;
        fileSize = NO_FILE_SIZE;
    }

    public String getFilenameShow() {
        return filenameShow;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
    }

    public int getSeasonNum() {
        return seasonNum;
    }

    public String getFilenameSeason() {
        return filenameSeason;
    }

    public void setFilenameSeason(String filenameSeason) {
        this.filenameSeason = filenameSeason;
        try {
            seasonNum = Integer.parseInt(filenameSeason);
        } catch (Exception e) {
            seasonNum = Show.NO_SEASON;
        }
    }

    public int getEpisodeNum() {
        return episodeNum;
    }

    public String getFilenameEpisode() {
        return filenameEpisode;
    }

    public void setFilenameEpisode(String filenameEpisode) {
        this.filenameEpisode = filenameEpisode;
        try {
            episodeNum = Integer.parseInt(filenameEpisode);
        } catch (Exception e) {
            episodeNum = Show.NO_EPISODE;
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

    public String getFilenameSuffix() {
        return filenameSuffix;
    }

    public Path getPath() {
        return path;
    }

    private void checkFile(boolean mustExist) {
        if (Files.exists(path)) {
            exists = true;
            try {
                fileStatus = FileStatus.ORIGINAL;
                fileSize = Files.size(path);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "couldn't get size of " + path, ioe);
                fileStatus = FileStatus.NO_FILE;
                fileSize = NO_FILE_SIZE;
            }
        } else {
            if (mustExist) {
                logger.warning("creating FileEpisode for nonexistent path, " + path);
            }
            exists = false;
            fileStatus = FileStatus.NO_FILE;
            fileSize = NO_FILE_SIZE;
        }
    }

    public void setPath(Path p) {
        path = p;
        fileNameString = path.getFileName().toString();

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
        return path.toAbsolutePath().toString();
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

    public void setEpisodeShow(Show show) {
        actualShow = show;
        if (actualShow == null) {
            logger.warning("setEpisodeShow should never be called with null");
            seriesStatus = SeriesStatus.UNFOUND;
        } else if (actualShow instanceof FailedShow) {
            seriesStatus = SeriesStatus.UNFOUND;
        } else {
            seriesStatus = SeriesStatus.GOT_SHOW;
        }
    }

    public boolean listingsComplete() {
        if (actualShow == null) {
            logger.warning("error: should not get listings, do not have show!");
            seriesStatus = SeriesStatus.UNFOUND;
            return false;
        }

        if (actualShow instanceof FailedShow) {
            logger.warning("error: should not get listings, have a failed show!");
            seriesStatus = SeriesStatus.UNFOUND;
            return false;
        }

        actualEpisode = actualShow.getEpisode(seasonNum, episodeNum);
        if (actualEpisode == null) {
            logger.log(Level.SEVERE, "Season #" + seasonNum + ", Episode #"
                       + episodeNum + " not found for show '"
                       + filenameShow + "'");
            seriesStatus = SeriesStatus.NO_LISTINGS;
            return false;
        }

        // Success!!!
        seriesStatus = SeriesStatus.GOT_LISTINGS;
        return true;
    }

    public void listingsFailed(Exception err) {
        seriesStatus = SeriesStatus.NO_LISTINGS;
        if (err != null) {
            logger.log(Level.WARNING, "failed to get listings for " + this, err);
        }
        if (actualShow == null) {
            logger.warning("error: should not have tried to get listings, do not have show!");
        } else if (actualShow instanceof FailedShow) {
            logger.warning("error: should not have tried to get listings, have a failed show!");
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
        } else if (actualShow instanceof FailedShow) {
            logger.warning("error: should not get move-to directory, have a failed show!");
        } else {
            String dirname = actualShow.getDirName();
            destPath = destPath + FILE_SEPARATOR_STRING + dirname;

            // Now we might append the "season" directory, if the user requested it in
            // the preferences.  But, only if we actually *have* season information.
            if (seasonNum > Show.NO_SEASON) {
                String seasonPrefix = userPrefs.getSeasonPrefix();
                // Defect #50: Only add the 'season #' folder if set,
                // otherwise put files in showname root
                if (StringUtils.isNotBlank(seasonPrefix)) {
                    String seasonString = userPrefs.isSeasonPrefixLeadingZero()
                        ? StringUtils.zeroPadTwoDigits(seasonNum)
                        : String.valueOf(seasonNum);
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
            return path.toAbsolutePath().getParent();
        }
    }

    private String formatDate(LocalDate date, String format) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
        return dateFormat.format(date);
    }

    @SuppressWarnings("WeakerAccess")
    String getRenamedBasename() {
        String showName;
        if (actualShow == null) {
            logger.warning("should not be renaming without an actual Show.");
            showName = filenameShow;
        } else {
            if (actualShow instanceof FailedShow) {
                logger.warning("should not be renaming with a FailedShow.");
            }
            // We can use getName() even if it was a FailedShow
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
                logger.log(Level.WARNING, "Episode air date not found for '" + toString() + "'");
            }
        }

        String newFilename = userPrefs.getRenameReplacementString();

        // Ensure that all special characters in the replacement are quoted
        showName = Matcher.quoteReplacement(showName);
        showName = GlobalOverrides.getInstance().getShowName(showName);

        // Make whatever modifications are required
        String episodeNumberString = StringUtils.formatDigits(episodeNum);
        String episodeNumberWithLeadingZeros = StringUtils.zeroPadThreeDigits(episodeNum);
        String episodeTitleNoSpaces = Matcher.quoteReplacement(StringUtils.makeDotTitle(titleString));
        String seasonNumberWithLeadingZero = StringUtils.zeroPadTwoDigits(seasonNum);

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

        // Note, this is an instance variable, not a local variable.
        baseForRename = StringUtils.sanitiseTitle(newFilename);

        return baseForRename;
    }

    public String getDestinationBasename() {
        if (userPrefs.isRenameEnabled()) {
            return getRenamedBasename();
        } else {
            return StringUtils.removeLast(fileNameString, filenameSuffix);
        }
    }

    private String getShowNamePlaceholder() {
        return "<" + actualShow.getName() + ">";
    }

    /**
     * @return the new full file path (for table display) using {@link #getRenamedBasename()} and
     *          the destination directory
     */
    public String getReplacementText() {
        switch (seriesStatus) {
            case NOT_STARTED: {
                return ADDED_PLACEHOLDER_FILENAME;
            }
            case GOT_SHOW: {
                return getShowNamePlaceholder();
            }
            case UNFOUND: {
                return BROKEN_PLACEHOLDER_FILENAME;
            }
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
                    // This setting doesn't make any sense, but we haven't bothered to
                    // disallow it yet.
                    return fileNameString;
                }
            }
            case NO_LISTINGS: {
                return DOWNLOADING_FAILED;
            }
            default: {
                logger.warning("internal error, seriesStatus check apparently not exhaustive: "
                               + seriesStatus);
                return BROKEN_PLACEHOLDER_FILENAME;
            }
        }
    }

    @Override
    public String toString() {
        return "FileEpisode { title:" + filenameShow + ", season:" + seasonNum + ", episode:" + episodeNum
            + ", file:" + fileNameString + " }";
    }
}
