package com.google.code.tvrenamer.model;

public enum ReplacementToken {
	// Note, the 'token' (ie. %s) format must match the PreferenesDialog.REPLACEMENT_OPTIONS_LIST_ENTRY_REGEX so the dnd works correctly
	
	SHOW_NAME("%S", "Show Name"), 
	SEASON_NUM("%s", "Season Number"), 
	SEASON_NUM_LEADING_ZERO("%0s", "Season Number (with leading 0)"), 
	EPISODE_NUM("%e", "Episode Number"), 
	EPISODE_NUM_NO_LEADING_ZERO("%E", "Episode Number (without leading 0s)"), 
	EPISODE_TITLE("%t", "Episode Title"), 
	EPISODE_TITLE_NO_SPACES("%T", "Episode Title (<space> replaced with '.')"),
	SEASON_NUM_LEADING_ZERO("%0s", "Season Number (with leading 0)"),
	YEAR("%y", "Year if available"),
	YEAR_WITH_BRACKETS("%Y", "Year if available (with brackets)");
	
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
