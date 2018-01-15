package org.tvrenamer.model;

import org.tvrenamer.controller.AddEpisodeListener;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

public class EpisodeDb implements Observer {

    private static final Logger logger = Logger.getLogger(EpisodeDb.class.getName());

    private final Map<String, FileEpisode> episodes = new ConcurrentHashMap<>(1000);
    private final UserPreferences prefs = UserPreferences.getInstance();
    private List<String> ignoreKeywords = prefs.getIgnoreKeywords();

    public EpisodeDb() {
        prefs.addObserver(this);
    }

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

    private String ignorableReason(String fileName) {
        for (String ignoreKeyword : ignoreKeywords) {
            if (fileName.contains(ignoreKeyword)) {
                return ignoreKeyword;
            }
        }
        return null;
    }

    private FileEpisode createFileEpisode(final Path path) {
        final FileEpisode episode = new FileEpisode(path);
        episode.setIgnoreReason(ignorableReason(path.toString()));
        if (!episode.wasParsed()) {
            logger.warning("Couldn't parse file: " + path);
        }
        return episode;
    }

    private FileEpisode createFileEpisode(final Folder parent,
                                          final Path fileName)
    {
        Path path = parent.resolve(fileName);
        final FileEpisode episode = new FileEpisode(path);
        episode.setIgnoreReason(ignorableReason(fileName.toString()));
        if (!episode.wasParsed()) {
            // We're putting the episode in the table anyway, but it's
            // not much use.  TODO: make better use of it.
            logger.warning("Couldn't parse file: " + fileName);
        }
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
                                final Folder parent,
                                final Path fileName,
                                final Path realpath)
    {
        final String key = realpath.toString();
        if (episodes.containsKey(key)) {
            logger.info("already in table: " + key);
        } else {
            FileEpisode ep = createFileEpisode(parent, fileName);
            put(key, ep);
            contents.add(ep);
        }
    }

    private void addFileToQueue(final Queue<FileEpisode> contents,
                                final Path realpath)
    {
        final String key = realpath.toString();
        if (episodes.containsKey(key)) {
            logger.info("already in table: " + key);
        } else {
            FileEpisode ep = createFileEpisode(realpath);
            put(key, ep);
            contents.add(ep);
        }
    }

    private void addFileIfVisible(final Queue<FileEpisode> contents,
                                  final Path path)
    {
        if (fileIsVisible(path) && Files.isRegularFile(path)) {
            final Folder root = Folder.getFolder(path.getRoot());
            final Path subpath = root.relativize(path);
            logger.finer("for " + path + " adding as " + root + ", " + subpath);
            addFileToQueue(contents, root, subpath, path);
        }
    }

    private void addFilesRecursively(final Queue<FileEpisode> contents,
                                     final Folder parent,
                                     final Path fileName)
    {
        final Path resolved = parent.resolve(fileName);

        Path realpath = null;
        try {
            realpath = resolved.toRealPath();
        } catch (IOException ioe) {
            if (Files.notExists(resolved)) {
                logger.warning("will not add nonexistent file: " + resolved);
                return;
            }
            logger.warning("skipping " + parent + " / " + fileName
                           + " due to I/O exception");
            return;
        }

        if (!fileIsVisible(realpath)) {
            return;
        }

        if (Files.isDirectory(realpath)) {
            final Folder folder = Folder.getFolder(realpath);
            try (DirectoryStream<Path> files = Files.newDirectoryStream(realpath)) {
                if (files != null) {
                    // recursive call
                    files.forEach(p -> addFilesRecursively(contents, folder,
                                                           p.getFileName()));
                }
            } catch (IOException ioe) {
                logger.warning("IO Exception descending " + realpath);
            }
        } else {
            logger.finer("adding file from " + parent + ":\n  " + fileName);
            addFileToQueue(contents, parent, fileName, realpath);
        }
    }

    /**
     * Recursively add the given folder to the queue.
     *
     * @param folder
     *     an existing folder
     */
    public void addRealPathFolderToQueue(final Folder folder) {
        Queue<FileEpisode> contents = new ConcurrentLinkedQueue<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder.toPath())) {
            if (files != null) {
                files.forEach(p -> addFilesRecursively(contents, folder,
                                                       p.getFileName()));
            }
        } catch (IOException ioe) {
            logger.warning("IO Exception descending " + folder);
        }
        publish(contents);
    }

    /**
     * Add the given folder to the queue.  This is intended to support the
     * "Add Folder" functionality.  This method itself does only sanity
     * checking, and if everything's in order, calls addRealPathFolderToQueue()
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

        final Path filepath = Paths.get(pathname);
        if (filepath == null) {
            logger.warning("cannot add files; filepath is null");
            return;
        }

        Path realpath = null;
        try {
            realpath = filepath.toRealPath();
        } catch (IOException ioe) {
            if (Files.notExists(filepath)) {
                logger.warning("will not add nonexistent file: " + filepath);
                return;
            }
            logger.warning("skipping " + pathname + " due to I/O exception");
            return;
        }

        if (!fileIsVisible(realpath)) {
            return;
        }

        if (!Files.isDirectory(realpath)) {
            logger.warning("argument to 'add folder' is not a folder");
        }

        final Folder folder = Folder.getFolder(realpath);
        addRealPathFolderToQueue(folder);
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
                addFilesRecursively(contents, null, path);
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

    @Override
    public void update(Observable observable, Object value) {
        if (value instanceof UserPreference) {
            UserPreference userPref = (UserPreference) value;
            if ((userPref == UserPreference.IGNORE_REGEX) && (observable instanceof UserPreferences)) {
                UserPreferences observed = (UserPreferences) observable;
                logger.info("looking for new ignorable files");
                ignoreKeywords = observed.getIgnoreKeywords();
                for (FileEpisode ep : episodes.values()) {
                    ep.setIgnoreReason(ignorableReason(ep.getFilepath()));
                }
                for (AddEpisodeListener listener : listeners) {
                    listener.refreshAll();
                }
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
