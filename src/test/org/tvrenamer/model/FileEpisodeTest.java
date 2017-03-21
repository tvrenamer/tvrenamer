package org.tvrenamer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.util.FileUtilities;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileEpisodeTest {
    private static Logger logger = Logger.getLogger(FileEpisodeTest.class.getName());

    private static final String TEMP_DIR_NAME = System.getProperty("java.io.tmpdir");
    private static final Path TEMP_DIR = Paths.get(TEMP_DIR_NAME);

    private List<Path> testFiles;

    private UserPreferences prefs;
    private ShowInformationListener mockListener;

    @Before
    public void setUp() throws Exception {
        testFiles = new ArrayList<>();
        prefs = UserPreferences.getInstance();
        prefs.setMoveEnabled(false);
        prefs.setRenameEnabled(true);
        mockListener = mock(ShowInformationListener.class);
    }

    /**
     * Test case for <a href="https://github.com/tvrenamer/tvrenamer/issues/36">Issue 36</a> where the title
     * "$pringfield" breaks the regex used for String.replaceAll()
     */
    @Test
    public void testGetNewFilenameSpecialRegexChars() throws Exception {
        prefs.setRenameReplacementString("%S [%sx%e] %t %r");

        String filename = "the.simpsons.5.10.720p.avi";
        Path path = TEMP_DIR.resolve(filename);
        createFile(path);

        String showName = "The Simpsons";
        String title = "$pringfield";
        int seasonNum = 5;
        int episodeNum = 10;
        String resolution = "720p";

        Show show = new Show("1", showName, "http://thetvdb.com/?tab=series&id=71663");
        Season season5 = new Season(seasonNum);
        season5.addEpisode(episodeNum, title, LocalDate.now());
        show.setSeason(seasonNum, season5);
        ShowStore.addShow(showName, show);

        FileEpisode episode = new FileEpisode(showName, seasonNum, episodeNum, resolution, path);
        episode.setStatus(EpisodeStatus.DOWNLOADED);

        String newFilename = episode.getNewFilename();

        assertEquals("The Simpsons [5x10] $pringfield 720p.avi", newFilename);
    }

    /**
     * Ensure that colons (:) don't make it into the renamed filename <br />
     * Fixes <a href="https://github.com/tvrenamer/tvrenamer/issues/46">Issue 46</a>
     */
    @Test
    public void testColon() throws Exception {
        prefs.setRenameReplacementString("%S [%sx%e] %t");

        String filename = "steven.segal.lawman.1.01.avi";
        Path path = TEMP_DIR.resolve(filename);
        createFile(path);

        String showName = "Steven Seagal: Lawman";
        String title = "The Way of the Gun";
        int seasonNum = 1;
        int episodeNum = 1;
        String resolution = "";

        Show show = new Show("1", showName, "http://thetvdb.com/?tab=series&id=126841&lid=7");
        Season season1 = new Season(seasonNum);
        season1.addEpisode(episodeNum, title, LocalDate.now());
        show.setSeason(seasonNum, season1);
        ShowStore.addShow(showName, show);

        FileEpisode fileEpisode = new FileEpisode(showName, seasonNum, episodeNum, resolution, path);
        fileEpisode.setStatus(EpisodeStatus.RENAMED);

        String newFilename = fileEpisode.getNewFilename();

        assertFalse("Resulting filename must not contain a ':' as it breaks Windows", newFilename.contains(":"));
    }

    /**
     * Helper method to physically create the file and add to file list for later deletion.
     */
    private void createFile(Path path) throws IOException {
        Files.createFile(path);
        testFiles.add(path);
    }

    @After
    public void teardown() throws Exception {
        for (Path path : testFiles) {
            logger.info("Deleting " + path);
            FileUtilities.deleteFile(path);
        }
    }
}
