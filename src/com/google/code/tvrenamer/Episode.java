package com.google.code.tvrenamer;

public class Episode {

	private final String path;
	private final String number;
	private final String title;
	private final String season;
	private final String show;

	public Episode(String path, String number, String title, String season,
			String show) {
		this.path = path;
		this.number = number;
		this.title = title;
		this.season = season;
		this.show = show;
	}

	public String getNumber() {
		return number;
	}

	public String getPath() {
		return path;
	}

	public String getSeason() {
		return season;
	}

	public String getShow() {
		return show;
	}

	public String getTitle() {
		return title;
	}

}
