package org.tvrenamer;

import org.tvrenamer.controller.TheTVDBProvider;
import org.tvrenamer.model.Show;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class TheTVDBProviderTest {

    @Test
    public void testGetShowOptions() throws Exception {
        for (Show show : TheTVDBProvider.getShowOptions("Gossip Girl")) {
            assertNotNull(show);
            assertNotEquals(0, show.getId().length());
            assertNotEquals(0, show.getName().length());
        }
    }

    @Test
    public void testGetShowListing() throws Exception {
        TheTVDBProvider.getShowListing(new Show("80547", "Gossip Girl", ""));
    }
}
