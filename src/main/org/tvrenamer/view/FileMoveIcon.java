package org.tvrenamer.view;

import org.eclipse.swt.graphics.Image;

import org.tvrenamer.model.util.Constants;

public enum FileMoveIcon {
    ADDED("/icons/SweetieLegacy/16-circle-blue.png"),
    DOWNLOADING("/icons/SweetieLegacy/16-clock.png"),
    OPTIONS("/icons/SweetieLegacy/16-comment-question.png"),
    RENAMING("/icons/SweetieLegacy/16-em-pencil.png"),
    SUCCESS("/icons/SweetieLegacy/16-em-check.png"),
    FAIL("/icons/SweetieLegacy/16-em-cross.png");

    public final Image icon;

    FileMoveIcon(String path) {
        icon = UIUtils.readImageFromPath(path, Constants.ICON_PARENT_DIRECTORY + "/" + path);
    }
}
