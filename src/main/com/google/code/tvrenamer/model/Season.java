package com.google.code.tvrenamer.model;

import java.util.HashMap;
import java.util.Map;

public class Season {
  private final int num;
  private final Map<Integer, String> episodes;

  public Season(int num) {
    this.num = num;
    episodes = new HashMap<Integer, String>();
  }

  public int getNumber() {
    return num;
  }

  public void setEpisode(int epNum, String title) {
    episodes.put(epNum, title);
  }

  public String getTitle(int epNum) {
    return episodes.get(epNum);
  }

}
