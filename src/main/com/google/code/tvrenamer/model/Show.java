package com.google.code.tvrenamer.model;

import java.util.HashMap;
import java.util.Map;

public class Show {
	private final String id;
	private final String name;
	private final String url;

	private final Map<Integer, Season> seasons;

	public Show(String id, String name, String url) {
		this.id = id;
		this.name = name;
		this.url = url;

		seasons = new HashMap<Integer, Season>();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getUrl() {
		return url;
	}

	public void setSeason(int sNum, Season season) {
		seasons.put(sNum, season);
	}

	public Season getSeason(int sNum) {
		Season s = seasons.get(sNum);
		if (s == null)
			throw new SeasonNotFoundException("Season #" + sNum + " not found for show '" + name + "'");
		return s;
	}
}
