package org.tvrenamer.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import org.tvrenamer.model.EpisodeTestData;
import org.tvrenamer.model.FileEpisode;

import java.util.LinkedList;
import java.util.List;

/**
 * There are three major steps to turning a filename into real show information.
 *
 * First is we parse the filename, and attempt to identify the parts of the
 * filename that represent the show name, the season number, the episode
 * number, and possibly the screen resolution.  For the substring that we think
 * identifies the show name, we normalize it somewhat.  We replace punctuation
 * and lower-case the name.
 *
 * The method "testParseFileName" in this file tests that functionality.  Each
 * line of the test input has a filename, and the expected values for show
 * name, season number, episode number, and resolution.  The method parses
 * the filename and verifies the values are as expected.
 *
 * The next step is to take the normalized string and send it to the
 * provider to try to figure out which show this is actually referring to.
 * The provider might return any number of results, including zero.  If
 * it returns more than one, we try to select the right one.
 *
 * Once we have identified the actual show, then we use the season and
 * episode information to look up the actual episode.
 *
 * The method testDownloadAndRename tests the second and third steps.  The
 * static data provided includes the expected episode title, and the test
 * obtains the episode title from the provider and verifies that it is as
 * we expected.
 *
 * (A potential fourth step would be to actually rename the file based on
 * the information from the provider.  Despite the name of the test, this
 * file does not test the actual renaming.)
 */
public class TVRenamerTest {
    private static final List<EpisodeTestData> values = new LinkedList<>();

