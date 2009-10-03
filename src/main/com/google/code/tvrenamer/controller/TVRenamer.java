package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;

public class TVRenamer {
  private static Logger logger = Logger.getLogger(TVRenamer.class);
  private Show show;

  public TVRenamer() {
  }

  public void setShow(Show show) {
    this.show = show;
  }

  public ArrayList<Show> downloadOptions(String showName) {
    return TVRageProvider.getShowOptions(showName);
  }

  public void downloadListing() {
    TVRageProvider.getShowListing(show);
  }

  public String parseFileName(String fileName, String showName, String format) {
    /*
     * this is horrible, we need to have a new method to get the title before we
     * try and rename
     *
     * need to handle multi-word show titles by:
     *
     * 1. checking the title against the directory name
     *
     * 2. if it matches, then remove the directory name from the string and
     * carry on
     *
     * 3. else, grab first word of string, presume it's the title and carry on
     */

    String showTitle = replacePunctuation(showName.toLowerCase());
    String cleanedFileName = replacePunctuation(fileName.toLowerCase());

    if (!cleanedFileName.startsWith(showTitle)) {
      logger.error("Show's name (" + showName + ") does not match file name: "
          + fileName);
      return fileName;
    }

    // grabs the show's name out of the filename, for shows with numeric titles
    // String seasonNum = fileName.toLowerCase();
    String seasonNum = cleanedFileName.replaceFirst(showTitle, "");

    String episodeNum = "";

    // if we keep the episode matching stuff, it will be non-greedy ^_^
    String regex = "[^\\d]*?(\\d\\d?)[^\\d]*?(\\d\\d).*";
    String keane = "[\\D0]*(\\d+?)\\D*0*(\\d+)\\D{2}.*$";
    Matcher matcher = Pattern.compile(regex).matcher(seasonNum);
    if (matcher.matches()) {
      seasonNum = matcher.group(1);
      // remove leading zero
      if (seasonNum.charAt(0) == '0') {
        seasonNum = seasonNum.substring(1);
      }
      episodeNum = matcher.group(2);
    }

    Season s = show.getSeason(seasonNum);
    if (s == null) {
      logger.error("season not found: " + seasonNum);
      return fileName;
    }
    String title = s.getTitle(episodeNum);
    if (title == null) {
      logger.error("Title not found for episode: " + episodeNum);
      return fileName;
    }
    title = sanitiseTitle(title);
    String extension = getExtension(fileName);

    format = format.replace("%S", showName);
    format = format.replace("%s", seasonNum);
    format = format.replace("%e", episodeNum);
    format = format.replace("%t", title);

    return format + "." + extension;
  }

  public String getShowName(File file) {
    String showName = file.getParentFile().getName();

    // If the showname is 'Season x' the go up another directory
    if (showName.toLowerCase().startsWith("season")) {
      showName = file.getParentFile().getParentFile().getName();
    }
    return showName;
  }

  private String getExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    return filename.substring(dot + 1);
  }

  private String sanitiseTitle(String title) {
    // need to add more mappings, such as ':'
    title = title.replace(":", " -");
    title = title.replace('/', '-');
    title = title.replace('\\', '-');
    title = title.replace("?", "");
    title = title.replace("`", "'");
    return title;
  }

  private String replacePunctuation(String s) {
    s = s.replaceAll("\\.", " ");
    s = s.replaceAll(",", " ");
    return s;
  }
}
