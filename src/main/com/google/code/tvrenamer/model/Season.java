package com.google.code.tvrenamer.model;

import java.util.HashMap;

public class Season {
  private final String num;
  private final HashMap<String, String> episodes;

  public Season(String num) {
    this.num = num;
    episodes = new HashMap<String, String>();
  }

  public String getNumber() {
    return num;
  }

  public String getTitle(String epNum) {
    return episodes.get(epNum);
  }

  public void setEpisode(String epNum, String title) {
    episodes.put(epNum, title);
  }
}
