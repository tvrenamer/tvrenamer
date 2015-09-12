package com.google.code.tvrenamer;

import com.google.code.tvrenamer.controller.TheTVDBProvider;
import com.google.code.tvrenamer.model.Show;
import org.junit.Test;

public class TheTVDBProviderTest {

    @Test
    public void testGetShowOptions() throws Exception {
        for (Show show : TheTVDBProvider.getShowOptions("Gossip Girl")) {
            System.out.println(show.getId() + " -> " + show.getName());
        }
    }

    @Test
    public void testGetShowListing() throws Exception {
        TheTVDBProvider.getShowListing(new Show("80547", "Gossip Girl", ""));
    }
}
