package org.tvrenamer.controller;

import static org.tvrenamer.model.util.Constants.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.tvrenamer.model.DiscontinuedApiException;
import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.Series;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.TVRenamerIOException;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Looks up series and episode information using the TVmaze API.<p>
 *
 * TVmaze serves JSON over HTTPS and needs no API key.  It replaced TheTVDB,
 * whose version 1 XML API this program used until that API was retired.<p>
 *
 * TVmaze publishes a single episode ordering, the over-the-air one, so the
 * DVD ordering fields of {@link EpisodeInfo} are left unset.  {@link
 * org.tvrenamer.model.Episode} treats a missing DVD placement as "fall back to
 * the air placement", so shows still resolve.
 */
public class TVmazeProvider {
    private static final Logger logger = Logger.getLogger(TVmazeProvider.class.getName());

    // Whether we have concluded that the API is no longer there at all.
    private static boolean apiIsDeprecated = false;

    private static final String API_URL = "https://api.tvmaze.com/";

    // The URL to get, to receive options for a given series search string.
    private static final String BASE_SEARCH_URL = API_URL + "search/shows?q=";

    // The URL to get, to receive the episode listing for a specific series.
    //
    // This endpoint leaves out specials unless "specials=1" is added, but we do
    // not ask for them. TVmaze has no season 0: it files a special under the
    // season it aired in, with a null episode number. An episode with no number
    // cannot be placed in the season index, so asking for specials would fetch
    // entries that can never be matched to a file. Note this means S00Exx
    // filenames cannot be resolved against this provider.
    private static final String BASE_LIST_URL = API_URL + "shows/";
    private static final String BASE_LIST_SUFFIX = "/episodes";

    // Keys in the search results.  Each element of the array wraps the series
    // it matched in a "show" object, alongside the match score.
    private static final String JSON_SHOW = "show";
    private static final String JSON_SHOW_ID = "id";
    private static final String JSON_SHOW_NAME = "name";

    // Keys in the episode listing.
    private static final String JSON_EPISODE_ID = "id";
    private static final String JSON_SEASON_NUM = "season";
    private static final String JSON_EPISODE_NUM = "number";
    private static final String JSON_EPISODE_NAME = "name";
    private static final String JSON_AIRDATE = "airdate";

    /**
     * Read a string out of a JSON object, mapping JSON nulls and missing
     * members alike onto a Java null.
     *
     * @param obj
     *   the object to read the member from
     * @param key
     *   the name of the member to read
     * @return the member's value as a String, or null if it is absent or null
     */
    private static String optString(final JsonObject obj, final String key) {
        JsonElement value = obj.get(key);
        if ((value == null) || value.isJsonNull()) {
            return null;
        }
        return value.getAsString();
    }

    private static String encodeQuery(final String queryString) {
        try {
            return URLEncoder.encode(queryString, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is required to be supported by every JVM.
            throw new IllegalStateException(e);
        }
    }

    private static String downloadJson(final String url)
        throws TVRenamerIOException, DiscontinuedApiException
    {
        if (apiIsDeprecated) {
            throw new DiscontinuedApiException();
        }

        logger.fine("About to download " + url);

        return new HttpConnectionHandler().downloadUrl(url);
    }

    private static JsonArray parseJsonArray(final String json)
        throws TVRenamerIOException
    {
        try {
            return JsonParser.parseString(json).getAsJsonArray();
        } catch (JsonParseException | IllegalStateException e) {
            logger.log(Level.WARNING, ERROR_PARSING_JSON, e);
            throw new TVRenamerIOException(ERROR_PARSING_JSON, e);
        }
    }

    private static void collectShowOptions(final JsonArray results, final ShowName showName) {
        for (JsonElement result : results) {
            JsonObject show = result.getAsJsonObject().getAsJsonObject(JSON_SHOW);
            if (show == null) {
                continue;
            }
            String id = optString(show, JSON_SHOW_ID);
            String seriesName = optString(show, JSON_SHOW_NAME);
            if ((id == null) || (seriesName == null)) {
                logger.warning("ignoring incomplete option for "
                               + showName.getExampleFilename());
            } else {
                showName.addShowOption(id, seriesName);
            }
        }
    }

    /**
     * Decide whether the given failure means the provider's API has gone away,
     * as opposed to a transient network problem.  A 404 on an endpoint we know
     * the shape of means the endpoint itself is gone.
     *
     * @param e
     *   the exception we caught
     * @return true if the API appears to be discontinued
     */
    private static synchronized boolean isApiDiscontinuedError(Throwable e) {
        if (apiIsDeprecated) {
            return true;
        }
        while (e != null) {
            if (e instanceof FileNotFoundException) {
                apiIsDeprecated = true;
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    /**
     * Fetch the show options from the provider, for the given show name.
     *
     * @param showName
     *   the show name to fetch the options for
     * @throws DiscontinuedApiException if it appears that the API we are using
     *   is no longer supported
     * @throws TVRenamerIOException if anything else goes wrong; this could
     *   include network difficulties or difficulty parsing the JSON.
     */
    public static void getShowOptions(final ShowName showName)
        throws TVRenamerIOException, DiscontinuedApiException
    {
        final String searchUrl = BASE_SEARCH_URL + encodeQuery(showName.getQueryString());
        try {
            String json = downloadJson(searchUrl);
            collectShowOptions(parseJsonArray(json), showName);
        } catch (TVRenamerIOException tve) {
            if (isApiDiscontinuedError(tve)) {
                throw new DiscontinuedApiException();
            }
            String msg = "error looking up " + showName.getExampleFilename()
                + " at " + searchUrl;
            logger.log(Level.WARNING, msg, tve);
            throw new TVRenamerIOException(msg, tve);
        }
    }

    private static EpisodeInfo createEpisodeInfo(final JsonObject episode) {
        try {
            return new EpisodeInfo.Builder()
                .episodeId(optString(episode, JSON_EPISODE_ID))
                .seasonNumber(optString(episode, JSON_SEASON_NUM))
                .episodeNumber(optString(episode, JSON_EPISODE_NUM))
                .episodeName(optString(episode, JSON_EPISODE_NAME))
                .firstAired(optString(episode, JSON_AIRDATE))
                .build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "exception parsing episode", e);
        }
        return null;
    }

    /**
     * Fetch the episode listings from the provider, for the given Series.
     *
     * @param series
     *   the Series to fetch the episode listings for
     * @throws TVRenamerIOException if anything goes wrong; this could include
     *   network difficulties or difficulty parsing the JSON.
     */
    public static void getSeriesListing(final Series series)
        throws TVRenamerIOException
    {
        final String listingUrl = BASE_LIST_URL + series.getId() + BASE_LIST_SUFFIX;
        final JsonArray episodes = parseJsonArray(downloadJson(listingUrl));

        final List<EpisodeInfo> infos = new ArrayList<>(episodes.size());
        for (JsonElement episode : episodes) {
            infos.add(createEpisodeInfo(episode.getAsJsonObject()));
        }

        series.addEpisodeInfos(infos.toArray(new EpisodeInfo[0]));
        series.listingsSucceeded();
    }
}
