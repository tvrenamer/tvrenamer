package org.tvrenamer.model;

public class ShowOption {

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
     * Return whether or not this show was successfully found in the
     * provider's data
     *
     * @return true the series is "valid", false otherwise
     */
    public boolean isValidSeries() {
        return (this instanceof Series);
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

    /* Instance data */

    final String idString;
    final String name;

    ShowOption(final String idString, final String name) {
        this.idString = idString;
        this.name = name;
    }

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

    /**
     * Get a Show that represents this ShowOption.  If this ShowOption is already
     * a Show (because Show is a subclass), just cast it and return it.
     *
     * @return ID
     *            the ID of the show from the provider, as a String
     */
    public Show getShowInstance() {
        if (this instanceof Show) {
            return (Show) this;
        }
        return Show.createShowInstance(idString, name);
    }

    @Override
    public String toString() {
        return name + " (" + idString + ") [ShowOption]";
    }
}
