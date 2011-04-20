package com.google.code.tvrenamer.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.tvrenamer.controller.ShowInformationListener;


public class FileEpisodeTest {
	private static Logger logger = Logger.getLogger(FileEpisodeTest.class.getName());
	
	private List<File> testFiles;
	
	private UserPreferences mockUserPrefs;
	private ShowInformationListener mockListener;

	@Before
	public void setUp() throws Exception {
		testFiles = new ArrayList<File>();
		mockUserPrefs = mock(UserPreferences.class);
		mockListener = mock(ShowInformationListener.class);

		when(mockUserPrefs.getRenameReplacementString()).thenReturn("%S [%sx%e] %t");
	}

	/**
	 * Test case for Issue 37 where the title "$pringfield"
	 * breaks the regex used for String.replaceAll()
	 */
	@Test
	public void testGetNewFilenameSpecialRegexChars() throws Exception {
		String showName = "The Simpsons";
		String title = "$pringfield";
		int seasonNum = 5;
		int episodeNum = 10;
		File file = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "the.simpsons.5.10.avi");
		file.createNewFile();
		
		Show show = new Show("1", showName, "http://www.tvrage.com/shows/id-6190");
		Season season5 = new Season(seasonNum);
		season5.addEpisode(episodeNum, title);
		show.setSeason(seasonNum, season5);
		ShowStore.addShow(showName, show);
		
		FileEpisode fileEpisode = new FileEpisode(showName, seasonNum, episodeNum, file);
		fileEpisode.setStatus(EpisodeStatus.RENAMED);
		
		String newFilename = fileEpisode.getNewFilename(mockUserPrefs);
		
		assertEquals("The Simpsons [5x10] $pringfield.avi", newFilename);
	}
	
	/**
	 * Ensure that Firefly episodes are ordered correcrly - ie. by DVD not TV order
	 */
	@Test
	public void testFireflyEpisodeOrder() throws Exception {
		String showName = "Firefly";
		int seasonNum = 1;
		int episodeNum = 1;
		File file = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "firefly.1.01.the.train.job.avi");
		file.createNewFile();
		
		// Verify that the show was already found in the store
		ShowStore.getShow(showName, mockListener);
		verify(mockListener).downloaded(any(Show.class));
		
		FileEpisode episode = new FileEpisode(showName, seasonNum, episodeNum, file);
		episode.setStatus(EpisodeStatus.DOWNLOADED);
		
		String newFilename = episode.getNewFilename(mockUserPrefs);
		
		// Ensure that 1x01 is Serenity and not The Train Job (as per tvrage.com)
		assertEquals("Firefly [1x01] Serenity.avi", newFilename);
	}
	
	@After
	public void teardown() throws Exception {
		for (File file : testFiles) {
			logger.info("Deleting " + file);
			file.delete();
		}
	}
}
