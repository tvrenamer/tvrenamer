package com.google.code.tvrenamer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.code.tvrenamer.controller.TVRenamer;
import com.google.code.tvrenamer.model.FileEpisode;
import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;

public class TVRenamerTest {

  @Test
  public void testParseFileName() {

    // dummy show, id is 1 and name is Test
    String showname = "Warehouse 13";
    Show show = new Show("7884", showname, "file:///");

    // dummy season, season 1, episode 1 called First Episode
    Season season = new Season(1);
    season.setEpisode(1, "Pilot");

    // setup rest of tvrenamer
    show.setSeason(1, season);

    String filename = "warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv";

    FileEpisode retval = TVRenamer.parseFilename(filename);

    assertEquals(showname.toLowerCase(), retval.getShowName());
    assertEquals(1, retval.getSeasonNumber());
    assertEquals(1, retval.getEpisodeNumber());
  }

}
