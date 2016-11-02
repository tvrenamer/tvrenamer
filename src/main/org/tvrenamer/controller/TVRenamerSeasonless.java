package org.tvrenamer.controller;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.FileEpisode;

import java.io.File;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class to handle parsing TV show files which are in season-less format.
 * Season-less format means that the name of the episode contains only episode number and no season number.
 * This can be either regular long running shows, daily shows or anime.
 *
 * Created by lsrom on 10/19/16.
 * @author lukas.srom@gmail.com
 */
public abstract class TVRenamerSeasonless {
    private static Logger logger = Logger.getLogger(TVRenamerSeasonless.class.getName());

    public static final String[] REGEX = {
            "(.+)\\W(\\d\\d?\\d?).*\\D(\\d+[pk]).*",    // matches all titles with title and episode (1 to 3 nums)
            "(.+)\\W(\\d\\d\\d).*",                     // matches everything, correct matches only for filenames without resolution
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

    /**
     * Parses given filename. Filename is first reformatted and striped of junk characters
     * (multiple spaces, underscores, etc.) and then parsed by regular expressions.
     *
     * @param filename String with filename to be parsed.
     * @return FileEpisode object.
     */
    public static FileEpisode parseFilename (String filename){
        TheTVDBProvider.setPreferDVDEpNum(false);

        File f = new File(filename);
        String fName = reformatInput(removeFileSystemPath(filename));
        int idx = 0;
        Matcher matcher = null;

        while (idx < COMPILED_REGEX.length){
            matcher = COMPILED_REGEX[idx++].matcher(fName);

            // g1: title, g2: episode num, g3: res
            if (matcher.matches() && matcher.groupCount() == 3){
                String show = matcher.group(1);
                show = StringUtils.replacePunctuation(show).trim().toLowerCase();

                int episode = Integer.parseInt(matcher.group(2));
                String resolution = matcher.group(3);

                FileEpisode ep = new FileEpisode(show, 0, episode, resolution, f);
                return ep;
            } else if (matcher.matches() && matcher.groupCount() == 2) {    // g1: title, g2: episode num
                String show = matcher.group(1);
                show = StringUtils.replacePunctuation(show).trim().toLowerCase();

                int episode = Integer.parseInt(matcher.group(2));

                FileEpisode ep = new FileEpisode(show, 0, episode, "", f);
                return ep;
            }
        }

        return null;
    }

    /**
     * Takes a string and returns it's value stripped of junk characters and reformatted so the parsing is easier.
     * Operations used in this method should not remove any vital information from the string.
     *
     * @param input String to be reformatted and striped of junk characters.
     * @return Cleaned string.
     */
    private static String reformatInput (final String input){
        String output = input;

        // remove any substring at the beginning enclosed in [] as it would mess up regex matching
        if (output.startsWith("[")){
            int x = output.indexOf("]");
            output = output.substring(x + 1, output.length()).trim();
        }

        // remove any substring at the end enclosed in [] as it would mess up regex matching
        // but only if it doesn't contain resolution string
        if (output.matches("^.*([\\]]\\.[0-9a-z]{3,4})$") && !output.matches("^.*(\\d+[pk])[\\]]\\.[a-z]{3,4}$")){
            int x = output.lastIndexOf("[");
            String ext = output.substring(output.lastIndexOf("."), output.length());
            output = output.substring(0, x) + ext;
        }

        // if there is more than one occurrence of '-' remove it all, allow one as it might be part of the title
        if (hasMultipleOccurrences(output, '-')){
            output = output.replaceAll("-", " ");
        }

        output = output.replaceAll("_", " ");           // remove all '_' from title
        output = output.replaceAll("[ ]{2,}", " ");     // replace any subsequent spaces with single space

        output = output.trim();

        return output;
    }

    /**
     * Checks if given character is present in the string more than once.
     * Note that it doesn't count the number of occurrences, only checks if there is more then one.
     *
     * @param str String to be searched for occurrences of given character.
     * @param c Character to be count in the string.
     * @return Boolean TRUE if character is in the string multiple times, otherwise FALSE.
     */
    private static boolean hasMultipleOccurrences (String str, char c){
        if (str.indexOf(c) != str.lastIndexOf(c)){
            return true;
        }

        return false;
    }

    private static String removeFileSystemPath (String filename){
        int i = filename.lastIndexOf('/');
        return filename.substring(i + 1, filename.length());
    }
}