    @BeforeClass
    public static void setupValuesThreeDigits() {
        // This should be parsed as episode 105, not episode 10
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("The.Daily.Show.S22E105.D.L.Hughley.HDTV.x264")
                   .filenameShow("The.Daily.Show.")
                   .seasonNumString("22")
                   .episodeNumString("105")
                   .build());
    }

    @BeforeClass
    public static void setupValuesNotThreeDigits() {
        // Make sure this is parsed as episode 14, not episode 142
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Futurama.S07E14.2-D Blacktop.HDTV.x264")
                   .filenameShow("Futurama.")
                   .seasonNumString("07")
                   .episodeNumString("14")
                   .build());
    }

    @BeforeClass
    public static void setupValues01() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("game.of.thrones.5x01.mp4")
                   .filenameShow("game.of.thrones.")
                   .seasonNumString("5")
                   .episodeNumString("1")
                   .build());
    }

    @BeforeClass
    public static void setupValues02() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("24.s08.e01.720p.hdtv.x264-immerse.mkv")
                   .filenameShow("24.")
                   .seasonNumString("8")
                   .episodeNumString("1")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues02a() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("MyShows/drama/widescreen/24/8x21.720p.hdtv.x264-immerse.mkv")
                   .filenameShow("24 ")
                   .seasonNumString("8")
                   .episodeNumString("21")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues03() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("24.S07.E18.720p.BlueRay.x264-SiNNERS.mkv")
                   .filenameShow("24.")
                   .seasonNumString("7")
                   .episodeNumString("18")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues04() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("human.target.2010.s01.e02.720p.hdtv.x264-2hd.mkv")
                   .filenameShow("human.target.2010.")
                   .seasonNumString("1")
                   .episodeNumString("2")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues05() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("dexter.407.720p.hdtv.x264-sys.mkv")
                   .filenameShow("dexter.")
                   .seasonNumString("4")
                   .episodeNumString("7")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues06() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("JAG.S10E01.DVDRip.XviD-P0W4DVD.avi")
                   .filenameShow("JAG.")
                   .seasonNumString("10")
                   .episodeNumString("1")
                   .build());
    }

    @BeforeClass
    public static void setupValues07() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Lost.S06E05.Lighthouse.DD51.720p.WEB-DL.AVC-FUSiON.mkv")
                   .filenameShow("Lost.")
                   .seasonNumString("6")
                   .episodeNumString("5")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues08() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("warehouse.13.s1e01.720p.hdtv.x264-dimension.mkv")
                   .filenameShow("warehouse.13.")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues09() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("one.tree.hill.s07e14.hdtv.xvid-fqm.avi")
                   .filenameShow("one.tree.hill.")
                   .seasonNumString("7")
                   .episodeNumString("14")
                   .build());
    }

    @BeforeClass
    public static void setupValues10() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("gossip.girl.s03e15.hdtv.xvid-fqm.avi")
                   .filenameShow("gossip.girl.")
                   .seasonNumString("3")
                   .episodeNumString("15")
                   .build());
    }

    @BeforeClass
    public static void setupValues11() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("smallville.s09e14.hdtv.xvid-xii.avi")
                   .filenameShow("smallville.")
                   .seasonNumString("9")
                   .episodeNumString("14")
                   .build());
    }

    @BeforeClass
    public static void setupValues12() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("smallville.s09e15.hdtv.xvid-2hd.avi")
                   .filenameShow("smallville.")
                   .seasonNumString("9")
                   .episodeNumString("15")
                   .build());
    }

    @BeforeClass
    public static void setupValues13() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("the.big.bang.theory.s03e18.720p.hdtv.x264-ctu.mkv")
                   .filenameShow("the.big.bang.theory.")
                   .seasonNumString("3")
                   .episodeNumString("18")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues14() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("castle.2009.s01e09.720p.hdtv.x264-ctu.mkv")
                   .filenameShow("castle.2009.")
                   .seasonNumString("1")
                   .episodeNumString("9")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues15() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("/TV/Dexter/S05E05 First Blood.mkv")
                   .filenameShow("Dexter ")
                   .seasonNumString("5")
                   .episodeNumString("5")
                   .build());
    }

    @BeforeClass
    public static void setupValues16() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("/TV/Lost/Lost [2x07].mkv")
                   .filenameShow("Lost [")
                   .seasonNumString("2")
                   .episodeNumString("7")
                   .build());
    }

    @BeforeClass
    public static void setupValues161() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("/TV/Lost/2x07.mkv")
                   .filenameShow("Lost ")
                   .seasonNumString("2")
                   .episodeNumString("7")
                   .build());
    }

    @BeforeClass
    public static void setupValues17() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("American.Dad.S09E17.HDTV.x264-2HD.mp4")
                   .filenameShow("American.Dad.")
                   .seasonNumString("9")
                   .episodeNumString("17")
                   .build());
    }

    @BeforeClass
    public static void setupValues18() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Californication.S07E04.HDTV.x264-2HD.mp4")
                   .filenameShow("Californication.")
                   .seasonNumString("7")
                   .episodeNumString("4")
                   .build());
    }

    @BeforeClass
    public static void setupValues19() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Continuum.S03E07.HDTV.x264-2HD.mp4")
                   .filenameShow("Continuum.")
                   .seasonNumString("3")
                   .episodeNumString("7")
                   .build());
    }

    @BeforeClass
    public static void setupValues20() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Elementary.S02E23.HDTV.x264-LOL.mp4")
                   .filenameShow("Elementary.")
                   .seasonNumString("2")
                   .episodeNumString("23")
                   .build());
    }

    @BeforeClass
    public static void setupValues21() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Family.Guy.S12E19.HDTV.x264-2HD.mp4")
                   .filenameShow("Family.Guy.")
                   .seasonNumString("12")
                   .episodeNumString("19")
                   .build());
    }

    @BeforeClass
    public static void setupValues22() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Fargo.S01E01.HDTV.x264-2HD.mp4")
                   .filenameShow("Fargo.")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .build());
    }

    @BeforeClass
    public static void setupValues23() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Girls.S03E11.HDTV.x264-KILLERS.mp4")
                   .filenameShow("Girls.")
                   .seasonNumString("3")
                   .episodeNumString("11")
                   .build());
    }

    @BeforeClass
    public static void setupValues24() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Grimm.S03E19.HDTV.x264-LOL.mp4")
                   .filenameShow("Grimm.")
                   .seasonNumString("3")
                   .episodeNumString("19")
                   .build());
    }

    @BeforeClass
    public static void setupValues25() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("House.Of.Cards.2013.S01E06.HDTV.x264-EVOLVE.mp4")
                   .filenameShow("House.Of.Cards.2013.")
                   .seasonNumString("1")
                   .episodeNumString("6")
                   .build());
    }

    @BeforeClass
    public static void setupValues26() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Modern.Family.S05E12.HDTV.x264-EXCELLENCE.mp4")
                   .filenameShow("Modern.Family.")
                   .seasonNumString("5")
                   .episodeNumString("12")
                   .build());
    }

    @BeforeClass
    public static void setupValues27() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("New.Girl.S03E23.HDTV.x264-LOL.mp4")
                   .filenameShow("New.Girl.")
                   .seasonNumString("3")
                   .episodeNumString("23")
                   .build());
    }

    @BeforeClass
    public static void setupValues28() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Nurse.Jackie.S06E04.HDTV.x264-2HD.mp4")
                   .filenameShow("Nurse.Jackie.")
                   .seasonNumString("6")
                   .episodeNumString("4")
                   .build());
    }

    @BeforeClass
    public static void setupValues29() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Offspring - S05E01.mp4")
                   .filenameShow("Offspring - ")
                   .seasonNumString("5")
                   .episodeNumString("1")
                   .build());
    }

    @BeforeClass
    public static void setupValues30() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Reign.2013.S01E20.HDTV.x264-2HD.mp4")
                   .filenameShow("Reign.2013.")
                   .seasonNumString("1")
                   .episodeNumString("20")
                   .build());
    }

    @BeforeClass
    public static void setupValues31() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Robot.Chicken.S07E04.PROPER.HDTV.x264-W4F.mp4")
                   .filenameShow("Robot.Chicken.")
                   .seasonNumString("7")
                   .episodeNumString("4")
                   .build());
    }

    @BeforeClass
    public static void setupValues32() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Supernatural.S09E21.HDTV.x264-LOL.mp4")
                   .filenameShow("Supernatural.")
                   .seasonNumString("9")
                   .episodeNumString("21")
                   .build());
    }

    @BeforeClass
    public static void setupValues33() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("The.Americans.2013.S02E10.HDTV.x264-LOL.mp4")
                   .filenameShow("The.Americans.2013.")
                   .seasonNumString("2")
                   .episodeNumString("10")
                   .build());
    }

    @BeforeClass
    public static void setupValues34() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("The.Big.Bang.Theory.S07E23.HDTV.x264-LOL.mp4")
                   .filenameShow("The.Big.Bang.Theory.")
                   .seasonNumString("7")
                   .episodeNumString("23")
                   .build());
    }

    @BeforeClass
    public static void setupValues35() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("The.Good.Wife.S05E20.HDTV.x264-LOL.mp4")
                   .filenameShow("The.Good.Wife.")
                   .seasonNumString("5")
                   .episodeNumString("20")
                   .build());
    }

    @BeforeClass
    public static void setupValues36() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("The.Walking.Dead.S04E16.PROPER.HDTV.x264-2HD.mp4")
                   .filenameShow("The.Walking.Dead.")
                   .seasonNumString("4")
                   .episodeNumString("16")
                   .build());
    }

    @BeforeClass
    public static void setupValues37() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Veep.S03E05.HDTV.x264-KILLERS.mp4")
                   .filenameShow("Veep.")
                   .seasonNumString("3")
                   .episodeNumString("5")
                   .build());
    }

    @BeforeClass
    public static void setupValues38() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Witches.of.East.End.S01E01.PROPER.HDTV.x264-2HD.mp4")
                   .filenameShow("Witches.of.East.End.")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .build());
    }

    @BeforeClass
    public static void setupValues39() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Warehouse.13.S05E04.HDTV.x264-2HD.mp4")
                   .filenameShow("Warehouse.13.")
                   .seasonNumString("5")
                   .episodeNumString("4")
                   .build());
    }

    @BeforeClass
    public static void setupValues40() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("the.100.208.hdtv-lol.mp4")
                   .filenameShow("the.100.")
                   .seasonNumString("2")
                   .episodeNumString("8")
                   .build());
    }

    @BeforeClass
    public static void setupValues41() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x01.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .build());
    }

    @BeforeClass
    public static void setupValues42() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x02.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("2")
                   .build());
    }

    @BeforeClass
    public static void setupValues43() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x03.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("3")
                   .build());
    }

    @BeforeClass
    public static void setupValues44() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x04.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("4")
                   .build());
    }

    @BeforeClass
    public static void setupValues45() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x05.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("5")
                   .build());
    }

    @BeforeClass
    public static void setupValues46() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x06.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("6")
                   .build());
    }

    @BeforeClass
    public static void setupValues47() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x07.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("7")
                   .build());
    }

    @BeforeClass
    public static void setupValues48() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x08.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("8")
                   .build());
    }

    @BeforeClass
    public static void setupValues49() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x09.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("9")
                   .build());
    }

    @BeforeClass
    public static void setupValues50() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x10.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("10")
                   .build());
    }

    @BeforeClass
    public static void setupValues51() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x11.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("11")
                   .build());
    }

    @BeforeClass
    public static void setupValues52() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x12.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("12")
                   .build());
    }

    @BeforeClass
    public static void setupValues53() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x13.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("13")
                   .build());
    }

    @BeforeClass
    public static void setupValues54() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("firefly.1x14.hdtv-lol.mp4")
                   .filenameShow("firefly.")
                   .seasonNumString("1")
                   .episodeNumString("14")
                   .build());
    }

    @BeforeClass
    public static void setupValues55() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Strike.Back.S01E01.Mini.720p.HDTV.DD5.1.x264.mkv")
                   .filenameShow("Strike.Back.")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues56() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("law.and.order.svu.1705.hdtv-lol")
                   .filenameShow("law.and.order.svu.")
                   .seasonNumString("17")
                   .episodeNumString("05")
                   .build());
    }

    @BeforeClass
    public static void setupValues57() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("ncis.1304.hdtv-lol")
                   .filenameShow("ncis.")
                   .seasonNumString("13")
                   .episodeNumString("04")
                   .build());
    }

    @BeforeClass
    public static void setupValues58() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Marvels.Agents.of.S.H.I.E.L.D.S03E03.HDTV.x264-FLEET")
                   .filenameShow("Marvels.Agents.of.S.H.I.E.L.D.")
                   .seasonNumString("3")
                   .episodeNumString("3")
                   .build());
    }

    @BeforeClass
    public static void setupValues59() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Marvels.Agents.of.S.H.I.E.L.D.S03E10.HDTV.x264-KILLERS")
                   .filenameShow("Marvels.Agents.of.S.H.I.E.L.D.")
                   .seasonNumString("3")
                   .episodeNumString("10")
                   .build());
    }

    @BeforeClass
    public static void setupValues60() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Nip.Tuck.S06E01.720p.HDTV.X264-DIMENSION.mkv")
                   .filenameShow("Nip.Tuck.")
                   .seasonNumString("6")
                   .episodeNumString("1")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues61() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("The.Big.Bang.Theory.S10E04.720p.HDTV.X264-DIMENSION[ettv].mkv")
                   .filenameShow("The.Big.Bang.Theory.")
                   .seasonNumString("10")
                   .episodeNumString("4")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues62() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Lucifer.S02E03.720p.HDTV.X264-DIMENSION[ettv].mkv")
                   .filenameShow("Lucifer.")
                   .seasonNumString("2")
                   .episodeNumString("3")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues63() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Marvels.Agents.of.S.H.I.E.L.D.S04E03.1080p.HDTV.x264-KILLERS[ettv].mkv")
                   .filenameShow("Marvels.Agents.of.S.H.I.E.L.D.")
                   .seasonNumString("4")
                   .episodeNumString("3")
                   .episodeResolution("1080p")
                   .build());
    }

    @BeforeClass
    public static void setupValues64() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Supernatural.S11E22.1080p.HDTV.X264-DIMENSION[ettv].mkv")
                   .filenameShow("Supernatural.")
                   .seasonNumString("11")
                   .episodeNumString("22")
                   .episodeResolution("1080p")
                   .build());
    }

    @BeforeClass
    public static void setupValues65() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Supernatural.S11E22.HDTV.X264-DIMENSION.720p.[ettv].mkv")
                   .filenameShow("Supernatural.")
                   .seasonNumString("11")
                   .episodeNumString("22")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues66() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Channel.Zero.S01E01.480p.HDTV.X264-DIMENSION[ettv].mkv")
                   .filenameShow("Channel.Zero.")
                   .seasonNumString("1")
                   .episodeNumString("1")
                   .episodeResolution("480p")
                   .build());
    }

    @BeforeClass
    public static void setupValues67() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("NCIS.S14E04.720p.HDTV.X264-DIMENSION[ettv].mkv")
                   .filenameShow("NCIS.")
                   .seasonNumString("14")
                   .episodeNumString("4")
                   .episodeResolution("720p")
                   .build());
    }

    @BeforeClass
    public static void setupValues68() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets.S01E02.Quintagious.avi")
                   .filenameShow("Quintuplets.")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues69() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/S01E02.Quintagious.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues70() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/versions/S01E02.Quintagious.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues71() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/versions/S01E02.Quintagious~2.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues72() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/Season1/versions/S01E02.Quintagious~9.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues73() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/Season01/versions/S01E02.Quintagious~4.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues74() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets.Season01/S01E02.Quintagious.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues75() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/s01/1x02.Quintagious.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("1")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues76() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/01x02.Quintagious.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues77() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Videos/TVShows/Fullscreen/LiveAction/Quintuplets/"
                                  + "Season01/S01E02.Quintagious.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @BeforeClass
    public static void setupValues78() {
        values.add(new EpisodeTestData.Builder()
                   .inputFilename("Quintuplets/Quintuplets.Season01/s01/"
                                  + "versions/season1/S01E02.Quintagious~7.avi")
                   .filenameShow("Quintuplets ")
                   .seasonNumString("01")
                   .episodeNumString("02")
                   .build());
    }

    @Test
    public void testParseFileName() {
        for (EpisodeTestData testInput : values) {
            String input = testInput.inputFilename;
            FileEpisode retval = new FileEpisode(input);
            TVRenamer.parseFilename(retval);
            assertTrue(input, retval.wasParsed());
            assertEquals(input, testInput.filenameShow, retval.getFilenameShow());
            assertEquals(input, Integer.parseInt(testInput.seasonNumString), retval.getSeasonNum());
            assertEquals(input, Integer.parseInt(testInput.episodeNumString), retval.getEpisodeNum());
            assertEquals(input, testInput.episodeResolution, retval.getFilenameResolution());
        }
    }
}
