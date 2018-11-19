package org.tvrenamer.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.FileEpisode;
import org.tvrenamer.model.MoveObserver;
import org.tvrenamer.model.util.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
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

    private static final EpisodeTestData robotChicken0704 = new EpisodeTestData.Builder()
        .inputFilename("robot chicken/7x04.Rebel.Appliance.mp4")
        .filenameShow("robot chicken")
        .properShowName("Robot Chicken")
        .seasonNumString("7")
        .episodeNumString("04")
        .filenameSuffix(".mp4")
        .episodeTitle("Rebel Appliance")
        .episodeId("4874676")
        .replacementMask("S%0sE%0e %t")
        .expectedReplacement("S07E04 Rebel Appliance")
        .build();

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private Path destDir;
    private FileEpisode episode;
    private Path srcFile;
    private Path srcDir;
    private Path expectedDest;

    private void setValues(final EpisodeTestData epdata) {
        Path tempPath = tempFolder.getRoot().toPath();
        Path sandbox = tempPath.resolve("input");
        destDir = tempPath.resolve("output");

        FileMover.userPrefs.setDestinationDirectory(destDir.toString());
        FileMover.userPrefs.setRenameReplacementString(epdata.replacementMask);

        episode = epdata.createFileEpisode(sandbox);
        srcFile = episode.getPath();
        srcDir = srcFile.getParent();
        if (srcDir == null) {
            // This really should not be the problem, but give it a shot.
            srcDir = srcFile.toAbsolutePath().getParent();
        }
        assertNotNull(srcDir);

        String seasonFolder = FileMover.userPrefs.getSeasonPrefix() + epdata.seasonNum;
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
        assertTrue("did not move " + srcFile + " to expected destination "
                   + expectedDest, Files.exists(expectedDest));
    }

    private void assertNotMoved() {
        assertTrue("although set to read-only " + srcFile
                   + " is no longer in place",
                   Files.exists(srcFile));
        assertTrue("although " + srcFile + " was read-only, destination "
                   + expectedDest + " was created",
                   Files.notExists(expectedDest));
        // We expect to create the actual dest dir -- the top level.
        // (Though, if not, that's ok, too.)
        // Presumably, in trying to move the file, we created some subdirs.
        // If so, they should be cleaned up by the time we get here.
        assertTrue("extra files were created even though couldn't move file",
                   Files.notExists(destDir) || TestUtils.isDirEmpty(destDir));
    }

    private void assertTimestamp(long expected) {
        long actualMillis = 0L;
        try {
            FileTime actualTimestamp = Files.getLastModifiedTime(expectedDest);
            actualMillis = actualTimestamp.toMillis();
        } catch (IOException ioe) {
            fail("could not obtain timestamp of " + expectedDest);
        }

        // We always get the current time AFTER the move is done.  So we're not
        // doing absolute value here.  We definitely expect the actual timestamp
        // to be a little bit before what we got for "now".
        long difference = expected - actualMillis;
        assertTrue("the timestamp of " + expectedDest + " was off by "
                   + difference + " milliseconds",
                   difference < 1000);
    }

    @Test
    public void testFileMover() {
        setValues(robotChicken0704);
        assertReady();

        FileMover mover = new FileMover(episode);
        mover.call();
        long now = System.currentTimeMillis();

        assertMoved();
        assertTimestamp(now);
    }

    @Test
    public void testFileMoverCannotMove() {
        setValues(robotChicken0704);
        TestUtils.setReadOnly(srcFile);
        TestUtils.setReadOnly(srcDir);
        assertReady();

        if (!Environment.IS_WINDOWS) {
            FileMover mover = new FileMover(episode);
            boolean didMove = mover.call();

            // Allow the framework to clean up by making the
            // files writable again.
            TestUtils.setWritable(srcDir);
            TestUtils.setWritable(srcFile);

            assertNotMoved();
            assertFalse("FileMover.call returned true on read-only file",
                        didMove);
        }
    }

    private static class FutureCompleter implements MoveObserver {
        private final CompletableFuture<Boolean> future;

        FutureCompleter(final CompletableFuture<Boolean> future) {
            this.future = future;
        }

        public void initializeProgress(long max) {
            // no-op
        }

        public void setProgressValue(long value) {
            // no-op
        }

        public void setProgressStatus(String status) {
            // no-op
        }

        public void finishProgress(FileEpisode episode) {
            future.complete(episode.isSuccess());
        }
    }

    @Test
    public void testMoveRunner() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        setValues(robotChicken0704);
        assertReady();

        FileMover mover = new FileMover(episode);
        mover.addObserver(new FutureCompleter(future));

        List<FileMover> moveList = new ArrayList<>();
        moveList.add(mover);

        MoveRunner runner = new MoveRunner(moveList);
        try {
            runner.runThread();
            boolean didMove = future.get(4, TimeUnit.SECONDS);
            long now = System.currentTimeMillis();
            assertMoved();
            assertTimestamp(now);
            assertTrue("got " + didMove
                       + " in finishProgress for successful move",
                       didMove);
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

    @Test
    public void testMoveRunnerCannotMove() {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        setValues(robotChicken0704);
        TestUtils.setReadOnly(srcFile);
        TestUtils.setReadOnly(srcDir);
        assertReady();

        FileMover mover = new FileMover(episode);
        mover.addObserver(new FutureCompleter(future));

        List<FileMover> moveList = new ArrayList<>();
        moveList.add(mover);

        MoveRunner runner = new MoveRunner(moveList);
        try {
            runner.runThread();
            boolean didMove = future.get(4, TimeUnit.SECONDS);

            if (!Environment.IS_WINDOWS) {
                // We expect that the file will not be moved, and that the
                // observer will be called with a negative status.
                assertNotMoved();
                assertFalse("expected to get false in finish progress, but got "
                            + didMove, didMove);
            }
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
        } finally {
            // Allow the framework to clean up by making the
            // files writable again.
            TestUtils.setWritable(srcDir);
            TestUtils.setWritable(srcFile);
        }
    }
}
