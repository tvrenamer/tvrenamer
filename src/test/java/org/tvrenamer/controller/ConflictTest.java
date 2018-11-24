package org.tvrenamer.controller;

import org.junit.Test;

import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.util.Constants;

import java.nio.file.Path;

/**
 * ConflictTest -- test file moving functionality when there is a conflict.
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
public class ConflictTest extends MoveTest {

    private static final EpisodeTestData bigBang0322 = new EpisodeTestData.Builder()
        .inputFilename("big.bang.theory.322.mp4")
        .filenameShow("big.bang.theory")
        .properShowName("The Big Bang Theory")
        .seasonNumString("3")
        .episodeNumString("22")
        .filenameSuffix(".mp4")
        .episodeTitle("The Staircase Implementation")
        .episodeId("2063661")
        .replacementMask("%S S%0sE%0e %t")
        .expectedReplacement("The Big Bang Theory S03E22 The Staircase Implementation")
        .build();

    private void makeConflict(final EpisodeTestData epdata,
                              final FileMover mover)
    {
        Path baseDestDir = mover.getMoveToDirectory();
        Path desiredDestDir = baseDestDir.resolve(Constants.DUPLICATES_DIRECTORY);
        String desiredFilename = epdata.expectedReplacement
            + mover.versionString()
            + epdata.filenameSuffix;

        // Create a file in the way, so that we will not be able to move the
        // source file to the desired destination
        TestUtils.createFile(desiredDestDir, desiredFilename);

        if (mover.destIndex == null) {
            mover.destIndex = 2;
        } else {
            mover.destIndex++;
        }

        expectedDest = desiredDestDir.resolve(desiredFilename);
    }

    @Test
    public void testFileMoverConflict() {
        setValues(bigBang0322);
        assertReady();

        FileMover mover = new FileMover(episode);
        makeConflict(bigBang0322, mover);
        mover.call();
        long now = System.currentTimeMillis();

        assertMoved();
        assertTimestamp(now);
    }
}
