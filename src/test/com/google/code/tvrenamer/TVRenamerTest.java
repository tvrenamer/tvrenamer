package com.google.code.tvrenamer;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.view.UIStarter;

public class TVRenamerTest {

  @Test
  public void testParseFileName() {
    // new instance of tvrenamer object
    TVRenamer tvr = new TVRenamer();

    // dummy show, id is 1 and name is Test
    String showname = "Warehouse 13";
    Show show = new Show("7884", showname, "file:///");

    // dummy season, season 1, episode 1 called First Episode
    Season season = new Season("1");
    season.setEpisode("01", "Pilot");

    // setup rest of tvrenamer
    show.setSeason("1", season);
    tvr.setShow(show);

    String filename = "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv";
    String expected = "Warehouse 13 [1x01] Pilot.mkv";

    String retval = tvr.parseFileName(filename, showname,
        UIStarter.DEFAULT_FORMAT_STRING);

    System.out.println("received: " + retval);
    assertTrue(retval.equals(expected));
  }

}
