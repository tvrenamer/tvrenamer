package org.tvrenamer.model;

import org.tvrenamer.controller.AddEpisodeListener;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class EpisodeDb {

    private static final Logger logger = Logger.getLogger(EpisodeDb.class.getName());

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
        Path path = Paths.get(pathname);
        final FileEpisode episode = new FileEpisode(path);
        if (!episode.wasParsed()) {
            // TODO: we can add these episodes to the table anyway,
            // to provide information to the user, and in the future,
            // to let them help us parse the filenames.
            logger.severe("Couldn't parse file: " + pathname);
            return null;
        }
        put(pathname, episode);
        return episode;
    }

    public FileEpisode remove(String key) {
        // This is called when the user removes a row from the table.
        // It's possible (even if unlikely) that the user might delete
        // the entry, only to re-add it later.  And this works fine.
        // But it does cause us to recreate the FileEpisode from scratch.
        // It might be nice to put removed episodes "aside" somewhere that
        // we could still find them, but just know they're not actively
        // in the table.
        return episodes.remove(key);
    }

    @SuppressWarnings("unused")
    public boolean remove(String key, FileEpisode value) {
        return episodes.remove(key, value);
    }

    @SuppressWarnings("unused")
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

    private boolean fileIsVisible(Path path) {
        boolean isVisible = false;
        try {
            if (Files.exists(path)) {
                if (Files.isHidden(path)) {
                    logger.finer("ignoring hidden file " + path);
                } else {
                    isVisible = true;
                }
            }
        } catch (IOException | SecurityException e) {
            logger.finer("could not access file; treating as hidden: " + path);
        }
        return isVisible;
    }

    private void addFileToQueue(final Queue<FileEpisode> contents,
                                final Path path)
    {
        final Path absPath = path.toAbsolutePath();
        final String key = absPath.toString();
        if (episodes.containsKey(key)) {
            logger.info("already in table: " + key);
        } else {
            FileEpisode ep = add(key);
            if (ep != null) {
                contents.add(ep);
            }
        }
    }

    private void addFileIfVisible(final Queue<FileEpisode> contents,
                                  final Path path)
    {
        if (fileIsVisible(path) && Files.isRegularFile(path)) {
            addFileToQueue(contents, path);
        }
    }

    private void addFilesRecursively(final Queue<FileEpisode> contents,
                                     final Path parent,
                                     final Path filename)
    {
        if (parent == null) {
            logger.warning("cannot add files; parent is null");
            return;
        }
        if (filename == null) {
            logger.warning("cannot add files; filename is null");
            return;
        }
        final Path fullpath = parent.resolve(filename);
        if (fileIsVisible(fullpath)) {
            if (Files.isDirectory(fullpath)) {
                try (DirectoryStream<Path> files = Files.newDirectoryStream(fullpath)) {
                    if (files != null) {
                        // recursive call
                        files.forEach(pth -> addFilesRecursively(contents,
                                                                 fullpath,
                                                                 pth.getFileName()));
                    }
                } catch (IOException ioe) {
                    logger.warning("IO Exception descending " + fullpath);
                }
            } else {
                addFileToQueue(contents, fullpath);
            }
        }
    }

    public void addFolderToQueue(final String pathname) {
        Queue<FileEpisode> contents = new ConcurrentLinkedQueue<>();
        final Path path = Paths.get(pathname);
        if (prefs.isRecursivelyAddFolders()) {
            addFilesRecursively(contents, path.getParent(), path.getFileName());
            publish(contents);
        } else {
            logger.warning("cannot add folder when preference \"add files recursively\" is off");
        }
    }

    public void addFilesToQueue(final String pathPrefix, String[] fileNames) {
        Queue<FileEpisode> contents = new ConcurrentLinkedQueue<>();
        if (pathPrefix != null) {
            Path path = Paths.get(pathPrefix);
            Path parent = path.getParent();

            for (String fileName : fileNames) {
                path = parent.resolve(fileName);
                addFileIfVisible(contents, path);
            }
            publish(contents);
        }
    }

    public void addArrayOfStringsToQueue(final String[] fileNames) {
        Queue<FileEpisode> contents = new ConcurrentLinkedQueue<>();
        boolean descend = prefs.isRecursivelyAddFolders();
        for (final String fileName : fileNames) {
            final Path path = Paths.get(fileName);
            if (descend) {
                addFilesRecursively(contents, path.getParent(), path.getFileName());
            } else {
                addFileIfVisible(contents, path);
            }
        }
        publish(contents);
    }

    @Override
    public String toString() {
        return "{EpisodeDb with " + episodes.size() + " files}";
    }

    public void preload() {
        if (prefs.isRecursivelyAddFolders()) {
            String preload = prefs.getPreloadFolder();
            if (preload != null) {
                // TODO: do in separate thread
                addFolderToQueue(preload);
            }
        }
    }

    private final Queue<AddEpisodeListener> listeners = new ConcurrentLinkedQueue<>();

    public void subscribe(AddEpisodeListener listener) {
        listeners.add(listener);
    }

    private void publish(Queue<FileEpisode> episodes) {
        for (AddEpisodeListener listener : listeners) {
            listener.addEpisodes(episodes);
        }
    }
}
