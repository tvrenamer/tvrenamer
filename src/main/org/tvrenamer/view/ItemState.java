package org.tvrenamer.view;

import org.eclipse.swt.graphics.Image;

import org.tvrenamer.model.util.Constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemState {
    private final Image image;
    private final String ordering;

    private ItemState(String ordering, String imageFilename) {
        this.ordering = ordering;
        this.image = UIStarter.readImageFromPath(Constants.SUBLINK_PATH + imageFilename);
    }

    public static final ItemState SUCCESS = new ItemState("a", "16-em-check.png");
    public static final ItemState OPTIONS = new ItemState("b", "16-circle-green-add.png");
    public static final ItemState ADDED = new ItemState("c", "16-circle-blue.png");
    public static final ItemState DOWNLOADING = new ItemState("d", "16-clock.png");
    public static final ItemState RENAMING = new ItemState("e", "16-em-pencil.png");
    public static final ItemState FAIL = new ItemState("f", "16-em-cross.png");

    private static final ItemState[] STANDARD_STATUSES = {
        SUCCESS,
        OPTIONS,
        ADDED,
        DOWNLOADING,
        RENAMING,
        FAIL
    };

    private static final Map<Image, ItemState> IMAGES = new ConcurrentHashMap<>();

    static {
        for (ItemState state : STANDARD_STATUSES) {
            IMAGES.put(state.image, state);
        }
    }

    /**
     * Gets the Image associated with this ItemState
     *
     * @return
     *    the Image to display for this ItemState
     */
    public Image getIcon() {
        return image;
    }

    /**
     * Returns a "prioritized" string that the given Image is mapped to.
     *
     * This is used for sorting.  If the user clicks the column header to sort
     * by "Status", we want to sort the table in a meaningful way.  Specifically,
     * assuming sort direction is "up", we want the "most resolved" files at
     * the top, and the "least resolved" at the bottom.
     *
     * Therefore, we associate each status with a String, which is meaningless
     * except that it is lexicographically appropriate relative to the Strings
     * of the other statuses.
     *
     * But in the case of sorting the table, we don't really even have the status!
     * All we can easily retrieve is the actual Image object that is being displayed
     * in the cell.  So, we maintain a mapping from Image objects to priority
     * Strings, as well, and use that mapping here.
     *
     * If we cannot find the given Image in the mapping, we return null, which might
     * well cause a null pointer exception in the caller.  It's up to the caller to
     * deal with that, but if things are so confused that we have an unrecognized
     * Image in the cell, maybe it's best for the program to just exit...
     *
     * @param img
     *   the Image that we want mapped to a priority String
     * @return
     *   a priority String if the Image is found, or null if it isn't
     */
    public static String getImagePriority(final Image img) {
        ItemState state = IMAGES.get(img);
        if (state == null) {
            return null;
        }
        return state.ordering;
    }
}
