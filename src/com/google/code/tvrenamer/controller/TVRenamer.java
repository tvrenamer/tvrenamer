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

  public String parseFileName(File file, String showName, String format) {
    // grabs the show's name out of the filename, for shows with numeric titles
    String seasonNum = file.getName();
    seasonNum = seasonNum.replaceFirst(showName, "");
    String episodeNum = "";

    // if we keep the episode matching stuff, it will be non-greedy ^_^
    String regex = "[^\\d]*?(\\d\\d?)[^\\d]*?(\\d\\d).*";
    Matcher matcher = Pattern.compile(regex).matcher(seasonNum);
    if (matcher.matches()) {
      seasonNum = matcher.group(1);
      // remove leading zero
      if (seasonNum.charAt(0) == '0')
        seasonNum = seasonNum.substring(1);
      episodeNum = matcher.group(2);
    }

    Season s = this.show.getSeason(seasonNum);
    if (s == null) {
      logger.error("season not found!");
      return file.getName();
    }
    String title = s.getTitle(episodeNum);
    if (title == null) {
      logger.error("title not found!");
      return file.getName();
    }
    title = sanitiseTitle(title);
    String extension = getExtension(file.getName());

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
    return title;
  }
}
