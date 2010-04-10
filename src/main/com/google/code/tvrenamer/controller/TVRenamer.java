package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.code.tvrenamer.model.FileEpisode;

public class TVRenamer {
  public static final String REGEX = "(.+?\\W\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*";
  public static final Pattern COMPILED_REGEX = Pattern.compile(REGEX);

  private TVRenamer() {
    // singleton
  }

  public static FileEpisode parseFilename(String fileName) {
    File f = new File(fileName);
    Matcher matcher = COMPILED_REGEX.matcher(f.getName());
    if (matcher.matches()) {
      String show = matcher.group(1);
      show = replacePunctuation(show).trim().toLowerCase();

      int season = Integer.parseInt(matcher.group(2));
      int episode = Integer.parseInt(matcher.group(3));

      FileEpisode ep = new FileEpisode(show, season, episode, f);
      return ep;
    }

    return null;
  }

  public static String getExtension(String filename) {
    int dot = filename.lastIndexOf('.');
    return filename.substring(dot + 1);
  }

  public static String sanitiseTitle(String title) {
    // need to add more mappings, such as ':'
    title = title.replace(":", " -");
    title = title.replace('/', '-');
    title = title.replace('\\', '-');
    title = title.replace("?", "");
    title = title.replace("`", "'");
    return title;
  }

  public static String replacePunctuation(String s) {
    s = s.replaceAll("\\.", " ");
    s = s.replaceAll(",", " ");
    return s;
  }
}
