package org.tvrenamer.controller;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.NoOpProgressUpdater;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * MoveTest -- test FileMover and MoveRunner.
 *
 * This file tests the functionality of FileMover and MoveRunner while trying
 * to avoid dependency on other classes as much as possible.
 *
 * The FileMover requires a FileEpisode, which has a whole bunch of information
 * in it, which it obtains from various places, including the user preferences,
 * the FilenameParser class, and the TVDB.  But we already have functionality
 * to create a filled-in FileEpisode without relying on the parser or provider.
 *
 * It does still rely on the user preferences, so we are sure to set all the
 * relevant settings.
 *
 * This class uses the TemporaryFolder functionality of jUnit to create and
 * move files that will be automatically cleaned up after each test completes.
 *
 */
public class MoveTest {
    /**
     * The specifics of how we rename and move files is very dependent on the
     * user preferences.  Set the values we expect here, before we run any
     * specific tests.
     *
     */
    @BeforeClass
    public static void initializePrefs() {
        FileMover.userPrefs.setCheckForUpdates(false);

        FileMover.userPrefs.setSeasonPrefix("Season ");
        FileMover.userPrefs.setSeasonPrefixLeadingZero(false);
        FileMover.userPrefs.setMoveSelected(true);
        FileMover.userPrefs.setRenameSelected(true);
        FileMover.userPrefs.setRemoveEmptiedDirectories(false);

        // We don't want to see "info" level messages, or even warnings,
        // as we run tests.  Setting the level to "SEVERE" means nothing
        // below that level will be printed.
        FileMover.logger.setLevel(Level.SEVERE);
    }

    private static EpisodeTestData robotChicken0704 = new EpisodeTestData.Builder()
        .inputFilename("robot chicken/7x04.Rebel.Appliance.mp4")
        .filenameShow("robot chicken")
        .properShowName("Robot Chicken")
        .seasonNumString("7")
        .episodeNumString("04")
        .filenameSuffix(".mp4")
        .episodeTitle("Rebel Appliance")
        .episodeId("4874676")
        .replacementMask("S%0sE%0e %t %yyyy.%0m.%0d")
        .expectedReplacement("S07E04 Rebel Appliance 2018.10.14")
        .build();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private Path tempPath = null;
    private Path sandbox;
    private Path destDir;
    private FileEpisode episode;
    private Path srcFile;
    private String seasonFolder;
    private Path expectedDest;

    private void setValues(final EpisodeTestData epdata) {
        tempPath = tempFolder.getRoot().toPath();
        sandbox = tempPath.resolve("input");
        destDir = tempPath.resolve("output");

        FileMover.userPrefs.setDestinationDirectory(destDir.toString());
        FileMover.userPrefs.setRenameReplacementString(epdata.replacementMask);

        episode = epdata.createFileEpisode(sandbox);
        srcFile = episode.getPath();

        seasonFolder = FileMover.userPrefs.getSeasonPrefix() + epdata.seasonNum;
        expectedDest = destDir
            .resolve(epdata.properShowName)
            .resolve(seasonFolder)
            .resolve(epdata.expectedReplacement + epdata.filenameSuffix);
    }

    private void assertReady() {
        assertNotNull("failed to create FileEpisode", episode);
        assertNotNull("FileEpisode does not have path", srcFile);

        assertTrue("failed to create file for FileEpisode",
                   Files.exists(srcFile));
        assertTrue("output dir exists before it should",
                   Files.notExists(destDir));
    }

    private void assertMoved() {
        assertTrue("did not move " + srcFile + " to expected destination",
                   Files.exists(expectedDest));
    }

    @Test
    public void testFileMover() {
        setValues(robotChicken0704);
        assertReady();

        FileMover mover = new FileMover(episode);
        mover.call();

        assertMoved();
    }

    @Test
    public void testMoveRunner() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        setValues(robotChicken0704);
        assertReady();

        FileMover mover = new FileMover(episode);

        List<FileMover> moveList = new ArrayList<>();
        moveList.add(mover);

        MoveRunner runner = new MoveRunner(moveList);
        runner.setUpdater(new NoOpProgressUpdater() {
                public void finish() {
                    MoveTest.this.assertMoved();
                    future.complete(true);
                }
            });
        try {
            runner.runThread();
            future.get(4, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            String failMsg = "timeout trying to move " + srcFile;
            String exceptionMessage = e.getMessage();
            if (exceptionMessage != null) {
                failMsg += exceptionMessage;
            } else {
                failMsg += "(no message)";
            }
            fail(failMsg);
        } catch (Exception e) {
            fail("failure (possibly interrupted?) trying to move "
                 + srcFile + ": " + e.getMessage());
        }
    }
}
