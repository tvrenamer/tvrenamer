package com.google.code.tvrenamer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.code.tvrenamer.controller.TVRageProvider;

public class ShowStore {

  private static Map<String, Show> _shows = new HashMap<String, Show>();

  public static void addShow(String showName) {
    ArrayList<Show> options = TVRageProvider.getShowOptions(showName);
    Show thisShow = options.get(0);

    TVRageProvider.getShowListing(thisShow);
    synchronized (_shows) {
      System.out.println("put show " + showName + " as " + thisShow.getName());
      _shows.put(showName, thisShow);
    }
  }

  public static Show getShow(String showName) {
    synchronized (_shows) {
      return _shows.get(showName);
    }
  }
}
