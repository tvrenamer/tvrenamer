package org.tvrenamer.model;

import org.tvrenamer.controller.TVRenamer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EpisodeDb {

    private static Logger logger = Logger.getLogger(EpisodeDb.class.getName());

    private final Map<String, FileEpisode> episodes = new ConcurrentHashMap<>(1000);
    private final UserPreferences prefs = UserPreferences.getInstance();

    public void put(String key, FileEpisode value) {
        if (value == null) {
            logger.info("cannot put null value into EpisodeDb!!!");
            return;
        }

        if (key == null) {
            logger.warning("cannot put null key into EpisodeDb!!!");
            return;
        }

        episodes.put(key, value);
    }

    private FileEpisode add(final String pathname) {
        final FileEpisode episode = TVRenamer.parseFilename(pathname);
        if (episode == null) {
            logger.severe("Couldn't parse file: " + pathname);
        } else {
            put(pathname, episode);
        }
        return episode;
    }

    public FileEpisode remove(String key) {
        return episodes.remove(key);
    }

    public boolean remove(String key, FileEpisode value) {
        return episodes.remove(key, value);
    }

    public boolean replaceKey(String oldKey, FileEpisode ep, String newKey) {
        if (ep == null) {
            throw new IllegalStateException("cannot have null value in EpisodeDb!!!");
        }

        if ((oldKey == null) || (oldKey.length() == 0)) {
            throw new IllegalStateException("cannot have null key in EpisodeDb!!!");
        }

        boolean removed = episodes.remove(oldKey, ep);
        if (!removed) {
            throw new IllegalStateException("unrecoverable episode DB corruption");
        }

        FileEpisode oldValue = episodes.put(newKey, ep);
        // The value returned is the *old* value for the key.  We expect it to be
        // null.  If it isn't, that means the new key was already mapped to an
        // episode.  In theory, this could legitimately happen if we rename A to B
        // and B to A, or any longer such cycle.  But that seems extremely unlikely
        // with this particular program, so we'll just warn and do nothing about it.
        if (oldValue != null) {
            logger.warning("removing episode from db due to new episode at that location: "
                           + oldValue);
            return false;
        }
        return true;
    }

    public FileEpisode get(String key) {
        return episodes.get(key);
    }

    private boolean containsKey(String key) {
        return episodes.containsKey(key);
    }

    private void clear() {
        episodes.clear();
    }

    @Override
    public String toString() {
        return "{EpisodeDb with " + episodes.size() + " files}";
    }
}
