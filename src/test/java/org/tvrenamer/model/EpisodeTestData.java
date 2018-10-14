package org.tvrenamer.model;

import org.tvrenamer.controller.TestUtils;

import java.nio.file.Path;

public class EpisodeTestData {
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_SEPARATOR = ".";
    @SuppressWarnings("unused")
    private static final String DEFAULT_SHOW_ID = "1";
    @SuppressWarnings("unused")
    private static final String DEFAULT_FILENAME_SUFFIX = ".avi";
    @SuppressWarnings("unused")
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
    public final String filenameSuffix;

    // These attributes are about looking up the show and episode from the provider
    public final String queryString;
    public final Integer seasonNum;
    public final Integer episodeNum;
    public final Boolean preferDvd;

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
        String separator;

        String inputFilename;
        String filenameShow;
        String seasonNumString;
        String episodeNumString;
        String episodeResolution;
        String filenameSuffix;

        String queryString;
        Integer seasonNum;
        Integer episodeNum;
        Boolean preferDvd;

        String properShowName;
        String episodeTitle;
        String showId;
        String episodeId;

        String replacementMask;
        String expectedReplacement;

        String documentation;

        public Builder() {
        }

        @SuppressWarnings("unused")
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

        public Builder preferDvd(boolean val) {
            if ((preferDvd == null) || (preferDvd == val)) {
                preferDvd = val;
            } else {
                throw new IllegalStateException("cannot re-set preferDvd");
            }
            return this;
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

    private EpisodeTestData(Builder builder) {
        final String separator = builder.getSeparator();

        filenameShow = builder.filenameShow;
        seasonNumString = builder.getSeasonNumString();
        episodeNumString = builder.getEpisodeNumString();
        episodeResolution = builder.getEpisodeResolution();
        filenameSuffix = builder.getFilenameSuffix();

        queryString = builder.queryString;
        seasonNum = builder.getSeasonNum();
        episodeNum = builder.getEpisodeNum();
        preferDvd = builder.preferDvd;

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
                + resolutionString
                + separator + filenameSuffix;
        } else {
            inputFilename = builder.inputFilename;
        }
    }

    /**
     * Create a FileEpisode based on the data in this object.  Note this doesn't
     * use a lot of what the real program does; it doesn't fetch anything from the
     * Internet (or even from a cache), and it doesn't use listeners.  And, of
     * course, it doesn't use the UI, which is intimately tied to moving files in
     * the real program.  But it does try to simulate the process.
     *
     * This method takes a directory Path as an argument.  It will try to create
     * a file that represents this object, in the directory, if a directory is
     * given.  If the caller doesn't want an actual file created, just pass null
     * as the directory.
     *
     * We create a FileEpisode from the "inputFilename" (which, unfortunately, is a
     * bit misnamed, in that it may well be a relative path string, and is not just
     * limited to being the filename part).  A FileEpisode initially is basically
     * just a shell.  We then fill in information about the filename.  (In the real
     * program, the parser in FilenameParser is used for this.)
     *
     * Then, once we know the part of the filename that we think represents the
     * show name, we find the actual show name, and create a Show object to
     * represent it.  We store the mapping between the query string and the Show
     * object in ShowStore.  (In the real program, we send the query string to the
     * TVDB, it responds with options in XML, which we parse, choose the best
     * match, and use to create the Show object.)
     *
     * Once we have the Show object, we add the episodes.  Here, we're creating a
     * single episode, but the Show API always expects an array.  (In the real
     * program, we get the episodes by querying The TVDB with the show ID, and
     * parsing the XML into an array of EpisodeInfo objects.)  We create a
     * one-element array and stick the EpisodeInfo into it, and add that to the
     * Show.
     *
     * Finally, we set the status of the FileEpisode to tell it we're finished
     * downloading all the episodes its show needs to know about, which enables
     * getReplacementText to give us the filename to use.  (If it didn't think we
     * were finished adding episodes, it would instead return a placeholder text.)
     *
     * NOTE: this does no real error checking.  It is trivial to create an instance
     * of EpisodeTestData that does not contain real values for some of the data
     * required here, in which case, calling this method will be unpredictable.
     * It is assumed the caller will have created the EpisodeTestData with enough
     * information for this to work.
     *
     * @param directory
     *   if the caller wants an actual file created, this specifies the directory
     *   to create it in; can be null
     * @return a FileEpisode that corresponds to the information in this object
     *
     */
    public FileEpisode createFileEpisode(final Path directory) {
        String relativeFilepath = this.inputFilename;

        if (directory != null) {
            boolean success = TestUtils.createFile(directory, relativeFilepath);
            if (!success) {
                return null;
            }
            relativeFilepath = directory.resolve(relativeFilepath).toString();
        }

        // Note, passing a String, not the actual Path.  The normal FileEpisode
        // constructor takes a Path, and then immediately invokes the
        // FilenameParser.  Although our filename should be parsable, we don't
        // want to use the FilenameParser; we want to specify to the FileEpisode
        // exactly what the components of the filename are.  The String constructor
        // is used for this.
        FileEpisode episode = new FileEpisode(relativeFilepath);

        // This part would normally be done by the FilenameParser
        episode.setFilenameShow(this.filenameShow);
        episode.setEpisodePlacement(this.seasonNumString, this.episodeNumString);
        episode.setFilenameResolution(this.episodeResolution);

        // This would normally be done by getting the options from the provider.
        Show show = ShowStore.getOrAddShow(this.filenameShow, this.properShowName);
        episode.setEpisodeShow(show);

        // This would be done by getting the listings from the provider.
        EpisodeInfo info = new EpisodeInfo.Builder()
            .episodeId(this.episodeId)
            .seasonNumber(this.seasonNumString)
            .episodeNumber(this.episodeNumString)
            .episodeName(this.episodeTitle)
            .build();
        show.addOneEpisode(info);
        show.indexEpisodesBySeason();
        episode.listingsComplete();

        return episode;
    }

    @Override
    public String toString() {
        return "EpisodeTestData[" + inputFilename + "]";
    }
}
