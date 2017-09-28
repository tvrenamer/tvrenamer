package org.tvrenamer.model;

public class EpisodeTestData {
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_SEPARATOR = ".";
    private static final String DEFAULT_SHOW_ID = "1";
    private static final String DEFAULT_FILENAME_SUFFIX = ".avi";
    private static final String DEFAULT_REPLACEMENT_MASK = "%S [%sx%e] %t";

    private static class Counter {
        private static int fakeEpisodeIdCounter = 101;

        synchronized String getIndex() {
            return String.valueOf(fakeEpisodeIdCounter++);
        }
    }

    private static final Counter COUNTER = new Counter();

    // These attributes concern the filename
    public final String inputFilename;
    public final String filenameShow;
    public final String seasonNumString;
    public final String episodeNumString;
    public final String episodeResolution;
    public final String separator;
    public final String filenameSuffix;

    // These attributes are about looking up the show and episode from the provider
    public final String queryString;
    public final Integer seasonNum;
    public final Integer episodeNum;

    // These are attributes we get back from the provider
    public final String properShowName;
    public final String episodeTitle;
    public final String showId;
    public final String episodeId;

    // These are about producing the renamed file
    public final String replacementMask;
    public final String expectedReplacement;

    // This is just about the test
    public final String documentation;

    public static class Builder {
        String inputFilename;
        String filenameShow;
        String seasonNumString;
        String episodeNumString;
        String episodeResolution;
        String separator;
        String filenameSuffix;

        String queryString;
        Integer seasonNum;
        Integer episodeNum;

        String properShowName;
        String episodeTitle;
        String showId;
        String episodeId;

        String replacementMask;
        String expectedReplacement;

        String documentation;

        public Builder() {
        }

        public Builder inputFilename(String val) {
            if (inputFilename == null) {
                inputFilename = val;
            } else {
                throw new IllegalStateException("cannot re-set inputFilename");
            }
            return this;
        }

        public Builder filenameShow(String val) {
            if (filenameShow == null) {
                filenameShow = val;
            } else {
                throw new IllegalStateException("cannot re-set filenameShow");
            }
            return this;
        }

        public Builder seasonNumString(String val) {
            if (seasonNumString == null) {
                seasonNumString = val;
            } else {
                throw new IllegalStateException("cannot re-set seasonNumString");
            }
            return this;
        }

        public String getSeasonNumString() {
            if (seasonNumString == null) {
                if (seasonNum == null) {
                    return EMPTY_STRING;
                } else {
                    return String.valueOf(seasonNum);
                }
            } else {
                return seasonNumString;
            }
        }

        public Builder episodeNumString(String val) {
            if (episodeNumString == null) {
                episodeNumString = val;
            } else {
                throw new IllegalStateException("cannot re-set episodeNumString");
            }
            return this;
        }

        public String getEpisodeNumString() {
            if (episodeNumString == null) {
                if (episodeNum == null) {
                    return EMPTY_STRING;
                } else {
                    return String.valueOf(episodeNum);
                }
            } else {
                return episodeNumString;
            }
        }

        public Builder episodeResolution(String val) {
            if (episodeResolution == null) {
                episodeResolution = val;
            } else {
                throw new IllegalStateException("cannot re-set episodeResolution");
            }
            return this;
        }

        public String getEpisodeResolution() {
            if (episodeResolution == null) {
                return "";
            } else {
                return episodeResolution;
            }
        }

        public Builder separator(String val) {
            if (separator == null) {
                separator = val;
            } else {
                throw new IllegalStateException("cannot re-set separator");
            }
            return this;
        }

        public String getSeparator() {
            if (separator == null) {
                return DEFAULT_SEPARATOR;
            } else {
                return separator;
            }
        }

        public Builder filenameSuffix(String val) {
            if (filenameSuffix == null) {
                filenameSuffix = val;
            } else {
                throw new IllegalStateException("cannot re-set filenameSuffix");
            }
            return this;
        }

        public String getFilenameSuffix() {
            if (filenameSuffix == null) {
                return ".avi";
            } else {
                return filenameSuffix;
            }
        }

        public Builder queryString(String val) {
            if (queryString == null) {
                queryString = val;
            } else {
                throw new IllegalStateException("cannot re-set queryString");
            }
            return this;
        }

