package com.google.code.tvrenamer.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Season {
	private final int num;
	private final Map<Integer, Episode> episodes;

	public Season(int num) {
		this.num = num;
		episodes = new HashMap<Integer, Episode>();
	}

	public int getNumber() {
		return num;
	}

	public void addEpisode(int epNum, String title, Date airDate) {
		episodes.put(epNum, new Episode(title,airDate));
	}

	public String getTitle(int epNum) {
		Episode e = episodes.get(epNum);
		if (e == null)
			throw new EpisodeNotFoundException("Episode #" + epNum + " not found for season #" + this.num);
		return e.getTitle();
	}
	
	public Date getAirDate(int epNum) {
		Episode e = episodes.get(epNum);
		if (e == null)
			throw new EpisodeNotFoundException("Episode #" + epNum + " not found for season #" + this.num);
		return e.getAirDate();
	}
	
	private class Episode {
		private String title;
		private Date airDate;
		
		public Episode (String title, Date airDate) {
			this.title = title;
			this.airDate = airDate;
		}

		public String getTitle() {
			return title;
		}

		public Date getAirDate() {
			return airDate;
		}
	}
	
}
