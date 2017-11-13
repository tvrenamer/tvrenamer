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
    private final Integer airSeasonNumber;
    private final Integer airEpisodeNumber;
    private final Integer dvdSeasonNumber;
    private final Integer dvdEpisodeNumber;

    public Episode(EpisodeInfo info) {
        title = info.episodeName;
        episodeId = info.episodeId;

        airDateString = info.firstAired;

        // stringToInt handles null or empty values ok
        airSeasonNumber = StringUtils.stringToInt(info.seasonNumber);
        dvdSeasonNumber = StringUtils.stringToInt(info.dvdSeason);

        airEpisodeNumber = StringUtils.stringToInt(info.episodeNumber);
        dvdEpisodeNumber = StringUtils.stringToInt(info.dvdEpisodeNumber);
    }

    public String getTitle() {
        return title;
    }

    public String getEpisodeId() {
        return episodeId;
    }

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

    public Integer getSeasonNumber() {
        return airSeasonNumber;
    }

    public Integer getEpisodeNumber() {
        return airEpisodeNumber;
    }

    public Integer getDvdSeasonNumber() {
        return dvdSeasonNumber;
    }

    public Integer getDvdEpisodeNumber() {
        return dvdEpisodeNumber;
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

    @Override
    public String toString() {
        return "Episode " + title
            + ", firstAired=" + getAirDate() + "]";
    }
}
