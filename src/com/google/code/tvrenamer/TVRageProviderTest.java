package com.google.code.tvrenamer;

import org.junit.Test;

public class TVRageProviderTest {

  @Test
  public void testGetShowOptions() {
    for (Show show : TVRageProvider.getShowOptions("Battlestar Galactica")) {
      System.out.println(show.getId() + " -> " + show.getName());
    }
  }

  @Test
  public void testGetShowListing() {
    TVRageProvider.getShowListing(new Show("2730", "Battlestar Galactica",
        "http://www.tvrage.com/Battlestar_Galactica"));
  }
}
