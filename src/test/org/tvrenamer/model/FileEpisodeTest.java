package org.tvrenamer.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.tvrenamer.model.util.Constants.*;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.controller.util.FileUtilities;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileEpisodeTest {
    private static Logger logger = Logger.getLogger(FileEpisodeTest.class.getName());

    /**
     * Just an unordered list of test data.
     */
    private static final List<EpisodeTestData> values = new ArrayList<>();

    /**
     * We don't want to write directly into the temp dir.  That could make it a lot
     * trickier to clean up.  Always create our own subdirectory within the temp
     * dir, and do all our work in there.  We may create further sub-directories
     * below this one.
     */
    private static final Path OUR_TEMP_DIR = TMP_DIR.resolve(APPLICATION_NAME);

    private UserPreferences prefs = UserPreferences.getInstance();

    // Helper method.  Basically mkdirs, but expects that the directory does
    // NOT exist, and considers it an error if it does.  Also does one final
    // check that, after we believe we've created it, the directory actually
    // does exist.
    private void createNewDirectory(Path newdir) {
        if (Files.exists(newdir)) {
            fail("directory " + newdir + " already exists.  It should not!");
        }
        boolean madeDir = FileUtilities.mkdirs(newdir);
        if (false == madeDir) {
            fail("unable to create directory " + newdir);
        }
        if (!Files.exists(newdir)) {
            fail("directory " + newdir + " still does not exist!");
        }
    }

    // Helper method to give information about an exception we catch, and
    // automatically fail because of it.
    private void verboseFail(String msg, Exception e) {
        String failMsg = msg + ": " + e.getClass().getName() + " ";
        String exceptionMessage = e.getMessage();
        if (exceptionMessage != null) {
            failMsg += exceptionMessage;
        } else {
            failMsg += "(no message)";
        }
        e.printStackTrace();
        fail(failMsg);
    }

    /**
     * Just makes sure our temp directory exists.
     */
    @Before
    public void setUp() {
        createNewDirectory(OUR_TEMP_DIR);
    }

    /* This method is intended to delete the temp files and our temp directory,
     * and to report failure if it is unable to do so.  Along the way, we check
     * for several extremely-unlikely-to-happen errors, just in case.  But we
     * don't ever want to interrupt the cleanup to report a failure.  Be sure to
     * try to delete each file and the directory before aborting due to any
     * failure.
     */
    private void teardown(List<Path> testFiles) {
        List<Path> outsideFailures = new ArrayList<>();
        List<Path> deleteFailures = new ArrayList<>();
        for (Path path : testFiles) {
            Path parent = path.getParent();
            boolean expected = FileUtilities.isSameFile(OUR_TEMP_DIR, parent);
            if (!expected) {
                outsideFailures.add(path);
            }
            logger.fine("Deleting " + path);
            boolean deleted = FileUtilities.deleteFile(path);
            if (!deleted) {
                deleteFailures.add(path);
            }
        }
        if (FileUtilities.isDirEmpty(OUR_TEMP_DIR)) {
            boolean rmed = FileUtilities.rmdir(OUR_TEMP_DIR);
            if (!rmed) {
                fail("unable to delete empty temp directory " + OUR_TEMP_DIR);
            }
        } else {
            fail("did not succeed in emptying temp directory " + OUR_TEMP_DIR);
        }
        if (!deleteFailures.isEmpty()) {
            fail("failed to delete " + deleteFailures.size() + " temp file(s)");
        }
        if (!outsideFailures.isEmpty()) {
            fail("created " + outsideFailures.size() + " file(s) in the wrong place");
        }
    }

    // We're going to add a bunch of test cases to the list.  The first two have been in this file
    // for a long time (in a different form), and are testing specific functionality.  The rest
    // are from the TVRenamer test.  In some cases, there's a strong hint as to what they're
    // testing, and in others, maybe not so much.  Try to refine these test cases down and expand
    // them so that specific functionality is being tested.
    //
    // The "BeforeClass" annotation means any method so marked will be run before the tests, so
    // we don't have to stuff it all into one huge method.  We can break it down arbitrarily.
    // As we change the test data to be more meaningful, we could change the method names as well.

    @BeforeClass
    public static void setupValues() {
        /**
         * Test case for <a href="https://github.com/tvrenamer/tvrenamer/issues/36">Issue 36</a>
         * where the title "$pringfield" breaks the regex used for String.replaceAll()
         */
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("the.simpsons")
                   .properShowName("The Simpsons")
                   .showId("71663")
                   .seasonNumString("5")
                   .episodeNumString("10")
                   .episodeResolution("720p")
                   .episodeTitle("$pringfield")
                   .episodeId("55542")
                   .replacementMask("%S [%sx%e] %t %r")
                   .documentation("makes sure regex characters are included literally in filename")
                   .expectedReplacement("The Simpsons [5x10] $pringfield 720p")
                   .build());
        /**
         * Ensure that colons (:) don't make it into the renamed filename <br />
         * Fixes <a href="https://github.com/tvrenamer/tvrenamer/issues/46">Issue 46</a>
         */
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("steven.segal.lawman")
                   .properShowName("Steven Seagal: Lawman")
                   .showId("126841")
                   .seasonNumString("1")
                   .episodeNumString("01")
                   .episodeTitle("The Way of the Gun")
                   .replacementMask("%S [%sx%e] %t")
                   .documentation("makes sure illegal characters are not included in filename")
                   .expectedReplacement("Steven Seagal - Lawman [1x1] The Way of the Gun")
                   .build());
    }

    @BeforeClass
    public static void setupValuesBadSuffix() {
        // This example essentially has no filename suffix, in reality.  Instead it has
        // "junk" after its final dot.  Luckily, it works out just the same.  If we used
        // an example that had important metadata after its dot, we'd probably fail.
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("ncis")
                   .properShowName("NCIS")
                   .seasonNumString("13")
                   .episodeNumString("04")
                   .filenameSuffix(".hdtv-lol")
                   .episodeTitle("Double Trouble")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("NCIS S13E04 Double Trouble")
                   .build());
    }

    @BeforeClass
    public static void setupValues03() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("nip tuck")
                   .properShowName("Nip/Tuck")
                   .seasonNumString("6")
                   .episodeNumString("1")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Don Hoberman")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Nip-Tuck S06E01 Don Hoberman")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("human target 2010")
                   .properShowName("Human Target (2010)")
                   .seasonNumString("1")
                   .episodeNumString("2")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Rewind")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Human Target (2010) S01E02 Rewind")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("castle 2009")
                   .properShowName("Castle (2009)")
                   .seasonNumString("1")
                   .episodeNumString("9")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Little Girl Lost")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Castle (2009) S01E09 Little Girl Lost")
                   .build());
    }

    @BeforeClass
    public static void setupValues04() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("reign 2013")
                   .properShowName("Reign (2013)")
                   .seasonNumString("1")
                   .episodeNumString("20")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Higher Ground")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Reign (2013) S01E20 Higher Ground")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("the americans 2013")
                   .properShowName("The Americans (2013)")
                   .seasonNumString("2")
                   .episodeNumString("10")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Yousaf")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("The Americans (2013) S02E10 Yousaf")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("house of cards us")
                   .properShowName("House of Cards (US)")
                   .seasonNumString("1")
                   .episodeNumString("6")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Chapter 6")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("House of Cards (US) S01E06 Chapter 6")
                   .build());
    }

    @BeforeClass
    public static void setupValues05() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("modern family")
                   .properShowName("Modern Family")
                   .seasonNumString("5")
                   .episodeNumString("12")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Under Pressure")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Modern Family S05E12 Under Pressure")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("game of thrones")
                   .properShowName("Game of Thrones")
                   .seasonNumString("5")
                   .episodeNumString("1")
                   .filenameSuffix(".mp4")
                   .episodeTitle("The Wars to Come")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Game of Thrones S05E01 The Wars to Come")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("24")
                   .properShowName("24")
                   .seasonNumString("8")
                   .episodeNumString("1")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Day 8: 4:00 P.M. - 5:00 P.M.")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("24 S08E01 Day 8 - 4 -00 P.M. - 5 -00 P.M.")
                   .build());
    }

    @BeforeClass
    public static void setupValues06() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("24")
                   .properShowName("24")
                   .seasonNumString("7")
                   .episodeNumString("18")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Day 7: 1:00 A.M. - 2:00 A.M.")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("24 S07E18 Day 7 - 1 -00 A.M. - 2 -00 A.M.")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("dexter")
                   .properShowName("Dexter")
                   .seasonNumString("4")
                   .episodeNumString("7")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Slack Tide")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Dexter S04E07 Slack Tide")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("jag")
                   .properShowName("JAG")
                   .seasonNumString("10")
                   .episodeNumString("1")
                   .filenameSuffix(".avi")
                   .episodeTitle("Hail and Farewell, Part II (2)")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("JAG S10E01 Hail and Farewell, Part II (2)")
                   .build());
    }

    @BeforeClass
    public static void setupValues07() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("lost")
                   .properShowName("Lost")
                   .seasonNumString("6")
                   .episodeNumString("5")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Lighthouse")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Lost S06E05 Lighthouse")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("warehouse 13")
                   .properShowName("Warehouse 13")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Pilot")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Warehouse 13 S01E01 Pilot")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("one tree hill")
                   .properShowName("One Tree Hill")
                   .seasonNumString("7")
                   .episodeNumString("14")
                   .filenameSuffix(".avi")
                   .episodeTitle("Family Affair")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("One Tree Hill S07E14 Family Affair")
                   .build());
    }

    @BeforeClass
    public static void setupValues08() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("gossip girl")
                   .properShowName("Gossip Girl")
                   .seasonNumString("3")
                   .episodeNumString("15")
                   .filenameSuffix(".avi")
                   .episodeTitle("The Sixteen Year Old Virgin")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Gossip Girl S03E15 The Sixteen Year Old Virgin")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("smallville")
                   .properShowName("Smallville")
                   .seasonNumString("9")
                   .episodeNumString("14")
                   .filenameSuffix(".avi")
                   .episodeTitle("Conspiracy")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Smallville S09E14 Conspiracy")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("smallville")
                   .properShowName("Smallville")
                   .seasonNumString("9")
                   .episodeNumString("15")
                   .filenameSuffix(".avi")
                   .episodeTitle("Escape")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Smallville S09E15 Escape")
                   .build());
    }

    @BeforeClass
    public static void setupValues09() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("the big bang theory")
                   .properShowName("The Big Bang Theory")
                   .seasonNumString("3")
                   .episodeNumString("18")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("The Pants Alternative")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("The Big Bang Theory S03E18 The Pants Alternative")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("dexter")
                   .properShowName("Dexter")
                   .seasonNumString("5")
                   .episodeNumString("5")
                   .filenameSuffix(".mkv")
                   .episodeTitle("First Blood")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Dexter S05E05 First Blood")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("lost")
                   .properShowName("Lost")
                   .seasonNumString("2")
                   .episodeNumString("7")
                   .filenameSuffix(".mkv")
                   .episodeTitle("The Other 48 Days")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Lost S02E07 The Other 48 Days")
                   .build());
    }

    @BeforeClass
    public static void setupValues10() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("californication")
                   .properShowName("Californication")
                   .seasonNumString("7")
                   .episodeNumString("4")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Dicks")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Californication S07E04 Dicks")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("continuum")
                   .properShowName("Continuum")
                   .seasonNumString("3")
                   .episodeNumString("7")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Waning Minutes")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Continuum S03E07 Waning Minutes")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("elementary")
                   .properShowName("Elementary")
                   .seasonNumString("2")
                   .episodeNumString("23")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Art in the Blood")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Elementary S02E23 Art in the Blood")
                   .build());
    }

    @BeforeClass
    public static void setupValues11() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("family guy")
                   .properShowName("Family Guy")
                   .seasonNumString("12")
                   .episodeNumString("19")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Meg Stinks!")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Family Guy S12E19 Meg Stinks!")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("fargo")
                   .properShowName("Fargo")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .filenameSuffix(".mp4")
                   .episodeTitle("The Crocodile's Dilemma")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Fargo S01E01 The Crocodile's Dilemma")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("girls")
                   .properShowName("Girls")
                   .seasonNumString("3")
                   .episodeNumString("11")
                   .filenameSuffix(".mp4")
                   .episodeTitle("I Saw You")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Girls S03E11 I Saw You")
                   .build());
    }

    @BeforeClass
    public static void setupValues12() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("grimm")
                   .properShowName("Grimm")
                   .seasonNumString("3")
                   .episodeNumString("19")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Nobody Knows the Trubel I've Seen")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Grimm S03E19 Nobody Knows the Trubel I've Seen")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("new girl")
                   .properShowName("New Girl")
                   .seasonNumString("3")
                   .episodeNumString("23")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Cruise")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("New Girl S03E23 Cruise")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("nurse jackie")
                   .properShowName("Nurse Jackie")
                   .seasonNumString("6")
                   .episodeNumString("4")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Jungle Love")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Nurse Jackie S06E04 Jungle Love")
                   .build());
    }

    @BeforeClass
    public static void setupValues13() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("offspring")
                   .properShowName("Offspring")
                   .seasonNumString("5")
                   .episodeNumString("1")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Back in the Game")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Offspring S05E01 Back in the Game")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("robot chicken")
                   .properShowName("Robot Chicken")
                   .seasonNumString("7")
                   .episodeNumString("4")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Rebel Appliance")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Robot Chicken S07E04 Rebel Appliance")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("supernatural")
                   .properShowName("Supernatural")
                   .seasonNumString("9")
                   .episodeNumString("21")
                   .filenameSuffix(".mp4")
                   .episodeTitle("King of the Damned")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Supernatural S09E21 King of the Damned")
                   .build());
    }

    @BeforeClass
    public static void setupValues14() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("the big bang theory")
                   .properShowName("The Big Bang Theory")
                   .seasonNumString("7")
                   .episodeNumString("23")
                   .filenameSuffix(".mp4")
                   .episodeTitle("The Gorilla Dissolution")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("The Big Bang Theory S07E23 The Gorilla Dissolution")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("the good wife")
                   .properShowName("The Good Wife")
                   .seasonNumString("5")
                   .episodeNumString("20")
                   .filenameSuffix(".mp4")
                   .episodeTitle("The Deep Web")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("The Good Wife S05E20 The Deep Web")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("veep")
                   .properShowName("Veep")
                   .seasonNumString("3")
                   .episodeNumString("5")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Fishing")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Veep S03E05 Fishing")
                   .build());
    }

    @BeforeClass
    public static void setupValues15() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("witches of east end")
                   .properShowName("Witches of East End")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Pilot")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Witches of East End S01E01 Pilot")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("warehouse 13")
                   .properShowName("Warehouse 13")
                   .seasonNumString("5")
                   .episodeNumString("4")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Savage Seduction")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Warehouse 13 S05E04 Savage Seduction")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("the 100")
                   .properShowName("The 100")
                   .seasonNumString("2")
                   .episodeNumString("8")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Spacewalker")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("The 100 S02E08 Spacewalker")
                   .build());
    }

    @BeforeClass
    public static void setupValuesFirefly() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .filenameSuffix(".mp4")
                   .episodeTitle("The Train Job")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E01 The Train Job")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("2")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Bushwhacked")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E02 Bushwhacked")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("3")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Our Mrs. Reynolds")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E03 Our Mrs. Reynolds")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("4")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Jaynestown")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E04 Jaynestown")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("5")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Out of Gas")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E05 Out of Gas")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("6")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Shindig")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E06 Shindig")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("7")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Safe")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E07 Safe")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("8")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Ariel")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E08 Ariel")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("9")
                   .filenameSuffix(".mp4")
                   .episodeTitle("War Stories")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E09 War Stories")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("10")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Objects in Space")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E10 Objects in Space")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("11")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Serenity")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E11 Serenity")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("12")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Heart of Gold")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E12 Heart of Gold")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("13")
                   .filenameSuffix(".mp4")
                   .episodeTitle("Trash")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E13 Trash")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("firefly")
                   .properShowName("Firefly")
                   .seasonNumString("1")
                   .episodeNumString("14")
                   .filenameSuffix(".mp4")
                   .episodeTitle("The Message")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Firefly S01E14 The Message")
                   .build());
    }

    @BeforeClass
    public static void setupValues17() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("strike back")
                   .properShowName("Strike Back")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Chris Ryan's Strike Back, Episode 1")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Strike Back S01E01 Chris Ryan's Strike Back, Episode 1")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("lucifer")
                   .properShowName("Lucifer")
                   .seasonNumString("2")
                   .episodeNumString("3")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Sin-Eater")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Lucifer S02E03 Sin-Eater")
                   .build());
    }

    @BeforeClass
    public static void setupValues18() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("marvels agents of shield")
                   .properShowName("Marvel's Agents of S.H.I.E.L.D.")
                   .seasonNumString("4")
                   .episodeNumString("3")
                   .filenameSuffix(".mkv")
                   .episodeResolution("1080p")
                   .episodeTitle("Uprising")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Marvel's Agents of S.H.I.E.L.D. S04E03 Uprising")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("supernatural")
                   .properShowName("Supernatural")
                   .seasonNumString("11")
                   .episodeNumString("22")
                   .filenameSuffix(".mkv")
                   .showId("78901")
                   .episodeId("5590688")
                   .episodeResolution("1080p")
                   .episodeTitle("We Happy Few")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Supernatural S11E22 We Happy Few")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("supernatural")
                   .properShowName("Supernatural")
                   .seasonNumString("11")
                   .episodeNumString("22")
                   .filenameSuffix(".mkv")
                   .showId("78901")
                   .episodeId("5590688")
                   .episodeResolution("720p")
                   .episodeTitle("We Happy Few")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Supernatural S11E22 We Happy Few")
                   .build());
    }

    @BeforeClass
    public static void setupValues19() {
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("channel zero")
                   .properShowName("Channel Zero")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .filenameSuffix(".mkv")
                   .episodeResolution("480p")
                   .episodeTitle("You Have to Go Inside")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("Channel Zero S01E01 You Have to Go Inside")
                   .build());
        values.add(new EpisodeTestData.Builder()
                   .filenameShow("ncis")
                   .properShowName("NCIS")
                   .seasonNumString("14")
                   .episodeNumString("4")
                   .filenameSuffix(".mkv")
                   .episodeResolution("720p")
                   .episodeTitle("Love Boat")
                   // .replacementMask("%S [%sx%e] %t %r")
                   .replacementMask("%S S%0sE%0e %t")
                   .expectedReplacement("NCIS S14E04 Love Boat")
                   .build());
    }

    /**
     * Here's where all that data in <code>values</code> is turned into something.  Note this
     * doesn't use a lot of what the real program does; it doesn't fetch anything from the
     * Internet (or even from a cache), and it doesn't use listeners.  And, of course, it
     * doesn't use the UI, which is intimately tied to moving files in the real program.
     * But it does try to simulate the process.
     *
     * We start off by making sure our preferences are what we want.  Then we get a Path.
     * We turn that path into a FileEpisode, which initially is basically just a shell.
     * We then fill in information based on the filename.  (In the real program, the parser
     * in TVRenamer is used for this.)
     *
     * Then, once we know the part of the filename that we think represents the show name,
     * we find the actual show name, and create a Show object to represent it.  We store
     * the mapping between the query string and the Show object in ShowStore.  (In the real
     * program, we send the query string to the TVDB, it responds with options in XML, which we
     * parse, choose the best match, and use to create the Show object.)
     *
     * Once we have the Show object, we add the episodes.  Here, we're creating a single episode,
     * but the Show API always expects an array.  (In the real program, we get the episodes by
     * querying The TVDB with the show ID, and parsing the XML into an array of EpisodeInfo
     * objects.)  We create a one-element array and stick the EpisodeInfo into it, and add that
     * to the Show.
     *
     * Finally, we set the status of the FileEpisode to tell it we're finished downloading all
     * the episodes its show needs to know about, which enables getReplacementText to give us
     * the filename to use.  (If it didn't think we were finished adding episodes, it would
     * instead return a placeholder text.)
     *
     * Then, we're done.  We return the replacement text to the driver method, and let it
     * do the checking.
     */
    private String getReplacementBasename(EpisodeTestData data, Path path)
        throws IOException
    {
        prefs.setRenameReplacementString(data.replacementMask);

        String pathstring = path.toAbsolutePath().toString();

        FileEpisode episode = new FileEpisode(pathstring);
        episode.setFilenameShow(data.filenameShow);
        episode.setFilenameSeason(data.seasonNumString);
        episode.setFilenameEpisode(data.episodeNumString);
        episode.setFilenameResolution(data.episodeResolution);

        Show show = ShowStore.getOrAddShow(data.filenameShow, data.properShowName);
        show.preferProductionOrdering();
        episode.setShow(show);

        EpisodeInfo info = new EpisodeInfo.Builder()
            .episodeId(data.episodeId)
            .seasonNumber(data.seasonNumString)
            .episodeNumber(data.episodeNumString)
            .episodeName(data.episodeTitle)
            .build();
        EpisodeInfo[] dummyArray = new EpisodeInfo[1];
        dummyArray[0] = info;
        show.addEpisodes(dummyArray, false);

        episode.listingsComplete();

        return episode.getRenamedBasename();
    }

    /**
     * This is, officially, the Test that checks all the EpisodeTestData, though really it's just
     * a driver method.  All the real work goes on in <code>getReplacementBasename</code>, above.
     *
     * This is the method where the expected and actual values are compared, though.
     */
    @Test
    public void testGetReplacementText() {
        prefs.setMoveEnabled(false);
        prefs.setRenameEnabled(true);
        List<Path> testFiles = new ArrayList<>();
        for (EpisodeTestData data : values ) {
            try {
                Path path = OUR_TEMP_DIR.resolve(data.inputFilename);
                Files.createFile(path);
                testFiles.add(path);

                String replacement = getReplacementBasename(data, path);
                assertEquals(data.expectedReplacement, replacement);
            } catch (Exception e) {
                verboseFail("testing " + data, e);
            }
        }
        teardown(testFiles);
    }

    /**
     * The tests are actually expected to clean up after themselves properly.
     * The <code>teardown</code> method is used for that, and checks things
     * every step of the way.  But if it fails, we still would like to try
     * to leave the world the way we left it.  This is straightforward to do
     * if we stuck with the rule of doing everything inside OUR_TEMP_DIR.
     * Try to basically do an <code>/bin/rm -rf</code> on our temp directory.
     */
    @After
    public void cleanUp() throws Exception {
        if (Files.exists(OUR_TEMP_DIR)) {
            logger.warning("trying to clean up " + OUR_TEMP_DIR);
            try {
                Files.walkFileTree(OUR_TEMP_DIR, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException
                        {
                            FileUtilities.deleteFile(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                            throws IOException
                        {
                            FileUtilities.rmdir(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    });
            } catch (IOException e) {
                verboseFail("unable to clean up leftover directory " + OUR_TEMP_DIR, e);
            }
        }
    }
}
