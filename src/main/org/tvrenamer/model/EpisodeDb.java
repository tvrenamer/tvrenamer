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

    /**
     * Add the given folder to the queue.  This is intended to support the
     * "Add Folder" functionality.  This method itself does only sanity
     * checking, and if everything's in order, calls addFilesRecursively()
     * to do the actual work.
     *
     * @param pathname the name of a folder
     */
    public void addFolderToQueue(final String pathname) {
        if (!prefs.isRecursivelyAddFolders()) {
            logger.warning("cannot add folder when preference \"add files recursively\" is off");
            return;
        }

        if (pathname == null) {
            logger.warning("cannot add files; pathname is null");
            return;
        }

        Queue<FileEpisode> contents = new ConcurrentLinkedQueue<>();
        final Path path = Paths.get(pathname);
        addFilesRecursively(contents, path.getParent(), path.getFileName());
        publish(contents);
    }

    /**
     * Add the given array of filename Strings, each of which are expected to be
     * found within the directory given by the pathPrefix, to the queue.
     * This is intended to support the "Add Files" functionality.
     *
     * @param pathPrefix the directory where the fileNames are found
     * @param fileNames an array of Strings presumed to represent filenames
     */
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

    /**
     * Add the given array of filename Strings to the queue.  This is intended
     * to support Drag and Drop.
     *
     * @param fileNames an array of Strings presumed to represent filenames
     */
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

    /**
     * Add the contents of the preload folder to the queue.
     *
     */
    public void preload() {
        if (prefs.isRecursivelyAddFolders()) {
            String preload = prefs.getPreloadFolder();
            if (preload != null) {
                // TODO: do in separate thread
                addFolderToQueue(preload);
            }
        }
    }

    /**
     * Standard object method to represent this EpisodeDb as a string.
     *
     * @return string version of this; just says how many episodes are in the map.
     */
    @Override
    public String toString() {
        return "{EpisodeDb with " + episodes.size() + " files}";
    }

    private final Queue<AddEpisodeListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Register interest in files and folders that are added to the queue.
     *
     * @param listener
     *    the AddEpisodeListener that should be called when we have finished processing
     *    a folder or array of files
     */
    public void subscribe(AddEpisodeListener listener) {
        listeners.add(listener);
    }

    /**
     * Notify registered interested parties that we've finished adding a folder or
     * array of files to the queue, and pass the queue to each listener.
     *
     * @param episodes
     *    the queue of FileEpisode objects we've created since the last time we
     *    published
     */
    private void publish(Queue<FileEpisode> episodes) {
        for (AddEpisodeListener listener : listeners) {
            listener.addEpisodes(episodes);
        }
    }
}
