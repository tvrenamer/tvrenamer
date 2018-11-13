// FileEpisode - represents a file on disk which is presumed to contain a single
//   episode of a TV show.
//
// This is a very mutable class.  It is initially created with just a path,
// and then information comes streaming in.
//

package org.tvrenamer.model;

import static org.tvrenamer.model.ReplacementToken.*;
import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.FilenameParser;
import org.tvrenamer.controller.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
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

    private static final long NO_FILE_SIZE = -1L;

    // Allow titles long enough to include this one:
    // "The One With The Thanksgiving Flashbacks (a.k.a. The One With All The Thanksgivings)"
    private static final int MAX_TITLE_LENGTH = 85;

    // This class actually figures out the proposed new name for the file, so we need
    // a link to the user preferences to know how the user wants the file renamed.
    private static final UserPreferences userPrefs = UserPreferences.getInstance();

    // This is the one final field in this class; it's the one thing that should never
    // change in a FileEpisode.  It could be the empty string (though it would be unusual).
    // If the file does actually have a suffix, this variable *includes* the leading dot.
    private final String filenameSuffix;

    // These four fields reflect the information derived from the filename.  In particular,
    // filenameShow is based on the part of the filename we "guessed" represented the name
    // of the show, and which we use to query the provider.  Note that the actual show name
    // that we get back from the provider will likely differ from what we have here.
    private String filenameShow = "";
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
    private List<Episode> actualEpisodes = null;
    private int chosenEpisode = 0;

    // The state of this object, not the state of the actual TV episode.
    private ParseStatus parseStatus = ParseStatus.UNPARSED;
    private SeriesStatus seriesStatus = SeriesStatus.NOT_STARTED;

    public String fileStatus = "UNCHECKED";
    private Boolean originalFileInPlace = null;
    private boolean currentPathMatchesTemplate = false;
    private boolean moveInProgress = false;
    // These values are not necessarily eternal
    private boolean moveHasBeenAttempted = false;
    private boolean moveHadConflict = false;
    private boolean moveHadError = false;

    public enum FailureReason {
        ORIGINAL_MISSING,
    }

    private List<String> replacementOptions = null;
    private String replacementText = ADDED_PLACEHOLDER_FILENAME;
    private String reasonIgnored = null;

    // The originalBasename is what you get when you take original file path, remove the
    // directory, and remove the file suffix.  For situations when the user wants to move,
    // but not rename the file, the originalBasename is what the target will be based on.
    private String originalBasename;

    // This is the basic part of what we would rename the file to.  That is, we would
    // rename it to destinationFolder + baseForRename + filenameSuffix.
    private String baseForRename = null;

    /**
     * Standard constructor for a FileEpisode; takes a Path.<p>
     *
     * Initially we create the FileEpisode with nothing more than the path.
     * Other information will flow in.
     *
     * @param p
     *   the Path of the file this FileEpisode represents
     */
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
        originalBasename = StringUtils.removeLast(fileNameString, filenameSuffix);
        checkFile(true);
        FilenameParser.parseFilename(this);
    }

    /**
     * Test constructor to make a FileEpisode from a String.<p>
     *
     * This constructor is intended for use only by the test framework.
     *
     * @param filename
     *    a String representing a path to a file (which doesn't need to exist).
     */
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
        originalBasename = StringUtils.removeLast(fileNameString, filenameSuffix);
        checkFile(false);
    }

    public String getFilenameShow() {
        return filenameShow;
    }

    public void setFilenameShow(String filenameShow) {
        this.filenameShow = filenameShow;
    }

    public EpisodePlacement getEpisodePlacement() {
        return placement;
    }

    /**
     * Sets the {@link EpisodePlacement}.<p>
     *
     * A given episode occurs within a particular season.  To identify the episode,
     * we need to find, from the filename, the season in which it aired, and where
     * it aired within that season.  We combine these two pieces of information into
     * an class we call the "EpisodePlacement".
     *
     * This method expects to receive the exact substrings we extracted from the
     * filename, parses them, creates an EpisodePlacement, and stores it within
     * this FileEpisode.
     *
     * @param filenameSeason
     *   the substring of the filename that indicates the episode's season
     * @param filenameEpisode
     *   the substring of the filename that indicates the episode's ordering within
     *   the season
     */
    public void setEpisodePlacement(String filenameSeason, String filenameEpisode) {
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

    /**
     * Gets the screen resolution found in the filename.<p>
     *
     * The filename may indicate a screen resolution (i.e., number of pixels) of the
     * video file.  This method has nothing to do with how the filename was "resolved";
     * that's just an unfortunate ambiguity.  This is just about having found substrings
     * like "720p" or "1080i".
     *
     * @return
     *   the substring of the filename that indicated a screen resolution
     */
    public String getFilenameResolution() {
        return filenameResolution;
    }

    /**
     * Sets the screen resolution found in the filename.<p>
     *
     * The filename may indicate a screen resolution (i.e., number of pixels) of the
     * video file.  This method has nothing to do with "resolving" the filename;
     * that's just an unfortunate ambiguity.  This is just about finding substrings
     * like "720p" or "1080i".
     *
     * @param filenameResolution
     *   the substring of the filename that indicates a screen resolution
     */
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

    public String getFileName() {
        return fileNameString;
    }

    /**
     * Updates the status to know that the source file is not found.
     *
     */
    public void setNoFile() {
        fileStatus = "NO_FILE";

        originalFileInPlace = false;
    }

    /**
     * Updates the status to know that the source file has been found.
     *
     */
    public void setFileVerified() {
        fileStatus = "UNMOVED";
        originalFileInPlace = true;
        moveInProgress = false;
    }

    private void checkFile(boolean mustExist) {
        if (Files.exists(pathObj)) {
            setFileVerified();
            try {
                fileSize = Files.size(pathObj);
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "couldn't get size of " + pathObj, ioe);
                setNoFile();
                fileSize = NO_FILE_SIZE;
            }
        } else {
            if (mustExist) {
                logger.warning("creating FileEpisode for nonexistent path, " + pathObj);
            }
            setNoFile();
            fileSize = NO_FILE_SIZE;
        }
    }

    /**
     * Sets the Path for the file that this FileEpisode refers to.<p>
     *
     * The path is originally set in the constructor.  A given FileEpisode is meant to
     * refer to a given file; they should not be reused.  This method should not be
     * called to try to get a FileEpisode to now refer to an unrelated file.<p>
     *
     * However, the point of this program is to move files, and, depending on the user
     * preference settings, the item may remain in the table after it's been moved.
     * We want to keep everything up to date, so when, at the user's request, we do
     * move a file, we want to update the model so that it now knows where it is.<p>
     *
     * It is illegal to try to set the path to a filename that does not have the same
     * file suffix as the original.  The idea is that the original file was moved/renamed,
     * and that should never require changing the file suffix.  If you're trying to re-use
     * a FileEpisode for a Path with a different file extension, you're likely doing the
     * wrong thing.
     *
     * @param p
     *    the new Path of the file that this FileEpisode was created to refer to
     */
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
        originalBasename = StringUtils.removeLast(fileNameString, filenameSuffix);
        baseForRename = getRenamedBasename(chosenEpisode);
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

    /**
     * Returns the number of options found for this FileEpisode.<p>
     *
     * This may be:<ul>
     * <li>0, because<ul>
     *    <li>we could not parse the filename</li>
     *    <li>we have not (yet) obtained enough information from the provider</li>
     *    <li>the "ignore files" setting says to ignore this item</li>
     *    <li>the episode was simply not found within the listings we downloaded</li>
     *    </ul>
     * <li>1, meaning we found an exact episode that this maps to</li>
     * <li>2, presumably meaning that the given placement (season X, episode Y)
     *    maps to different episodes depending on whether you assume the over-the-air
     *    ordering, or the DVD ordering</li>
     * <li>(theoretically) more than 2, for some unforeseen reason that the listings
     *    we downloaded contains multiple hits for the given placement</li>
     * </ul>
     *
     * @return the number of options found for this FileEpisode
     */
    public synchronized int optionCount() {
        if (seriesStatus != SeriesStatus.GOT_LISTINGS) {
            return 0;
        }
        if (reasonIgnored != null) {
            return 0;
        }
        if (replacementOptions == null) {
            // This should never happen; if we have GOT_LISTINGS,
            // replacementOptions should be initialized
            logger.warning("error: replacementOptions is null despite GOT_LISTINGS");
            return 0;
        }
        return replacementOptions.size();
    }

    /**
     * Update this object to know that its path has been parsed successfully.
     *
     * This causes it to update its replacementText to notify the user as such.
     *
     */
    public void setParsed() {
        parseStatus = ParseStatus.PARSED;
        replacementText = ADDED_PLACEHOLDER_FILENAME;
    }

    /**
     * Update this object to know that its path has failed to parse.
     *
     * This causes it to update its replacementText to notify the user as such.
     *
     */
    public void setFailToParse() {
        parseStatus = ParseStatus.BAD_PARSE;
        replacementText = BAD_PARSE_MESSAGE;
    }

    /**
     * Update this object to know that we are unable to look up its show,
     * apparently because the provider we're using has changed its API.
     *
     * (More technically, this means we got a "not found" when we tried to
     * make the REST call.)
     *
     * This causes it to update its replacementText to notify the user as such.
     *
     */
    public void setApiDiscontinued() {
        parseStatus = ParseStatus.PARSED;
        seriesStatus = SeriesStatus.UNFOUND;
        replacementText = DOWNLOADING_FAILED;
    }

    /**
     * Updates the status to know that the process of moving the file to its
     * desired name/location has begun, and is not known to have finished.
     *
     */
    public void setMoving() {
        fileStatus = "MOVING";
        originalFileInPlace = true;
        moveInProgress = true;
        // Since we are starting a new move, we reset these values.
        moveHasBeenAttempted = false;
        moveHadConflict = false;
        moveHadError = false;
    }

    /**
     * Updates the status to know that the source file is already in the
     * directory that the user wants it in, and has the best name that it
     * can be given.
     *
     */
    public void setAlreadyInPlace() {
        fileStatus = "ALREADY_IN_PLACE";
        originalFileInPlace = true;
        currentPathMatchesTemplate = true;
        moveInProgress = false;
        moveHadError = false;
    }

    /**
     * Updates the status to know that there is already a file with the desired
     * name/location where the user's template requests we move the source file.
     *
     */
    public void setConflict() {
        moveHadConflict = true;
    }

    /**
     * Updates the status to know that we have successfully "moved" the file to
     * the name/location where the user's template specified.  At the filesystem
     * level, this could mean the file was renamed, or that it was copied and
     * the original deleted.
     *
     */
    public void setRenamed() {
        fileStatus = "RENAMED";
        originalFileInPlace = false;
        currentPathMatchesTemplate = true;
        moveInProgress = false;
        moveHasBeenAttempted = true;
        moveHadConflict = false;
        moveHadError = false;
    }

    /**
     * Updates the status to know that we successfully copied the file to the
     * desired name/location, but we have not (yet?) deleted the original file.
     * If the move is in this state at the completion of the process, it means
     * we were not able to delete the original file.
     *
     */
    public void setCopied() {
        fileStatus = "COPIED";
        originalFileInPlace = true;
        currentPathMatchesTemplate = true;
        moveInProgress = false;
        moveHasBeenAttempted = true;
        moveHadConflict = false;
    }

    /**
     * Updates the status to know that we tried to move the file, but were not
     * able to.  This could be for any number of reasons.  But whatever the
     * reason, it indicates that the original file is still where it originally
     * was, and that the destination file to which we tried to move the file,
     * does not exist.
     *
     */
    public void setFailToMove() {
        fileStatus = "FAIL_TO_MOVE";
        originalFileInPlace = true;
        currentPathMatchesTemplate = false;
        moveInProgress = false;
        moveHasBeenAttempted = true;
        moveHadConflict = false;
        moveHadError = true;
    }

    /**
     * Updates the status to know that the original file has been moved, but
     * that it was not moved to where we expected.  Thankfully, this is is very
     * likely impossible, but want to have infrastructure for it, just in case.
     *
     */
    public void setMisnamed() {
        fileStatus = "MISNAMED";
        originalFileInPlace = false;
        currentPathMatchesTemplate = false;
        moveInProgress = false;
        moveHasBeenAttempted = true;
        moveHadError = true;
    }

    /**
     * Evaluates the status as "successful" or not.
     *
     * @return
     *   whether the status indicates "success" or not
     */
    public boolean isSuccess() {
        return currentPathMatchesTemplate;
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
        return BROKEN_PLACEHOLDER_FILENAME + " for \""
            + showName.getQueryString()
            + "\"";
    }

    private String getTimeoutPlaceholder() {
        ShowName showName = ShowName.lookupShowName(filenameShow);
        return TIMEOUT_DOWNLOADING + " \""
            + showName.getQueryString()
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
            replacementText = getNoShowPlaceholder();
        } else {
            seriesStatus = SeriesStatus.GOT_SHOW;
            replacementText = getShowNamePlaceholder();
        }
    }

    /**
     * Confirm the actualShow of this object to be null, and set the replacement text
     * to inform the user of what appears to have gone wrong.
     *
     * @param failedShow
     *    the FailedShow object that represents the failure
     */
    public void setFailedShow(FailedShow failedShow) {
        actualShow = null;
        seriesStatus = SeriesStatus.UNFOUND;
        if (failedShow.isTimeout()) {
            replacementText = getTimeoutPlaceholder();
        } else {
            replacementText = getNoShowPlaceholder();
        }
    }

    /**
     *
     * @return the number of episode options to offer the user
     */
    public int listingsComplete() {
        chosenEpisode = 0;
        if (actualShow == null) {
            logger.warning("error: should not get listings, do not have show!");
            seriesStatus = SeriesStatus.NOT_STARTED;
            replacementText = BAD_PARSE_MESSAGE;
            return 0;
        }

        if (actualShow.noEpisodes()) {
            seriesStatus = SeriesStatus.NO_LISTINGS;
            replacementText = getNoListingsPlaceholder();
            return 0;
        }

        actualEpisodes = actualShow.getEpisodes(placement);
        if ((actualEpisodes != null) && (actualEpisodes.size() == 0)) {
            actualEpisodes = null;
        }
        if (actualEpisodes == null) {
            logger.info("Season #" + placement.season + ", Episode #"
                        + placement.episode + " not found for show '"
                        + filenameShow + "'");
            seriesStatus = SeriesStatus.NO_MATCH;
            replacementText = getNoMatchPlaceholder();
            return 0;
        }

        // Success!!!
        synchronized (this) {
            buildReplacementTextOptions();

            if (reasonIgnored != null) {
                return 0;
            }

            return replacementOptions.size();
        }
    }

    /**
     *
     * @param err
     *    an exception that may have occurred while trying to get the listings
     *    (could be null)
     */
    public void listingsFailed(Exception err) {
        seriesStatus = SeriesStatus.NO_LISTINGS;
        replacementText = getNoListingsPlaceholder();
        if (err != null) {
            logger.log(Level.WARNING, "failed to get listings for " + this, err);
        }
        if (actualShow == null) {
            logger.warning("error: should not have tried to get listings, do not have show!");
        }
    }

    /**
     * Returns the directory to which the file should be moved.<p>
     *
     * @return the new Path into which this file would be moved, based on the information
     *         we've gathered, and the user's preferences
     */
    public Path getMoveToPath() {
        Path destPath = userPrefs.getDestinationDirectory();
        if (destPath == null) {
            return pathObj.toAbsolutePath().getParent();
        } else {
            if (actualShow == null) {
                logger.warning("error: should not get move-to directory, do not have show!");
            } else {
                destPath = destPath.resolve(actualShow.getDirName());

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
                        destPath = destPath.resolve(seasonPrefix + seasonString);
                    }
                } else {
                    logger.fine("maybe should not get move-to directory, do not have season");
                }
            }
            return destPath;
        }
    }

    /**
     * Returns the pathname to which the file should be moved.
     *
     * <p>Takes the base part of the name as an argument, resolves it in the
     * destination directory, and returns it as a String.
     *
     * @param filename
     *   the filename to which we should move the file associated with this
     *   FileEpisode
     * @return
     *   the pathname to which we should move the file, as a String
     *
     */
    private String getMoveToFile(final String filename) {
        Path destPath = getMoveToPath();
        Path dest = destPath.resolve(filename);
        return dest.toString();
    }

    private static String formatDate(final LocalDate date, final String format) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(format);
        return dateFormat.format(date);
    }

    /**
     * Replace the control strings in the replacement template, with the episode information.
     *
     * This method is static to make it obvious that it doesn't rely on any instance variables;
     * since it also does not modify any class variables, it is a pure function, and safe to
     * call from any context.
     *
     * @param replacementTemplate
     *     the template provided by the user via the preferences dialog
     * @param actualShow
     *     the TV show that we have determined matches this FileEpisode
     * @param actualEpisode
     *     the episode that we, possibly with help from the user, have determined matches
     * @param placement
     *     the season number and episode number information we obtained from the filename
     * @param resolution
     *     the screen resolution (e.g., "720p", etc.) we obtained from the filename
     * @return the template string with the episode information replacing the control strings
     */
    @SuppressWarnings("WeakerAccess")
    static String plugInInformation(final String replacementTemplate,
                                    final Show actualShow, final Episode actualEpisode,
                                    final EpisodePlacement placement, final String resolution)
    {
        final String showName = actualShow.getName();
        String episodeTitle = actualEpisode.getTitle();
        int len = episodeTitle.length();
        if (len > MAX_TITLE_LENGTH) {
            logger.fine("truncating episode title to " + episodeTitle);
            episodeTitle = episodeTitle.substring(0, MAX_TITLE_LENGTH);
        }
        String newFilename = replacementTemplate
            .replaceAll(SEASON_NUM.getToken(),
                        String.valueOf(placement.season))
            .replaceAll(SEASON_NUM_LEADING_ZERO.getToken(),
                        StringUtils.zeroPadTwoDigits(placement.season))
            .replaceAll(EPISODE_NUM.getToken(),
                        StringUtils.formatDigits(placement.episode))
            .replaceAll(EPISODE_NUM_LEADING_ZERO.getToken(),
                        StringUtils.zeroPadThreeDigits(placement.episode))
            .replaceAll(SHOW_NAME.getToken(),
                        Matcher.quoteReplacement(showName))
            .replaceAll(EPISODE_TITLE.getToken(),
                        Matcher.quoteReplacement(episodeTitle))
            .replaceAll(EPISODE_TITLE_NO_SPACES.getToken(),
                        Matcher.quoteReplacement(StringUtils.makeDotTitle(episodeTitle)))
            .replaceAll(EPISODE_RESOLUTION.getToken(),
                        resolution);

        // Date and times
        final LocalDate airDate = actualEpisode.getAirDate();
        if (airDate == null) {
            logger.log(Level.WARNING, "Episode air date not found for " + showName
                       + ", " + placement + ", \"" + episodeTitle + "\"");
        }
        // If the airDate is null, we warn (above) but we go ahead and do the substitution anyway;
        // if the date is null, we need to replace the control strings with the empty string.
        newFilename = plugInAirDate(airDate, newFilename);

        return StringUtils.sanitiseTitle(newFilename);
    }

    private static String removeTokens(final String orig, final ReplacementToken... tokens) {
        String removed = orig;

        for (ReplacementToken token : tokens) {
            removed = removed.replaceAll(token.getToken(), "");
        }
        return removed;
    }

    /**
     * Replace the date control strings in the template, with the episode air date information.
     * May be called with null if the episode in question doesn't have air date information.
     *
     * This method is static to make it obvious that it doesn't rely on any instance variables;
     * since it also does not modify any class variables, it is a pure function, and safe to
     * call from any context.
     *
     * @param airDate
     *     the date information we obtained from the episode; may be null
     * @param template
     *     the replacement template provided by the user via the preferences dialog; may be
     *     partially filled in already, of course.
     * @return the template string with the air date information replacing the control strings
     */
    @SuppressWarnings("WeakerAccess")
    static String plugInAirDate(final LocalDate airDate, final String template) {
        // Date and times
        if (airDate == null) {
            return removeTokens(template,
                                DATE_DAY_NUM, DATE_DAY_NUMLZ,
                                DATE_MONTH_NUM, DATE_MONTH_NUMLZ,
                                DATE_YEAR_FULL, DATE_YEAR_MIN);
        } else {
            return template
                .replaceAll(DATE_DAY_NUM.getToken(),
                            formatDate(airDate, "d"))
                .replaceAll(DATE_DAY_NUMLZ.getToken(),
                            formatDate(airDate, "dd"))
                .replaceAll(DATE_MONTH_NUM.getToken(),
                            formatDate(airDate, "M"))
                .replaceAll(DATE_MONTH_NUMLZ.getToken(),
                            formatDate(airDate, "MM"))
                .replaceAll(DATE_YEAR_FULL.getToken(),
                            formatDate(airDate, "yyyy"))
                .replaceAll(DATE_YEAR_MIN.getToken(),
                            formatDate(airDate, "yy"));
        }
    }

    /**
     * Calculates the destination basename for this FileEpisode.<p>
     *
     * Ultimately, the destination for where we move a file to has four parts:<ol>
     *  <li>the destination directory -- specified by the user in the preferences</li>
     *  <li>an optional subdirectory -- templates specified by the user in the
     *        preferences, and filled in by getMoveToFile()</li>
     *  <li>the basename of the file, which is what this method constructs</li>
     *  <li>the file suffix, which is determined by the original filename, and
     *        not changeable</li></ol><p>
     *
     * To get the basename, we use the template provided by the user in the
     * preferences, and plug in the information we found about the actual show
     * and the actual episode.  We may have found more than one matching episode;
     * the argument to this method tells us which option to use.
     *
     * @param n
     *    the episode option to get the basename of
     * @return the basename to use for the replacement file
     */
    String getRenamedBasename(final int n) {
        if (!userPrefs.isRenameSelected()) {
            return null;
        }

        if (actualShow == null) {
            logger.severe("cannot rename without an actual Show.");
            return originalBasename;
        }
        if (actualEpisodes == null) {
            logger.severe("should not be renaming when have no actual episodes");
            return originalBasename;
        }
        if (actualEpisodes.size() <= n) {
            logger.severe("cannot get option " + n + " of " + this);
            return originalBasename;
        }

        return plugInInformation(userPrefs.getRenameReplacementString(),
                                 actualShow, actualEpisodes.get(n),
                                 placement, filenameResolution);
    }

    /**
     *
     * @param n
     *    the option chosen
     */
    public void setChosenEpisode(final int n) {
        if (n >= actualEpisodes.size()) {
            logger.warning("no option " + n + " for " + this);
        } else {
            int previous = chosenEpisode;
            chosenEpisode = n;
            if (chosenEpisode != previous) {
                logger.info("changing episode from " + actualEpisodes.get(previous).getTitle()
                            + " to " + actualEpisodes.get(chosenEpisode).getTitle());
                baseForRename = getRenamedBasename(chosenEpisode);
            }
        }
    }

    /**
     * Get the currently chosen option
     *
     * @return
     *    the option currently chosen
     */
    public int getChosenEpisode() {
        return chosenEpisode;
    }

    /**
     * Retrieves the "basename" for the proposed destination for this file.<p>
     *
     * The "basename" is what you get when you take a file path, remove the directory,
     * and remove the file suffix.<p>
     *
     * The user has the option of "moving" the file to a different directory, but not
     * renaming it.  When that option is selected, the destination basename is simply
     * the original basename.<p>
     *
     * When rename is enabled, to get the destination basename, we use the template
     * provided by the user in  the preferences, and plug in the information we found
     * about the actual show and the actual episode.
     *
     * @return the "basename" of the proposed destination for this file
     */
    public String getDestinationBasename() {
        if (userPrefs.isRenameSelected()) {
            if (baseForRename == null) {
                logger.warning("unable to get destination basename; "
                               + "reverting to original basename "
                               + originalBasename);
                return originalBasename;
            }
            return baseForRename;
        } else {
            return originalBasename;
        }
    }

    /**
     * Build the new full file path options (for table display) using {@link #getRenamedBasename(int)}
     * and the destination directory
     *
     */
    private synchronized void buildReplacementTextOptions() {
        seriesStatus = SeriesStatus.GOT_LISTINGS;
        replacementOptions = new LinkedList<>();
        if (userPrefs.isRenameSelected()) {
            for (int i=0; i < actualEpisodes.size(); i++) {
                String newBasename = getRenamedBasename(i);
                if (i == chosenEpisode) {
                    baseForRename = newBasename;
                }

                if (userPrefs.isMoveSelected()) {
                    replacementOptions.add(getMoveToFile(newBasename + filenameSuffix));
                } else {
                    replacementOptions.add(newBasename + filenameSuffix);
                }
            }
        } else if (userPrefs.isMoveSelected()) {
            replacementOptions.add(getMoveToFile(fileNameString));
        } else {
            // This setting doesn't make any sense, but we haven't bothered to
            // disallow it yet.
            logger.severe("apparently both rename and move are disabled! This is not allowed!");
            replacementOptions.add(fileNameString);
        }
        replacementText = replacementOptions.get(chosenEpisode);
    }

    /**
     *
     * @param ignoreReason the substring that is contained in the filename that
     *   the user has told us to ignore
     */
    public void setIgnoreReason(final String ignoreReason) {
        reasonIgnored = ignoreReason;
    }

    /**
     *
     * @return the new full file path, or user message, for table display
     */
    public String getReplacementText() {
        if (reasonIgnored != null) {
            return "Ignoring file due to \"" + reasonIgnored + "\"";
        }
        return replacementText;
    }

    /**
     *
     * @return the new full file path options
     */
    public synchronized List<String> getReplacementOptions() {
        return replacementOptions;
    }

    /**
     * Refresh the proposed destination for this file episode, presumably after
     * the user has made a change to something like the replacement template,
     * the output destination folder, etc.
     *
     */
    public void refreshReplacement() {
        if (seriesStatus == SeriesStatus.GOT_LISTINGS) {
            buildReplacementTextOptions();
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
