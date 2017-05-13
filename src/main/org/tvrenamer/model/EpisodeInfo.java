package org.tvrenamer.model;

public class EpisodeInfo {

    public final String episodeId;
    public final String seasonNumber;
    public final String episodeNumber;
    public final String episodeName;
    public final String firstAired;
    // public final String overview;
    // public final String productionCode;
    // public final String language;
    // public final String id;
    // public final String seriesid;
    // public final String seasonid;
    // public final String lastupdated;
    public final String dvdSeason;
    public final String dvdEpisodeNumber;
    public final String absoluteNumber;
    public final String seriesId;

    public static class Builder {
        private String episodeId;
        private String seasonNumber;
        private String episodeNumber;
        private String episodeName;
        private String firstAired;
        private String dvdSeason;
        private String dvdEpisodeNumber;
        private String absoluteNumber;
        private String seriesId;

        public Builder() {
        }

        public Builder episodeId(String val) {
            episodeId = val;
            return this;
        }

        public Builder seasonNumber(String val) {
            seasonNumber = val;
            return this;
        }

        public Builder episodeNumber(String val) {
            episodeNumber = val;
            return this;
        }

        public Builder episodeName(String val) {
            episodeName = val;
            return this;
        }

        public Builder firstAired(String val) {
            firstAired = val;
            return this;
        }

        public Builder dvdSeason(String val) {
            dvdSeason = val;
            return this;
        }

        public Builder dvdEpisodeNumber(String val) {
            dvdEpisodeNumber = val;
            return this;
        }

        public Builder absoluteNumber(String val) {
            absoluteNumber = val;
            return this;
        }

        public Builder seriesId(String val) {
            seriesId = val;
            return this;
        }

        public EpisodeInfo build() {
            return new EpisodeInfo(this);
        }
    }

    private EpisodeInfo(Builder builder) {
        episodeId = builder.episodeId;
        seasonNumber = builder.seasonNumber;
        episodeNumber = builder.episodeNumber;
        episodeName = builder.episodeName;
        firstAired = builder.firstAired;
        dvdSeason = builder.dvdSeason;
        dvdEpisodeNumber = builder.dvdEpisodeNumber;
        absoluteNumber = builder.absoluteNumber;
        seriesId = builder.seriesId;
    }
}
