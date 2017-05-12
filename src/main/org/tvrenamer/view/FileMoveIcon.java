package org.tvrenamer.view;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.tvrenamer.model.util.Constants;

import java.io.InputStream;

public enum FileMoveIcon {
    ADDED("/icons/SweetieLegacy/16-circle-blue.png"),
    DOWNLOADING("/icons/SweetieLegacy/16-clock.png"),
    RENAMING("/icons/SweetieLegacy/16-em-pencil.png"),
    // SUCCESS("/icons/SweetieLegacy/16-em-check.png"),
    FAIL("/icons/SweetieLegacy/16-em-cross.png");

    public final Image icon;

    FileMoveIcon(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        if (stream != null) {
            icon = new Image(Display.getCurrent(), stream);
        } else {
            icon = new Image(Display.getCurrent(), Constants.ICON_PARENT_DIRECTORY
                             + "/" + path);
        }
    }
}
