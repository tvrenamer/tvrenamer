package org.tvrenamer.controller;

import org.junit.Test;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TheTVDBSwaggerProviderSimpleTest {

    @Test
    public void testGetShowOptions() throws Exception {
        ShowName showName = ShowName.lookupShowName("Heroes");
        TheTVDBSwaggerProvider.getShowOptions(showName);
        assertTrue(showName.hasShowOptions());

        System.out.println(showName);
    }

    @Test
    public void testGetShowListing() throws Exception {
        Show show = Show.getShowInstance("72218", "Smallville", null);
        TheTVDBSwaggerProvider.getShowListing(show);
        assertTrue(show.hasSeasons());
        assertTrue(show.hasEpisodes());

        assertNotNull(show.getEpisode(9, 15));

    }

    @Test
    public void testReadShowsFromSearchResponse() throws Exception {
        Path inputFile = Paths.get("C:\\Users\\vipul\\Documents\\GitHub\\tvrenamer\\heroes-seriessearch.json");
        String input = new String(Files.readAllBytes(inputFile), "UTF8");
        TheTVDBSwaggerProvider.readSeriesFromSearchResponse(input, ShowName.lookupShowName("Heroes"));
    }

    @Test
    public void testReadEpisodesFromSearchResponse() throws Exception {
        Path inputFile = Paths.get("C:\\Users\\vipul\\Documents\\GitHub\\tvrenamer\\heroes-seriesepisodes.json");
        String input = new String(Files.readAllBytes(inputFile), "UTF8");
        Show show = Show.getShowInstance("79501", "Heroes", null);
        TheTVDBSwaggerProvider.readEpisodesFromSearchResponse(input, show);
    }

}
