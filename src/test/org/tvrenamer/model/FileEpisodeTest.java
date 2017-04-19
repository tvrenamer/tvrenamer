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

        String filenameShow = "the.simpsons";
        String seasonNumString = "5";
        String episodeNumString = "10";
        String resolution = "720p";

        String filename = filenameShow + "." + seasonNumString + "."
            + episodeNumString + "." + resolution + ".avi";
        Path path = TEMP_DIR.resolve(filename);
        createFile(path);

        FileEpisode episode = new FileEpisode(path);
        episode.setFilenameShow(filenameShow);
        episode.setFilenameSeason(seasonNumString);
        episode.setFilenameEpisode(episodeNumString);
        episode.setFilenameResolution(resolution);

        String showName = "The Simpsons";
        Show show = new Show("71663", showName, "http://thetvdb.com/?tab=series&id=71663");
        show.preferProductionOrdering();
        ShowStore.addShow(filenameShow, show);

        String title = "$pringfield";
        EpisodeInfo info = new EpisodeInfo.Builder()
            .episodeId("55542")
            .seasonNumber(seasonNumString)
            .episodeNumber(episodeNumString)
            .episodeName(title)
            .build();
        EpisodeInfo[] dummyArray = new EpisodeInfo[1];
        dummyArray[0] = info;
        show.addEpisodes(dummyArray);

        episode.setStatus(EpisodeStatus.GOT_LISTINGS);

        assertEquals("The Simpsons [5x10] $pringfield 720p.avi",
                     episode.getReplacementText());
    }

    /**
     * Ensure that colons (:) don't make it into the renamed filename <br />
     * Fixes <a href="https://github.com/tvrenamer/tvrenamer/issues/46">Issue 46</a>
     */
    @Test
    public void testColon() throws Exception {
        prefs.setRenameReplacementString("%S [%sx%e] %t");

        String filenameShow = "steven.segal.lawman";
        String seasonNumString = "1";
        String episodeNumString = "01";

        String filename = filenameShow + "." + seasonNumString + "."
            + episodeNumString + ".avi";
        Path path = TEMP_DIR.resolve(filename);
        createFile(path);

        FileEpisode episode = new FileEpisode(path);
        episode.setFilenameShow(filenameShow);
        episode.setFilenameSeason(seasonNumString);
        episode.setFilenameEpisode(episodeNumString);

        String showName = "Steven Seagal: Lawman";
        Show show = new Show("126841", showName, "http://thetvdb.com/?tab=series&id=126841&lid=7");
        show.preferProductionOrdering();
        ShowStore.addShow(filenameShow, show);

        String title = "The Way of the Gun";
        EpisodeInfo info = new EpisodeInfo.Builder()
            .episodeId("1111")
            .seasonNumber(seasonNumString)
            .episodeNumber(episodeNumString)
            .episodeName(title)
            .build();
        EpisodeInfo[] dummyArray = new EpisodeInfo[1];
        dummyArray[0] = info;
        show.addEpisodes(dummyArray);
        episode.setStatus(EpisodeStatus.GOT_LISTINGS);

        String newFilename = episode.getReplacementText();
        assertFalse("Resulting filename must not contain a ':' as it breaks Windows",
                    newFilename.contains(":"));
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
