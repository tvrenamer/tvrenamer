package com.google.code.tvrenamer.controller;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.code.tvrenamer.model.FileEpisode;

public class TVRenamer {
  public static final String[] REGEX = {
      "(.+?\\d{4}\\W\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*",
      "(.+?\\W\\D*?)[sS]?(\\d\\d?)\\D*?(\\d\\d).*" };
  public static final Pattern[] COMPILED_REGEX = new Pattern[REGEX.length];
  static {
    for (int i = 0; i < REGEX.length; i++) {
      COMPILED_REGEX[i] = Pattern.compile(REGEX[i]);
    }
  }

  private TVRenamer() {
    // singleton
  }

  public static FileEpisode parseFilename(String fileName) {
    File f = new File(fileName);
    String fName = f.getName();
    int idx = 0;
    Matcher matcher = null;
    while (idx < COMPILED_REGEX.length) {
      matcher = COMPILED_REGEX[idx++].matcher(fName);
      if (matcher.matches()) {
        String show = matcher.group(1);
        show = replacePunctuation(show).trim().toLowerCase();

        int season = Integer.parseInt(matcher.group(2));
        int episode = Integer.parseInt(matcher.group(3));

        FileEpisode ep = new FileEpisode(show, season, episode, f);
        return ep;
      }
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
