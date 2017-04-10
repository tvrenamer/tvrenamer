package org.tvrenamer.model;

public enum ReplacementToken {
    // Note, the 'token' (ie. %s) format must match the PreferencesDialog.REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX so the dnd works correctly

    SHOW_NAME("%S", "Show Name"),
    SEASON_NUM("%s", "Season Number"),
    SEASON_NUM_LEADING_ZERO("%0s", "Season Number (with leading 0s)"),
    EPISODE_NUM("%e", "Episode Number"),
    EPISODE_NUM_LEADING_ZERO("%0e", "Episode Number (with leading 0s)"),
    EPISODE_TITLE("%t", "Episode Title"),
    EPISODE_TITLE_NO_SPACES("%T", "Episode Title (<space> replaced with '.')"),
    EPISODE_RESOLUTION("%r", "Episode resolution"),

    DATE_YEAR_FULL("%yyyy", "Year e.g. 2012"),
    DATE_YEAR_MIN ("%yy", "Short year e.g. 12"),
    DATE_MONTH_NUM("%m", "Month num"),
    DATE_MONTH_NUMLZ("%0m", "Month num (with leading 0s)"),
    DATE_DAY_NUM("%d", "Day num"),
    DATE_DAY_NUMLZ("%0d", "Day num (with leading 0s)");

    private String token;
    private String description;

    private ReplacementToken(String token, String description) {
        this.token = token;
        this.description = description;
    }

    public String getToken() {
        return token;
    }

    /**
     * @return text to display, currently %token, %description
     */
    @Override
    public String toString() {
        return token.concat(" : ").concat(description);
    }
}
