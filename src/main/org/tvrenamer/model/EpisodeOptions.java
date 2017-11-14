package org.tvrenamer.model;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * It is useful to remember the context of what this program does, and how there is a slight
 * mismatch against the model of TheTvDb.
 *
 * What TheTvDb does is more natural.  Given a particular episode of the series, where does it
 * fit in the ordering?  And TheTvDb may give two answers.  One is where it was originally aired.
 * The other is where it appears in the DVD releases.  There frequently is a difference.
 *
 * Let's look at a couple of episodes of Futurama as an example.
 *
 * Title                    Aired      DVD
 * =====                    ======     ======
 * A Leela of Her Own       S04E10     S03E16
 * The Why of Fry           S05E08     S04E10
 *
 * So, if you know you have "A Leela of Her Own", it could be either S04E10 or S03E16, depending
 * on what ordering you want to use.
 *
 * But this program works a bit differently.  We look at the filename, and extract the season and
 * episode information.  Based on that information, we decide which episode it is, so we can
 * add the episode title to the filename.  So, the question is not, "Where does 'The Why of Fry'
 * fit in?".  It is, "What is S04E10 of Futurama?"  And as we can see, there can be two answers,
 * depending on which ordering is being used.
 *
 * Theoretically, there could be more than two answers.  Even for a given ordering, the DB could
 * potentially assign the same season number and episode number to two distinct episodes.
 * It probably SHOULDN'T ever happen, but we're prepared to handle it if it does.
 *
 * We use a class called EpisodeOptions which ties together an episode and an ordering.
 * The EpisodeNumber objects are indexed into the Season; each index has a list.  So, using the
 * Futurama example, if we just added those two episodes, we'd have:
 *    Season 3:  16: {DVD: A Leela of Her Own}
 *    Season 4:  10: {DVD: The Why of Fry}, {AIR: A Leela of Her Own}
 *    Season 5:   8: {AIR: The Why of Fry}
 *
 */
class EpisodeOptions {

    private static final class EpisodeNumber {
        final boolean isDvd;
        final Episode episode;

        EpisodeNumber(boolean isDvd, Episode episode) {
            this.isDvd = isDvd;
            this.episode = episode;
        }

        @Override
        public String toString() {
            return (isDvd ? "DVD: " : "air: ") + episode.getTitle();
        }
    }

    private final List<EpisodeNumber> episodeList = new LinkedList<>();

    /**
     *
     * @param isDvd
     *           whether or not the placement is the DVD ordering
     * @param episode
     *           the episode to add at the given index
     */
    public void addEpisode(boolean isDvd, Episode episode) {
        episodeList.add(new EpisodeNumber(isDvd, episode));
    }

    /**
     * Look up an episode in this season in the given ordering.
     *
     * @param preferDvd
     *           whether the caller prefers the DVD ordering, or over-the-air ordering
     * @return the Episode that best matches the request criteria, or null if none does
     */
    public Episode get(boolean preferDvd) {
        if (episodeList.size() == 0) {
            return null;
        }

        return episodeList.stream()
            .filter(ep -> ep.isDvd == preferDvd)
            .findFirst()
            .orElse(episodeList.get(0))
            .episode;
    }

    /**
     * Standard object method to represent this EpisodeOptions as a string.
     *
     * @return string version of this
     */
    @Override
    public String toString() {
        StringBuilder rep = new StringBuilder();
        rep.append("[");
        int n = episodeList.size();
        if (n > 0) {
            rep.append(episodeList.get(0));
            if (n > 1) {
                for (int i=1; i<n; i++) {
                    rep.append("\n                ");
                    rep.append(episodeList.get(i));
                }
            }
        }
        rep.append("]");
        return rep.toString();
    }
}
