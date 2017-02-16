package org.tvrenamer.model;

public enum UserPreference {
    PROXY,
    REPLACEMENT_MASK,
    MOVE_ENABLED,
    RENAME_ENABLED,
    DEST_DIR,
    SEASON_PREFIX,
    LEADING_ZERO,
    ADD_SUBDIRS,
    IGNORE_REGEX,

    // Since these are only meaningful at startup, they probably should not be watched
    UPDATE_CHECK,
    PRELOAD_FOLDER
}
