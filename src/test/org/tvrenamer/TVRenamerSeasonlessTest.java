package org.tvrenamer;

import org.junit.BeforeClass;
import org.junit.Test;
import org.tvrenamer.TestInput;
import org.tvrenamer.controller.ShowInformationListener;
import org.tvrenamer.controller.TVRenamerSeasonless;
import org.tvrenamer.model.*;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Created by lsrom on 11/2/16.
 */
public class TVRenamerSeasonlessTest {
    public static final List<TestInput> values = new LinkedList<>();

    @BeforeClass
    public static void setupValues() {
        // make sure absolute numbering works
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 001 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "1", "The Peace Prize. Who'll Get the 100 Million Zeny!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 002 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "2", "To the Promised Resort! Vegeta's Family Vacation?!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 003 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "3", "Where's the Rest of My Dream?! Find the Super Saiyan God!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 013 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "13", "Goku, Go Surpass Super Saiyan God!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 014 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "14", "This is Every Last Bit of My Power! The Battle of Gods Concludes!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 015 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "15", "Heroic Satan, Cause a Miracle! A Challenge From Outer Space!!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 016 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "16", "Vegeta Becomes a Pupil?! Take Down Whis!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 049 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "49", "A Message from the Future - Goku Black Strikes!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 050 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "50", "Goku Vs Black! The Closed Path to the Future", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 051 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "51", "Feelings That Travel Beyond Time - Trunks and Mai", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 052 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "52", "Teacher and Student Reunited - Son Gohan and \"Future\" Trunks", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 053 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "53", "Uncover Black's Identity! To the Sacred World of the Kais!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 054 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "54", "The One Who Inherits the Saiyan Blood - Trunks' Resolve", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 055 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "55", "I Want to See Son Goku - Zen‐Oh Sama's Summoning!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 056 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "56", "A Rematch with Goku Black! Enter Super Saiyan Rosé", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 057 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "57", "The Birth of the Immortal God, Zamasu!", "1080p"));
        values.add(new TestInput("[Batman] Dragon Ball Super - 058  720p Eng Dub x264.mkv", "Dragon Ball Super", "0", "58", "The Secret of Black and Zamasu", "720p"));
        values.add(new TestInput("Dragon Ball Super - 059 Eng Sub x264.mp4", "Dragon Ball Super", "0", "59", "Protect Kaiō-shin Gowasu - Destroy Zamasu!", ""));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 060 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "60", "Back to the Future - Goku Black's True Identity Revealed!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 061 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "61", "Zamasu's Ambition — Presenting the 'Zero Mortals Plan'", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 062 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "62", "I Will Protect the World! Trunks's Furious Super Power Explodes!!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 063 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "63", "Don't Defile the Saiyan Cells! The Curtain Rises on Vegeta's Battle!!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 064 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "64", "Revere Him! Praise Him! Fusion Zamasu's Explosive Birth!!", "1080p"));
        values.add(new TestInput("[AnimeRG] Dragon Ball Super - 065 [1080p] [x265] [pseudo].mkv", "Dragon Ball Super", "0", "65", "Final Judgment?! The Supreme God's Ultimate Power", "1080p"));

        values.add(new TestInput("[PA]-Dragon-Ball-Z-001-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "1", "The New Threat", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-002-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "2", "Reunions", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-003-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "3", "Unlikely Alliance", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-004-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "4", "Piccolo's Plan", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-005-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "5", "Gohan's Rage", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-006-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "6", "No Time Like the Present", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-007-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "7", "Day 1", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-008-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "8", "Gohan Goes Bananas!", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-009-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "9", "The Strangest Robot", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-010-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "10", "A New Friend", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-011-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "11", "Terror on Arlia", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-012-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "12", "Global Training", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-013-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "13", "Goz and Mez", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-014-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "14", "Princess Snake", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-015-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "15", "Dueling Piccolos", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-016-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "16", "Plight of the Children", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-017-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "17", "Pendulum Room Peril", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-018-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "18", "The End of Snake Way", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-019-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "19", "Defying Gravity", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-020-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "20", "Goku's Ancestors", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-021-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "21", "Counting Down", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-022-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "22", "The Darkest Day", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-023-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "23", "Saibamen Attack!", "1080p"));
        values.add(new TestInput("[PA]-Dragon-Ball-Z-024-[1080p][Hi8b][3849F7F5][V2]-(1).mkv", "Dragon Ball Z", "0", "24", "The Power of Nappa", "1080p"));

        values.add(new TestInput("One-Punch-Man 001 [1080p].mkv", "One Punch Man", "0", "1", "The Strongest Man", "1080p"));
        values.add(new TestInput("One-Punch-Man 002 [1080p].mkv", "One Punch Man", "0", "2", "The Lone Cyborg", "1080p"));
        values.add(new TestInput("One-Punch-Man 003 [1080p].mkv", "One Punch Man", "0", "3", "The Obsessive Scientist", "1080p"));
        values.add(new TestInput("One-Punch-Man 004 [1080p].mkv", "One Punch Man", "0", "4", "The Modern Ninja", "1080p"));
        values.add(new TestInput("One-Punch-Man 005 [1080p].mkv", "One Punch Man", "0", "5", "The Ultimate Mentor", "1080p"));
        values.add(new TestInput("One-Punch-Man 006 [1080p].mkv", "One Punch Man", "0", "6", "The Terrifying City", "1080p"));
        values.add(new TestInput("One-Punch-Man 007 [1080p].mkv", "One Punch Man", "0", "7", "The Ultimate Disciple", "1080p"));
        values.add(new TestInput("One-Punch-Man 008 [1080p].mkv", "One Punch Man", "0", "8", "The Deep Sea King", "1080p"));
        values.add(new TestInput("One-Punch-Man 009 [1080p].mkv", "One Punch Man", "0", "9", "Unyielding Justice", "1080p"));
        values.add(new TestInput("One-Punch-Man 010 [1080p].mkv", "One Punch Man", "0", "10", "Unparalleled Peril", "1080p"));
        values.add(new TestInput("One-Punch-Man 011 [1080p].mkv", "One Punch Man", "0", "11", "The Dominator of the Universe", "1080p"));
        values.add(new TestInput("One-Punch-Man 012 [1080p].mkv", "One Punch Man", "0", "12", "The Strongest Hero", "1080p"));

        values.add(new TestInput("[a-s]_dragon_ball_-_056_-_strange_visitor__rs2_[4276a3cf].mkv", "Dragon Ball", "0", "56", "Strange Visitor", ""));
        values.add(new TestInput("[a-s]_dragon_ball_-_057_-_arale_vs._blue__rs2_[720p].mkv", "Dragon Ball", "0", "57", "Arale vs. Blue", "720p"));
        values.add(new TestInput("[a-s]_dragon_ball_-_062v2_-_sacred_water__rs2_[4k].mkv", "Dragon Ball", "0", "62", "Sacred Water", "4k"));
        values.add(new TestInput("[a-s]_dragon_ball_-_064_-_the_last_of_mercenary_tao__rs2_[d21abfdb].mkv", "Dragon Ball", "0", "64", "The Last of Mercenary Tao", ""));
        values.add(new TestInput("[a-s]_dragon_ball_-_067_-_the_end_of_commander_red__rs2_[35f58d3d].mkv", "Dragon Ball", "0", "67", "The End of Commander Red", ""));
        values.add(new TestInput("[a-s]_dragon_ball_-_070_-_we_are_the_five_warriors__rs2_[1080p].mkv", "Dragon Ball", "0", "70", "We Are The Five Warriors", "1080p"));
        values.add(new TestInput("[a-s]_dragon_ball_-_079_-_terror_and_plague__rs2_[cace5d7c].mkv", "Dragon Ball", "0", "79", "Terror and Plague", ""));
        values.add(new TestInput("[a-s]_dragon_ball_-_081_-_goku_goes_to_demon_land__rs2_[589278c2].mkv", "Dragon Ball", "0", "81", "Goku Goes to Demon Land", ""));
    }

    @Test
    public void testParseFileName(){
        for (TestInput testInput : values){
            FileEpisode retval = TVRenamerSeasonless.parseFilename(testInput.input);
            assertNotNull(retval);
            assertEquals(testInput.input, testInput.show, retval.getShowName());
            assertEquals(testInput.input, Integer.parseInt(testInput.season), retval.getSeasonNumber());
            assertEquals(testInput.input, Integer.parseInt(testInput.episode), retval.getEpisodeNumber());
        }
    }

    @Test
    public void testDownloadAndRename() {
        try {
            for (TestInput testInput : values) {
                if (testInput.episodeTitle != null) {
                    final FileEpisode fileEpisode = TVRenamerSeasonless.parseFilename(testInput.input);
                    assertNotNull(fileEpisode);
                    String showName = fileEpisode.getShowName();

                    final CompletableFuture<String> future = new CompletableFuture<>();
                    ShowStore.getShow(showName, new ShowInformationListener() {
                        @Override
                        public void downloaded(Show show) {
                            String titleString = "__not_initialized__";

                            boolean stop = false;
                            for (int i = 0; i < show.getSeasonNum(); i++) {
                                if (i == 0) {
                                    continue;
                                }
                                if (stop) {
                                    break;
                                }

                                Season s = show.getSeason(i);

                                Map<Integer, Episode> episodes = s.getEpisodes();
                                Iterator it = episodes.entrySet().iterator();
                                int episodeNumber = Integer.parseInt(testInput.episode);

                                while (it.hasNext()) {
                                    Map.Entry pair = (Map.Entry) it.next();

                                    if (((Episode) pair.getValue()).getEpNumAbs() == episodeNumber) {
                                        titleString = s.getTitle((Integer) pair.getKey());
                                        stop = true;
                                        break;
                                    }
                                    it.remove(); // avoids a ConcurrentModificationException
                                }
                            }

                            future.complete(titleString);
                        }

                        @Override
                        public void downloadFailed(Show show) {
                            fail();
                        }
                    });

                    String got = future.get();
                    assertEquals(testInput.episodeTitle, got);
                }
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
