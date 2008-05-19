package com.google.code.tvrenamer.model;

import java.util.HashMap;

public class Season {
  private final String num;
  private HashMap<String, String> episodes;

  public Season(String num) {
    this.num = num;
    this.episodes = new HashMap<String, String>();
  }

  public String getNumber() {
    return this.num;
  }

  public String getTitle(String epNum) {
    return this.episodes.get(epNum);
  }

  public void setEpisode(String epNum, String title) {
    this.episodes.put(epNum, title);
  }
}
