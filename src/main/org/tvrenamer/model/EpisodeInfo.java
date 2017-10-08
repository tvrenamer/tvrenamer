package org.tvrenamer.model;

public class EpisodeInfo {

    public final String episodeId;
    public final String seasonNumber;
    public final String episodeNumber;
    public final String episodeName;
    public final String firstAired;
    public final String dvdSeason;
    public final String dvdEpisodeNumber;

    public static class Builder {
        private String episodeId;
        private String seasonNumber;
        private String episodeNumber;
        private String episodeName;
        private String firstAired;
        private String dvdSeason;
        private String dvdEpisodeNumber;

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
    }

    public String toString() {
        return "dvd season " + dvdSeason + ", dvd episode " + dvdEpisodeNumber
            + ", " + episodeName + "; season " + seasonNumber + ", episode "
            + episodeNumber;
    }
}
