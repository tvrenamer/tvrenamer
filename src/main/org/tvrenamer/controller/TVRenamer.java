package org.tvrenamer.controller;

import org.tvrenamer.model.FileEpisode;

import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TVRenamer {
    private static Logger logger = Logger.getLogger(TVRenamer.class.getName());

    public static final String[] REGEX = {
            "(.+?\\d{4}[^a-zA-Z0-9]\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*\\D(\\d+[pk]).*", // this one works for titles with years
            "(.+?[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)[eE](\\d\\d?).*\\D(\\d+[pk]).*", // this one matches SXXEXX
            "(.+[^a-zA-Z0-9]\\D*?)[sS](\\d\\d?)\\D*?[eE](\\d\\d).*\\D(\\d+[pk]).*", // this one matches sXX.eXX
            "(.+[^a-zA-Z0-9]\\D*?)(\\d\\d?)\\D+(\\d\\d).*\\D(\\d+[pk]).*", // this one matches everything else
            "(.+[^a-zA-Z0-9]+)(\\d\\d?)(\\d\\d).*\\D(\\d+[pk]).*" // truly last resort
    };

    public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length * 2];

    static {
        for (int i = 0; i < REGEX.length * 2; i++) {
            if (i / REGEX.length == 0) {
                COMPILED_REGEX[i] = Pattern.compile(REGEX[i]);
            } else {
                COMPILED_REGEX[i] = Pattern.compile(REGEX[i - REGEX.length].replace(".*\\D(\\d+[pk])", ""));
            }
        }
    }

    private TVRenamer() {
        // singleton
    }

    public static boolean parseFilename(final FileEpisode episode) {
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
                episode.setFilenameShow(matcher.group(1));
                episode.setSeasonNum(Integer.parseInt(matcher.group(2)));
                episode.setEpisodeNum(Integer.parseInt(matcher.group(3)));
                episode.setFilenameResolution(resolution);
                episode.setParsed();

                return true;
            }
        }

        episode.setFailToParse();
        return false;
    }

    private static String stripJunk(String input) {
        String output = input;
        output = removeLast(output, "hdtv");
        output = removeLast(output, "dvdrip");
        return output;

    }

    public static String removeLast(String input, String match) {
        int idx = input.toLowerCase().lastIndexOf(match);
        if (idx > 0) {
            input = input.substring(0, idx)
                + input.substring(idx + match.length(), input.length());
        }
        return input;
    }

    static String insertShowNameIfNeeded(final Path filePath) {
        String pName = filePath.getFileName().toString();
        logger.fine("pName = " + pName);
        if (pName.matches("[sS]\\d\\d?[eE]\\d\\d?.*")) {
            Path parent = filePath.getParent();
            String parentName = parent.getFileName().toString();
            if (parentName.toLowerCase().startsWith("season")) {
                parentName = parent.getParent().getFileName().toString();
            }
            logger.fine("appending parent directory '" + parentName + "' to filename '" + pName + "'");
            return parentName + " " + pName;
        } else {
            return pName;
        }
    }
}
