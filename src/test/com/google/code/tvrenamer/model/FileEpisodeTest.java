package com.google.code.tvrenamer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
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
	private ShowInformationListener mockListener;

	@Before
	public void setUp() throws Exception {
		testFiles = new ArrayList<File>();
		mockListener = mock(ShowInformationListener.class);
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
		createFile(file);
		
		Show show = new Show("1", showName, "http://www.tvrage.com/shows/id-6190");
		Season season5 = new Season(seasonNum);
		season5.addEpisode(episodeNum, title);
		show.setSeason(seasonNum, season5);
		ShowStore.addShow(showName, show);
		
		FileEpisode fileEpisode = new FileEpisode(showName, seasonNum, episodeNum, file);
		fileEpisode.setStatus(EpisodeStatus.RENAMED);
		
		String newFilename = fileEpisode.getNewFilename();
		
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
		createFile(file);
		
		// Verify that the show was already found in the store
		ShowStore.getShow(showName, mockListener);
		verify(mockListener).downloaded(any(Show.class));
		
		FileEpisode episode = new FileEpisode(showName, seasonNum, episodeNum, file);
		episode.setStatus(EpisodeStatus.DOWNLOADED);
		
		String newFilename = episode.getNewFilename();
		
		// Ensure that 1x01 is Serenity and not The Train Job (as per tvrage.com)
		assertEquals("Firefly [1x01] Serenity.avi", newFilename);
	}
	
	/**
	 * Ensure that colons (:) don't make it into the renamed filename
	 * <br />Fixes <a href="http://code.google.com/p/tv-renamer/issues/detail?id=46">Defect 46</a>
	 */
	@Test
	public void testColon() throws Exception {
		String showName = "Steven Seagal: Lawman";
		String title = "The Way of the Gun";
		int seasonNum = 1;
		int episodeNum = 1;
		File file = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "steven.segal.lawman.1.01.avi");
		createFile(file);
		
		Show show = new Show("1", showName, "http://www.tvrage.com/shows/id-20664");
		Season season1 = new Season(seasonNum);
		season1.addEpisode(episodeNum, title);
		show.setSeason(seasonNum, season1);
		ShowStore.addShow(showName, show);
		
		FileEpisode fileEpisode = new FileEpisode(showName, seasonNum, episodeNum, file);
		fileEpisode.setStatus(EpisodeStatus.RENAMED);
		
		String newFilename = fileEpisode.getNewFilename();
		
		assertFalse("Resulting filename must not contain a ':' as it breaks Windows", newFilename.contains(":"));
	}
	
	/**
	 * Helper method to physically create the file and add to file list for later deletion.
	 */
	private void createFile(File file) throws IOException {
		file.createNewFile();
		testFiles.add(file);
	}
	
	@After
	public void teardown() throws Exception {
		for (File file : testFiles) {
			logger.info("Deleting " + file);
			file.delete();
		}
	}
}
