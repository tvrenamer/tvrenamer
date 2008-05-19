package com.google.code.tvrenamer.model;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class Show {

  private static Logger logger = Logger.getLogger(Show.class);

  private final String id;
  private final String name;
  private final String url;

  private HashMap<String, Season> seasons;

  public Show(String id, String name, String url) {
    this.id = id;
    this.name = name;
    this.url = url;

    this.seasons = new HashMap<String, Season>();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public void setSeason(String sNum, Season season) {
    this.seasons.put(sNum, season);
  }

  public Season getSeason(String sNum) {
    return this.seasons.get(sNum);
  }
}
