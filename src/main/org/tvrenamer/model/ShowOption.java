package org.tvrenamer.model;

/**
 * Simple class -- basically a record -- to encapsulate information we received from
 * the provider about potential Shows.  We shouldn't create actual Show objects for
 * the options we reject.
 */
class ShowOption {

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
        ShowOption matchedShowOption = Show.getExistingShow(id);
        if (matchedShowOption != null) {
            return matchedShowOption;
        }
        return new ShowOption(id, name);
    }

    final String idString;
    final String name;

    ShowOption(final String idString, final String name) {
        this.idString = idString;
        this.name = name;
    }

    /**
     * Get this Show's ID, as a String.
     *
     * @return ID
     *            the ID of the show from the provider, as a String
     */
    @SuppressWarnings("unused")
    public String getIdString() {
        return idString;
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

    public Show getShow() {
        if (this instanceof Show) {
            return (Show) this;
        }
        return Show.createShowInstance(idString, name);
    }

    @Override
    public String toString() {
        return name + " (" + idString + ")";
    }
}
