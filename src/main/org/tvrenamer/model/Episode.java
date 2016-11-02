package org.tvrenamer.model;

import java.util.Date;

public class Episode {
	private final String title;
	private final int epNumAbs;
	private final Date airDate;

	public Episode (String title, int epNumAbs, Date airDate) {
		this.title = title;
		this.epNumAbs = epNumAbs;
		this.airDate = airDate;
	}

	public String getTitle() {
		return this.title;
	}

	public int getEpNumAbs() {
		return epNumAbs;
	}

	public Date getAirDate() {
		return this.airDate;
	}

	@Override
	public String toString() {
		return "Episode [title=" + title + ", airDate=" + airDate + "]";
	}
}