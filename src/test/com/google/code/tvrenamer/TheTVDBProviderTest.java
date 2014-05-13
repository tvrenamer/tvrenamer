package com.google.code.tvrenamer;

import org.junit.Test;

import com.google.code.tvrenamer.controller.TheTVDBProvider;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.TVRenamerIOException;

public class TheTVDBProviderTest {

	@Test
	public void testGetShowOptions() throws Exception {
		for (Show show : TheTVDBProvider.getShowOptions("House of Cards")) {
			System.out.println(show.getId() + " -> " + show.getName());
		}
	}

	@Test
	public void testGetShowListing() throws TVRenamerIOException {
		Show hoc = new Show("262980", "House of Cards", TheTVDBProvider.IMDB_BASE_URL + "tt2161930");
		TheTVDBProvider.getShowListing(hoc);
		System.out.println(hoc);
	}
}
