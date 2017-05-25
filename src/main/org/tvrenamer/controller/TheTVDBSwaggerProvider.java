package org.tvrenamer.controller;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.TVRenamerIOException;
import org.tvrenamer.model.UserPreferences;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TheTVDBSwaggerProvider {
    private static final Logger logger = Logger.getLogger(TheTVDBProvider.class.getName());

    private static final String API_KEY = "4A9560FF0B2670B2";

    private static final String BASE_URL = "https://api.thetvdb.com";
    private static final String LOGIN_URL = "/login";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient();

    static class LoginResponse {
        String token;
    }

    private static String login() throws IOException {
        String json = "{\"apikey\":\"" + API_KEY + "\"}";
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(BASE_URL + LOGIN_URL)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<LoginResponse> adapter = moshi.adapter(LoginResponse.class);

        String token = adapter.fromJson(response.body().string()).token;
        UserPreferences.getInstance().setApiToken(token);
        return token;
    }

    private static String get(String url) throws IOException {
        String jwt = UserPreferences.getInstance().getApiToken();
        if (jwt == null) {
            jwt = login();
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", "Bearer " + jwt)
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        switch (response.code()) {
            case 401:
            case 403:
                login();
                return get(url);
            default:
                return response.body().string();
        }
    }

    private static String getSearchSeries(final String queryName) throws IOException {
        return get(BASE_URL + "/search/series?name=" + queryName);
    }

    private static String getSeriesEpisodes(final Integer id) throws IOException {
        return get(BASE_URL + "/series/" + id + "/episodes");
    }

    private static String getSeriesEpisodes(final Integer id, final Integer page) throws IOException {
        return get(BASE_URL + "/series/" + id + "/episodes?page=" + page);
    }

    public static void getShowOptions(final ShowName showName) throws TVRenamerIOException {
        String responseJSON = "";
        try {
            responseJSON = getSearchSeries(showName.getQueryString());
            readSeriesFromSearchResponse(responseJSON, showName);
        } catch (IOException e) {
            String msg = "error parsing JSON from " + responseJSON + " for series " + showName.getFoundName();
            logger.log(Level.WARNING, msg, e);
            throw new TVRenamerIOException(msg, e);
        }
    }

    public static void readSeriesFromSearchResponse(String response, ShowName showName) throws IOException {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<SeriesSearchResponse> adapter = moshi.adapter(SeriesSearchResponse.class);

        List<Series> seriesList = adapter.fromJson(response).data;
        seriesList.forEach(series -> showName.addShowOption(series.id, series.seriesName));
    }

    static class SeriesSearchResponse {
        List<Series> data;
    }

    static class Series {
        Integer id;
        String seriesName;

        // following are currently not required
        List<String> aliases;
        String banner;
        String firstAired;
        String network;
        String overview;
        String status;
    }

    public static void getShowListing(final Show show) throws TVRenamerIOException {
        String responseJSON = "";
        try {
            responseJSON = getSeriesEpisodes(show.getId());
            readEpisodesFromSearchResponse(responseJSON, show);
        } catch (IOException e) {
            String msg = "error parsing JSON from " + responseJSON + " for series #" + show.getId();
            logger.log(Level.WARNING, msg, e);
            throw new TVRenamerIOException(msg, e);
        }
    }

    public static void readEpisodesFromSearchResponse(String response, Show show) throws IOException {
        Moshi moshi = new Moshi.Builder().build();
        JsonAdapter<EpisodesResponse> adapter = moshi.adapter(EpisodesResponse.class);

        EpisodesResponse episodesResponse = adapter.fromJson(response);

        if (episodesResponse.links.next != null) {
            readEpisodesFromSearchResponse(getSeriesEpisodes(show.getId(), episodesResponse.links.next), show);
        }

        List<Episode> episodeList = episodesResponse.data;

        List<EpisodeInfo> episodeInfos = new LinkedList<>();

        episodeList.forEach(episode -> {
            episodeInfos.add(new EpisodeInfo.Builder()
                    .episodeId(episode.id)
                    .seasonNumber(episode.airedSeason)
                    .episodeNumber(episode.airedEpisodeNumber)
                    .episodeName(episode.episodeName)
                    .firstAired(episode.firstAired)
                    .dvdSeason(episode.dvdSeason)
                    .dvdEpisodeNumber(episode.dvdEpisodeNumber)
                .build()
            );
        });

        show.addEpisodes(episodeInfos);
    }

    static class EpisodesResponse {
        Links links;
        List<Episode> data;
    }

    static class Links {
        Integer first;
        Integer last;
        Integer next;
        Integer prev;
    }

    static class Episode {
        String airedEpisodeNumber;
        String airedSeason;
        String dvdEpisodeNumber;
        String dvdSeason;
        String episodeName;
        String firstAired;
        String id;

        String absoluteNumber;
        String airedSeasonID;
        Language language;
        String lastUpdated;
        String overview;
    }

    static class Language {
        String episodeName;
        String overview;
    }
}
