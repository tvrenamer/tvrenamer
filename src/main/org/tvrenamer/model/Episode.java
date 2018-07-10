package org.tvrenamer.model;

import org.tvrenamer.controller.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

public class Episode {
    private static final Logger logger = Logger.getLogger(Episode.class.getName());

    private static final String EPISODE_DATE_FORMAT = "yyyy-MM-dd";
    // Unlike java.text.DateFormat, DateTimeFormatter is thread-safe, so we can create just one instance here.
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(EPISODE_DATE_FORMAT);

    private final String title;
    private final String episodeId;

    private final String airDateString;
    // Not final; we don't calculate it in the constructor.  When we look up a series, we process every
    // episode of that series, whether the user has that episode or not.  But we only need to know the
    // air date for episodes the user actually has.  We parse the string only on demand.
    private LocalDate firstAired = null;

    // This object does not have an opinion of its place within the series ordering.
    // It does serve as a useful place to hang information about such questions, as
    // we do, below.  But it's up to the Show to decide what the "real" answer is.
    private final EpisodePlacement airPlacement;
    private final EpisodePlacement dvdPlacement;

    /**
     * Constructs an Episode from the given EpisodeInfo
     *
     * @param info
     *   the EpisodeInfo to use to create this Episode
     */
    public Episode(EpisodeInfo info) {
        title = info.episodeName;
        episodeId = info.episodeId;

        airDateString = info.firstAired;

        // stringToInt handles null or empty values ok
        final Integer airSeasonNumber = StringUtils.stringToInt(info.seasonNumber);
        final Integer airEpisodeNumber = StringUtils.stringToInt(info.episodeNumber);
        final Integer dvdSeasonNumber = StringUtils.stringToInt(info.dvdSeason);
        final Integer dvdEpisodeNumber = StringUtils.stringToInt(info.dvdEpisodeNumber);

        if (airSeasonNumber == null) {
            logger.warning("episode \"" + title + "\" does not have an integer season ("
                           + info.seasonNumber + ")");
            airPlacement = null;
        } else if (airEpisodeNumber == null) {
            logger.info("episode \"" + title + "\" does not have an integer episode number ("
                        + info.episodeNumber + ")");
            airPlacement = null;
        } else {
            airPlacement = new EpisodePlacement(airSeasonNumber, airEpisodeNumber);
        }

        if (dvdSeasonNumber == null) {
            logger.finer("episode \"" + title + "\" does not have an integer DVD season ("
                         + info.dvdSeason + ")");
            dvdPlacement = null;
        } else if (dvdEpisodeNumber == null) {
            logger.fine("episode \"" + title + "\" does not have an integer DVD episode number ("
                        + info.dvdEpisodeNumber + ")");
            dvdPlacement = null;
        } else {
            dvdPlacement = new EpisodePlacement(dvdSeasonNumber, dvdEpisodeNumber);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    /**
     * Gets the air date of this episode.<p>
     *
     * We download complete listings of every show we're interested in, and parse
     * those listings.  Parsing the air date is slightly expensive, and some shows
     * have hundreds of episodes.  It's likely that the user has only a small subset
     * of those episodes that they want to rename, and we don't actually need to
     * parse the air date of the episodes that they're not interested in.<p>
     *
     * So, we parse it somewhat lazily.  We allow it to be null until someone calls
     * this method, at which point, we parse it, and store the result in the instance
     * variable.  If the method is called again, it will simply return the already
     * parsed data in the instance variable.<p>
     *
     * The provider should give us an air date for every episode it has, but we can't
     * guarantee that, and we may have seen exceptions in the past.  So this method
     * is ready to handle a lack of air date information.  If no information is found,
     * we supply "now" as the air date.<p>
     *
     * We also supply "now" as the air date if we do get information, but are unable
     * to parse that information.  In that case, we also log an error, which we don't
     * do if the air date information is simply missing.<p>
     *
     * @return the LocalDate that represents the time and date that this episode was
     *   first aired, or "now" if we can't get that information
     */
    public LocalDate getAirDate() {
        if (firstAired == null) {
            if (StringUtils.isBlank(airDateString)) {
                // When no value (good or bad) is found, use "now".
                firstAired = LocalDate.now();
            } else {
                try {
                    firstAired = LocalDate.parse(airDateString, DATE_FORMATTER);
                } catch (DateTimeParseException e) {
                    // While a null or empty string is not considered an error,
                    // a badly formatted string is.
                    logger.severe("could not parse as date: " + airDateString);
                    firstAired = LocalDate.now();
                }
            }
        }
        return firstAired;
    }

    /**
     * Returns the requested {@link EpisodePlacement} for this Episode.<p>
     *
     * This object represents a particular episode of a given series, and that
     * episode may have been placed in a different ordering on the DVD release
     * than it was when it originally aired.  The caller must tell us whether
     * they want the DVD information or the over-the-air information.
     *
     * @param useDvd
     *    supply true if they want the DVD ordering, false otherwise
     * @return
     *    the requested EpisodePlacement for this episode
     */
    public EpisodePlacement getEpisodePlacement(boolean useDvd) {
        if (useDvd) {
            return dvdPlacement;
        }
        return airPlacement;
    }

    // "Package-private".  Used by Show; should not be used by other classes.
    String getDifferenceMessage(EpisodeInfo info) {
        if (StringUtils.stringsAreEqual(title, info.episodeName)) {
            if (StringUtils.stringsAreEqual(airDateString, info.firstAired)) {
                return null;
            } else {
                return "different airdate: " + info.episodeName
                    + " was " + airDateString
                    + ", now " + info.firstAired;
            }
        } else if (StringUtils.stringsAreEqual(airDateString, info.firstAired)) {
            return "different title: " + info.episodeId
                + " was " + title
                + ", now " + info.episodeName;
        } else {
            return "different title: " + info.episodeId
                + " was " + title
                + ", now " + info.episodeName
                + " and different airdate: " + info.episodeName
                + " was " + airDateString
                + ", now " + info.firstAired;
        }
    }

    /**
     * Standard object method to represent this Episode as a String.
     *
     * @return string which gives the title and air date of this Episode
     */
    @Override
    public String toString() {
        return "Episode " + title
            + ", firstAired=" + getAirDate() + "]";
    }
}
