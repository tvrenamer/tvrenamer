package org.tvrenamer.model;

public enum UserPreference {
    REPLACEMENT_MASK,
    MOVE_ENABLED,
    RENAME_ENABLED,
    REMOVE_EMPTY,
    DEST_DIR,
    SEASON_PREFIX,
    LEADING_ZERO,
    ADD_SUBDIRS,
    IGNORE_REGEX,

    // Since these are only meaningful at startup, they probably should not be watched
    UPDATE_CHECK,
    @SuppressWarnings("unused")
    PRELOAD_FOLDER
}
