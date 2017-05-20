package org.tvrenamer.controller;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.ShowName;

import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVRenamer {
    private static final Logger logger = Logger.getLogger(TVRenamer.class.getName());

    private static final String RESOLUTION_REGEX = "\\D(\\d+[pk]).*";

    private static final String[] REGEX = {
        // this one works for titles with years:
        "(.+?\\d{4}[^a-zA-Z0-9]\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*",

        // this one matches SXXEXX:
        "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)[eE](\\d\\d*).*",

        // this one matches sXX.eXX:
        "(.+[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)\\D*?[eE](\\d\\d).*",

        // this one matches everything else:
        "(.+[^a-zA-Z0-9]\\D*?)(\\d\\d?)\\D+(\\d\\d).*",

        // truly last resort:
        "(.+[^a-zA-Z0-9]+)(\\d\\d?)(\\d\\d).*"
    };

    // REGEX is a series of regular expressions for different patterns comprising
    // show name, season number, and epsiode number.  We also want to be able to
    // recognize episode resolution ("720p", etc.)  To make the resolution optional,
    // we compile the patterns with the resolution first, and then compile the
    // basic patterns.  So we need an array twice the size of REGEX to hold the
    // two options for each.
    private static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length * 2];

    static {
        // Recognize the "with resolution" pattern first, since the basic patterns
        // would always permit the resolution and just see it as "junk".

        for (int i = 0; i < REGEX.length; i++) {
            // Add the resolution regex to the end of the basic pattern
            COMPILED_REGEX[i] = Pattern.compile(REGEX[i] + RESOLUTION_REGEX);
        }
        for (int i = 0; i < REGEX.length; i++) {
            // Now add the basic patterns to the end of the array.
            // That is, pattern 0 becomes compiled pattern 6, etc.
            COMPILED_REGEX[i + REGEX.length] = Pattern.compile(REGEX[i]);
        }
    }

    private TVRenamer() {
        // singleton
    }

    public static void parseFilename(final FileEpisode episode) {
        Path filePath = episode.getPath();
        String withShowName = insertShowNameIfNeeded(filePath);
        String strippedName = stripJunk(withShowName);
        int idx = 0;
        Matcher matcher;
        while (idx < COMPILED_REGEX.length) {
            matcher = COMPILED_REGEX[idx++].matcher(strippedName);
            if (matcher.matches()) {
                String resolution = "";
                if (matcher.groupCount() == 4) {
                    resolution = matcher.group(4);
                } else if (matcher.groupCount() != 3) {
                    // This should never happen and so we should probably consider it
                    // an error if it does, but not important.
                    continue;
                }
                String foundName = matcher.group(1);
                ShowName.lookupShowName(foundName);
                episode.setFilenameShow(foundName);
                episode.setFilenameSeason(matcher.group(2));
                episode.setFilenameEpisode(matcher.group(3));
                episode.setFilenameResolution(resolution);
                episode.setParsed();

                return;
            }
        }

        episode.setFailToParse();
    }

    private static String stripJunk(String input) {
        String output = input;
        output = StringUtils.removeLast(output, "hdtv");
        output = StringUtils.removeLast(output, "dvdrip");
        return output;
    }

    private static String insertShowNameIfNeeded(final Path filePath) {
        String pName = filePath.getFileName().toString();
        logger.fine("pName = " + pName);
        if (pName.matches("[sS]\\d\\d?[eE]\\d\\d?.*")) {
            Path parent = filePath.getParent();
            String parentName = parent.getFileName().toString();
            if (StringUtils.toLower(parentName).startsWith("season")) {
                parentName = parent.getParent().getFileName().toString();
            }
            logger.fine("appending parent directory '" + parentName + "' to filename '" + pName + "'");
            return parentName + " " + pName;
        } else {
            return pName;
        }
    }
}