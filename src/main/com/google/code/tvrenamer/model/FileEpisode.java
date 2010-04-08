package com.google.code.tvrenamer.model;

import java.io.File;

public class FileEpisode {
  private final String showName;
  private final int seasonNumber;
  private final int episodeNumber;
  private File file;

  public FileEpisode(String name, int season, int episode, File f) {
    showName = name;
    seasonNumber = season;
    episodeNumber = episode;
    file = f;
  }

  public String getShowName() {
    return showName;
  }

  public int getSeasonNumber() {
    return seasonNumber;
  }

  public int getEpisodeNumber() {
    return episodeNumber;
  }

  public File getFile() {
    return file;
  }

  public void setFile(File f) {
    file = f;
  }

}
