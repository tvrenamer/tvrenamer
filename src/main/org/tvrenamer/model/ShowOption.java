package org.tvrenamer.model;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShowOption {
    private static final Logger logger = Logger.getLogger(ShowOption.class.getName());

    final String idString;
    final String name;

    ShowOption(final String idString, final String name) {
        this.idString = idString;
        this.name = name;
    }

    /**
     * "Factory"-type static method to get an instance of a ShowOption.  Looks up the ID
     * in a hash table, and returns the object if it's already been created.  Otherwise,
     * we create a new ShowOption and return it.
     *
     * @param id
     *     The ID of this show, from the provider, as a String
     * @param name
     *     The proper name of this show, from the provider.  May contain a distinguisher,
     *     such as a year.
     * @return a ShowOption with the given ID
     */
    public static ShowOption getShowOption(String id, String name) {
        ShowOption matchedShowOption = Series.getExistingSeries(id);
        if (matchedShowOption != null) {
            return matchedShowOption;
        }
        return new ShowOption(id, name);
    }

    /**
     * Return a Show that represents this Show option.  May return itself if this
     * is already a Show.
     *
     * @return a version of this ShowOption that is an instance of a Show
     */
    public Show getShowInstance() {
        if (this instanceof Show) {
            return (Show) this;
        }
        // Note that at this point, "this" could be either a FailedShow or a ShowOption,
        // but we won't make the distinction.
        Show reified = Series.getExistingSeries(idString);
        if (reified != null) {
            return reified;
        }

        Integer parsedId;
        try {
            parsedId = Integer.parseInt(idString);
            return Series.createSeries(parsedId, name);
        } catch (Exception e) {
            String msg = "ShowOption could not be created with ID " + idString;
            logger.log(Level.WARNING, msg, e);
            return new Show(idString, name);
        }
    }

    /**
     * Return whether or not this is a "failed" show.
     *
     * @return true the show is "failed", false otherwise
     */
    public boolean isFailedShow() {
        return (this instanceof FailedShow);
    }

    /**
     * Get this FailedShow as its specific type.  Call {@link #isFailedShow}
     * before calling this.
     *
     * @return this as a FailedShow, or else throws an exception
     */
    public FailedShow asFailedShow() {
        if (this instanceof FailedShow) {
            return (FailedShow) this;
        }
        throw new IllegalStateException("cannot make FailedShow out of " + this);
    }

    /**
     * Return whether or not this show was successfully found in the
     * provider's data
     *
     * @return true the series is "valid", false otherwise
     */
    public boolean isValidSeries() {
        return (this instanceof Series);
    }

    /**
     * Get this Series as its specific type.  Call {@link #isValidSeries}
     * before calling this.
     *
     * @return this as a Series, or else throws an exception
     */
    public Series asSeries() {
        if (this instanceof Series) {
            return (Series) this;
        }
        throw new IllegalStateException("cannot make Series out of " + this);
    }

    /* Instance data */

    /**
     * Get this Show's actual, well-formatted name.  This may include a distinguisher,
     * such as a year, if the Show's name is not unique.  This may contain punctuation
     * characters which are not suitable for filenames, as well as non-ASCII characters.
     *
     * @return show name
     *            the name of the show from the provider
     */
    public String getName() {
        return name;
    }

    /**
     * Get this ShowOption's ID, as a String
     *
     * @return ID
     *            the ID of the show from the provider, as a String
     */
    public String getIdString() {
        return idString;
    }

    @Override
    public String toString() {
        return name + " (" + idString + ")";
    }
}
