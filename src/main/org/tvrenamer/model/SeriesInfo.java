package org.tvrenamer.model;

public class SeriesInfo {
    public Integer id;
    public String seriesName;

    SeriesInfo(final Integer id, final String seriesName) {
        this.id = id;
        this.seriesName = seriesName;
    }

    /**
     * Get this SeriesInfo's actual, well-formatted name.  This may include a distinguisher,
     * such as a year, if the show's name is not unique.  This may contain punctuation
     * characters which are not suitable for filenames, as well as non-ASCII characters.
     *
     * @return the seriesName of the show from the provider
     */
    public String getSeriesName() {
        return seriesName;
    }

    /**
     * Get this SeriesInfo's ID, as an Integer
     *
     * @return ID
     *            the ID of the show from the provider, as an Integer
     */
    public Integer getIdNum() {
        return id;
    }

    @Override
    public String toString() {
        return seriesName + " (" + id + ")";
    }
}
