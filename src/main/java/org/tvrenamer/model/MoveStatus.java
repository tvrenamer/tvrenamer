package org.tvrenamer.model;

public enum MoveStatus {
    UNCHECKED,
    NO_FILE,
    UNMOVED,
    ALREADY_IN_PLACE,
    MOVING,
    RENAMED,
    MISNAMED,
    COPIED,
    FAIL_TO_MOVE
}
