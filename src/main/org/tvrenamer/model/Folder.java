package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

// Folder - represents a directory on disk which is presumed to contain one or more
//   episodes of a TV series, either directly or in subfolders.
//
// This is not much more than a wrapper around a Path object.  As such, it may seem like
// superfluous cruft.  But a folder's name often gives us information about its contents.
// Although we're not getting much of that information as of this first commit, this will
// be a place to find and store information in the future.
//
// It's also worth noting that Java's "Path" class (like "File" before it) does not make
// any class-level distinction between a directory and an ordinary file.  This class does.
// It is only used for directories.  So in the code, when you see a "Folder", you know
// right away it's a directory, whereas if you see a "Path", it's probably a movie file.
public class Folder {
    private static Logger logger = Logger.getLogger(Folder.class.getName());

    private static final Map<Path, Folder> ALL_FOLDERS = new ConcurrentHashMap<>();

    // To understand what this means, let's say the user tells us to work on a folder,
    //   C:/Users/me/Documents/Incoming/Videos/TV/to-rename
    // Then within that folder, there might be elaborate paths like:
    //   .../comedies/live-action/BigBang/1080p/s08/Big.Bang.Theory.S08E02.dvdrip.x264/
    // with contents like "Big.Bang.Theory.S08E02.dvdrip.x264.avi".
    // Or, of course, it might be much more simple, like:
    //  .../Veep/S04E01.mp4
    // However long the path is, it might help us figure out the series information, and it might
    // be something that the user wants to preserve.  But, we assume that's only true of the
    // "subpath" -- the part of the path below the folder that the user originally specified.

    // A Folder has a "parent", also a Folder.  But it tops out at the directory that the user
    // has given us to search -- NOT at the actual root of the file system.  So, using the
    // example above, "live-action" would have a parent of "comedies", but "comedies" would
    // have a parent of null.

    private Folder parent;
    private Path element;
    private Path realpath;

    private Folder(Folder parent, Path element, Path realpath) {
        logger.fine("creating folder!!\n  parent = " + parent + "\n  element = " + element);
        this.parent = parent;
        this.element = element;
        this.realpath = realpath;
    }

    public static Folder getFolder(Path realpath) {
        if (realpath == null) {
            logger.warning("cannot have null folder");
            return null;
        }
        Folder rval = ALL_FOLDERS.get(realpath);
        if (rval == null) {
            if (Files.exists(realpath)) {
                Path parentPath = realpath.getParent();
                Folder parent = ALL_FOLDERS.get(parentPath);
                Path fileName = realpath.getFileName();
                rval = new Folder(parent, fileName, realpath);
                ALL_FOLDERS.put(realpath, rval);
            } else {
                logger.warning("did not create folder for " + realpath);
            }
        }
        return rval;
    }

    public Path resolve(Path child) {
        return realpath.resolve(child);
    }

    public Path resolve(String child) {
        return realpath.resolve(child);
    }

    public Folder getParent() {
        return parent;
    }

    public Folder descend(Path file) {
        return getFolder(realpath.resolve(file));
    }

    public Path relativize(Path other) {
        return realpath.relativize(other);
    }

    public Path toPath() {
        return realpath;
    }

    public String getName() {
        return element.toString();
    }

    /**
     * When a user adds a folder to the TVRenamer, with "descend directories" on, we load the
     * whole subtree beneath it -- but, obviously, nothing above it.  The folder the user
     * specifies is a root of the tree we're adding.  This method retrieves that root folder.
     *
     * This is certainly _not_ the root of the file system; it's just the root of the tree
     * that the user specified.
     *
     * @return the "root" folder that caused this Folder to be added
     */
    public Path getRoot() {
        if (parent == null) {
            return realpath;
        }
        // recursive call
        return parent.getRoot();
    }

    /**
     * Get the relative path from this Folder's root (see {@link getRoot}) to the Folder.
     *
     * @return the the relative path from the root
     */
    public Path getRelativePath() {
        return getRoot().relativize(realpath);
    }

    /**
     * Standard object method to represent this Folder as a string.
     *
     * @return string version of this; just says how many episodes are in the map.
     */
    @Override
    public String toString() {
        return "Folder { " + realpath + " }";
    }
}
