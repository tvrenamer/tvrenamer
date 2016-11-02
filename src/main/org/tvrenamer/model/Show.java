package org.tvrenamer.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a TV Show, with a name, url and list of seasons.
 */
public class Show implements Comparable<Show> {
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
        return seasons.get(sNum);
    }

    @Override
    public String toString() {
        return "Show [id=" + id + ", name=" + name + ", url=" + url + ", seasons=" + seasons + "]";
    }

    @Override
    public int compareTo(Show other) {
        return Integer.parseInt(other.id) - Integer.parseInt(this.id);
    }

}
