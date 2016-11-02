package org.tvrenamer.controller;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FileEpisode;

public class TVRenamer {
    private static Logger logger = Logger.getLogger(TVRenamer.class.getName());

    public static final String[] REGEX = {
            "(.+?\\d{4}\\W\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*\\D(\\d+[pk]).*", // this one works for titles with years
            "(.+?\\W\\D*?)[sS](\\d\\d?)[eE](\\d\\d?).*\\D(\\d+[pk]).*", // this one matches SXXEXX
            "(.+\\W\\D*?)[sS](\\d\\d?)\\D*?[eE](\\d\\d).*\\D(\\d+[pk]).*", // this one matches sXX.eXX
            "(.+\\W\\D*?)(\\d\\d?)\\D+(\\d\\d).*\\D(\\d+[pk]).*", // this one matches everything else
            "(.+\\W+)(\\d\\d?)(\\d\\d).*\\D(\\d+[pk]).*" // truly last resort
    };

    public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length * 2];

    static {
        for (int i = 0; i < REGEX.length * 2; i++) {
            if (i / REGEX.length == 0){
                COMPILED_REGEX[i] = Pattern.compile(REGEX[i]);
            } else {
                COMPILED_REGEX[i] = Pattern.compile(REGEX[i - REGEX.length].replace(".*\\D(\\d+[pk])", ""));
            }
        }
    }

    private TVRenamer() {
        // singleton
    }

    public static FileEpisode parseFilename(String fileName) {
        File f = new File(fileName);
        String fName = stripJunk(insertShowNameIfNeeded(f));
        int idx = 0;
        Matcher matcher = null;
        while (idx < COMPILED_REGEX.length) {
            matcher = COMPILED_REGEX[idx++].matcher(fName);
            if (matcher.matches() && matcher.groupCount() == 4) {
                String show = matcher.group(1);
                show = StringUtils.replacePunctuation(show).toLowerCase();

                int season = Integer.parseInt(matcher.group(2));
                int episode = Integer.parseInt(matcher.group(3));
                String resolution = matcher.group(4);

                FileEpisode ep = new FileEpisode(show, season, episode, resolution, f);
                return ep;
            } else if (matcher.matches() && matcher.groupCount() == 3){
                String show = matcher.group(1);
                show = StringUtils.replacePunctuation(show).toLowerCase();

                int season = Integer.parseInt(matcher.group(2));
                int episode = Integer.parseInt(matcher.group(3));
                String resolution = "";

                FileEpisode ep = new FileEpisode(show, season, episode, resolution, f);
                return ep;
            }
        }

        return null;
    }

    private static String stripJunk(String input) {
        String output = input;
        output = removeLast(output, "hdtv");
        output = removeLast(output, "dvdrip");
        return output;

    }

    private static String removeLast(String input, String match) {
        int idx = input.toLowerCase().lastIndexOf(match);
        if (idx > 0) {
            input = input.substring(0, idx);
        }
        return input;
    }

    private static String insertShowNameIfNeeded(File file) {
        String fName = file.getName();
        if (fName.matches("[sS]\\d\\d?[eE]\\d\\d?.*")) {
            String parentName = file.getParentFile().getName();
            if (parentName.toLowerCase().startsWith("season")) {
                parentName = file.getParentFile().getParentFile().getName();
            }
            logger.info("appending parent directory '" + parentName + "' to filename '" + fName + "'");
            return parentName + " " + fName;
        } else {
            return fName;
        }
    }
}
