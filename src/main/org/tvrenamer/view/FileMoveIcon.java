package org.tvrenamer.view;

import static org.tvrenamer.model.util.Constants.*;

import org.eclipse.swt.graphics.Image;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileMoveIcon {
    public enum Status {
        SUCCESS,
        OPTIONS,
        ADDED,
        DOWNLOADING,
        RENAMING,
        FAIL;
    }

    private final Status status;
    private final Image image;
    private final String ordering;

    FileMoveIcon(String ordering, Status status, String imageFilename) {
        this.ordering = ordering;
        this.status = status;
        this.image = UIStarter.readImageFromPath(SUBLINK_PATH + imageFilename);
    }

    private static final FileMoveIcon[] STANDARD_STATUSES = {
        new FileMoveIcon("a", Status.SUCCESS, "16-em-check.png"),
        new FileMoveIcon("b", Status.OPTIONS, "16-circle-green-add.png"),
        new FileMoveIcon("c", Status.ADDED, "16-circle-blue.png"),
        new FileMoveIcon("d", Status.DOWNLOADING, "16-clock.png"),
        new FileMoveIcon("e", Status.RENAMING, "16-em-pencil.png"),
        new FileMoveIcon("f", Status.FAIL, "16-em-cross.png")
    };

    private static final Map<Status, FileMoveIcon> MAPPING = new ConcurrentHashMap<>();
    private static final Map<Image, FileMoveIcon> IMAGES = new ConcurrentHashMap<>();

    static {
        for (FileMoveIcon state : STANDARD_STATUSES) {
            MAPPING.put(state.status, state);
            IMAGES.put(state.image, state);
        }
    }

    public static Image getIcon(final Status status) {
        FileMoveIcon state = MAPPING.get(status);
        if (state == null) {
            return null;
        }
        return state.image;
    }

    public static String getImagePriority(final Image img) {
        FileMoveIcon state = IMAGES.get(img);
        if (state == null) {
            return null;
        }
        return state.ordering;
    }
}