        public Builder seasonNum(Integer val) {
            if (seasonNum == null) {
                seasonNum = val;
            } else {
                throw new IllegalStateException("cannot re-set seasonNum");
            }
            return this;
        }

        public Integer getSeasonNum() {
            if (seasonNum == null) {
                if (seasonNumString != null) {
                    try {
                        return Integer.parseInt(seasonNumString);
                    } catch (NumberFormatException nfe) {
                        return Show.NO_SEASON;
                    }
                } else {
                    return Show.NO_SEASON;
                }
            } else {
                return seasonNum;
            }
        }

        public Builder episodeNum(Integer val) {
            if (episodeNum == null) {
                episodeNum = val;
            } else {
                throw new IllegalStateException("cannot re-set episodeNum");
            }
            return this;
        }

        public Integer getEpisodeNum() {
            if (episodeNum == null) {
                if (episodeNumString != null) {
                    try {
                        return Integer.parseInt(episodeNumString);
                    } catch (NumberFormatException nfe) {
                        return Show.NO_EPISODE;
                    }
                } else {
                    return Show.NO_EPISODE;
                }
            } else {
                return episodeNum;
            }
        }

        public Builder properShowName(String val) {
            if (properShowName == null) {
                properShowName = val;
            } else {
                throw new IllegalStateException("cannot re-set properShowName");
            }
            return this;
        }

        public Builder episodeTitle(String val) {
            if (episodeTitle == null) {
                episodeTitle = val;
            } else {
                throw new IllegalStateException("cannot re-set episodeTitle");
            }
            return this;
        }

        public Builder showId(String val) {
            if (showId == null) {
                showId = val;
            } else {
                throw new IllegalStateException("cannot re-set showId");
            }
            return this;
        }

        public String getShowId() {
            if (showId == null) {
                return "1";
            } else {
                return showId;
            }
        }

        public Builder episodeId(String val) {
            if (episodeId == null) {
                episodeId = val;
            } else {
                throw new IllegalStateException("cannot re-set episodeId");
            }
            return this;
        }

        public String getEpisodeId() {
            if (episodeId == null) {
                synchronized (COUNTER) {
                    return COUNTER.getIndex();
                }
            } else {
                return episodeId;
            }
        }

        public Builder replacementMask(String val) {
            if (replacementMask == null) {
                replacementMask = val;
            } else {
                throw new IllegalStateException("cannot re-set replacementMask");
            }
            return this;
        }

        public String getReplacementMask() {
            if (replacementMask == null) {
                return "%S [%sx%e] %t";
            } else {
                return replacementMask;
            }
        }

        public Builder expectedReplacement(String val) {
            if (expectedReplacement == null) {
                expectedReplacement = val;
            } else {
                throw new IllegalStateException("cannot re-set expectedReplacement");
            }
            return this;
        }

        public Builder documentation(String val) {
            if (documentation == null) {
                documentation = val;
            } else {
                throw new IllegalStateException("cannot re-set documentation");
            }
            return this;
        }

        public EpisodeTestData build() {
            return new EpisodeTestData(this);
        }
    }

    public EpisodeTestData(Builder builder) {
        filenameShow = builder.filenameShow;
        seasonNumString = builder.getSeasonNumString();
        episodeNumString = builder.getEpisodeNumString();
        episodeResolution = builder.getEpisodeResolution();
        separator = builder.getSeparator();
        filenameSuffix = builder.getFilenameSuffix();

        queryString = builder.queryString;
        seasonNum = builder.getSeasonNum();
        episodeNum = builder.getEpisodeNum();

        properShowName = builder.properShowName;
        episodeTitle = builder.episodeTitle;
        showId = builder.getShowId();
        episodeId = builder.getEpisodeId();

        replacementMask = builder.getReplacementMask();
        expectedReplacement = builder.expectedReplacement;

        documentation = builder.documentation;

        if (builder.inputFilename == null) {
            String resolutionString = EMPTY_STRING;
            if (episodeResolution.length() > 0) {
                resolutionString = separator + episodeResolution;
            }
            inputFilename = filenameShow
                + separator + seasonNumString
                + separator + episodeNumString
                + resolutionString;
        } else {
            inputFilename = builder.inputFilename;
        }
    }

    @Override
    public String toString() {
        return "EpisodeTestData[" + inputFilename + "]";
    }
}
