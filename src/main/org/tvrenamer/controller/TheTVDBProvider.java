package org.tvrenamer.controller;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;
import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.model.EpisodeInfo;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.ShowName;
import org.tvrenamer.model.TVRenamerIOException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class TheTVDBProvider {
    private static Logger logger = Logger.getLogger(TheTVDBProvider.class.getName());

    private static final String API_KEY = "4A9560FF0B2670B2";

    private static final String BASE_SEARCH_URL = "http://www.thetvdb.com/api/GetSeries.php?seriesname=";
    private static final String XPATH_SHOW = "/Data/Series";
    private static final String XPATH_SHOWID = "seriesid";
    private static final String XPATH_NAME = "SeriesName";
    private static final String XPATH_IMDB = "IMDB_ID";
    private static final String BASE_LIST_URL = "http://thetvdb.com/api/" + API_KEY + "/series/";
    private static final String BASE_LIST_FILENAME = "/all/" + DEFAULT_LANGUAGE + XML_SUFFIX;
    private static final String XPATH_EPISODE_LIST = "/Data/Episode";
    private static final String SERIES_NOT_PERMITTED = "** 403: Series Not Permitted **";

    private static final String XPATH_EPISODE_ID = "id";
    private static final String XPATH_SEASON_NUM = "SeasonNumber";
    private static final String XPATH_EPISODE_NUM = "EpisodeNumber";
    private static final String XPATH_EPISODE_NAME = "EpisodeName";
    private static final String XPATH_AIRDATE = "FirstAired";
    private static final String XPATH_EPISODE_SERIES_ID = "seriesid";
    private static final String XPATH_DVD_SEASON_NUM = "DVD_season";
    private static final String XPATH_DVD_EPISODE_NUM = "DVD_episodenumber";
    private static final String XPATH_EPISODE_NUM_ABS = "absolute_number";

    private static String getShowSearchXml(final ShowName showName)
        throws TVRenamerIOException
    {
        String searchURL = BASE_SEARCH_URL + showName.getQueryString();

        logger.fine("About to download search results from " + searchURL);

        String searchXmlText = new HttpConnectionHandler().downloadUrl(searchURL);
        return searchXmlText;
    }

    private static String getShowListingXml(final Show show)
        throws TVRenamerIOException
    {
        String showURL = BASE_LIST_URL + show.getId() + BASE_LIST_FILENAME;

        logger.fine("Downloading episode listing from " + showURL);

        String listingXmlText = new HttpConnectionHandler().downloadUrl(showURL);
        return listingXmlText;
    }

    private static void collectShowOptions(final NodeList shows, final ShowName showName)
        throws XPathExpressionException
    {
        for (int i = 0; i < shows.getLength(); i++) {
            Node eNode = shows.item(i);
            String seriesName = nodeTextValue(XPATH_NAME, eNode);
            String tvdbId = nodeTextValue(XPATH_SHOWID, eNode);
            String imdbId = nodeTextValue(XPATH_IMDB, eNode);

            if (SERIES_NOT_PERMITTED.equals(seriesName)) {
                logger.warning("ignoring unpermitted option for "
                               + showName.getFoundName());
            } else {
                showName.addShowOption(tvdbId, seriesName, imdbId);
            }
        }
    }

    public static void readShowsFromInputSource(final DocumentBuilder bld,
                                                final InputSource searchXmlSource,
                                                final ShowName showName)
        throws TVRenamerIOException
    {
        try {
            Document doc = bld.parse(searchXmlSource);
            NodeList shows = nodeListValue(XPATH_SHOW, doc);
            collectShowOptions(shows, showName);
        } catch (SAXException | XPathExpressionException | DOMException | IOException e) {
            logger.log(Level.WARNING, ERROR_PARSING_XML, e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    public static void getShowOptions(final ShowName showName)
        throws TVRenamerIOException
    {
        DocumentBuilder bld;
        try {
            bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, "could not create DocumentBuilder: " + e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }

        String searchXml = "";
        try {
            searchXml = getShowSearchXml(showName);
            InputSource source = new InputSource(new StringReader(searchXml));
            readShowsFromInputSource(bld, source, showName);
        } catch (TVRenamerIOException tve) {
            String msg  = "error parsing XML from " + searchXml + " for series "
                + showName.getFoundName();
            logger.log(Level.WARNING, msg, tve);
            throw new TVRenamerIOException(msg, tve);
        }
    }

    private static EpisodeInfo createEpisodeInfo(final Node eNode) {
        try {
            return new EpisodeInfo.Builder()
                .episodeId(nodeTextValue(XPATH_EPISODE_ID, eNode))
                .seasonNumber(nodeTextValue(XPATH_SEASON_NUM, eNode))
                .episodeNumber(nodeTextValue(XPATH_EPISODE_NUM, eNode))
                .episodeName(nodeTextValue(XPATH_EPISODE_NAME, eNode))
                .firstAired(nodeTextValue(XPATH_AIRDATE, eNode))
                .dvdSeason(nodeTextValue(XPATH_DVD_SEASON_NUM, eNode))
                .dvdEpisodeNumber(nodeTextValue(XPATH_DVD_EPISODE_NUM, eNode))
                .absoluteNumber(nodeTextValue(XPATH_EPISODE_NUM_ABS, eNode))
                .seriesId(nodeTextValue(XPATH_EPISODE_SERIES_ID, eNode))
                .build();
        } catch (Exception e) {
            logger.log(Level.WARNING, "exception parsing episode", e);
        }
        return null;
    }

    private static NodeList getEpisodeList(final Show show)
        throws TVRenamerIOException
    {
        NodeList episodeList;

        DocumentBuilder dbf;
        try {
            dbf = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, "could not create DocumentBuilder: " + e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }

        try {
            String listingsXml = getShowListingXml(show);
            InputSource listingsXmlSource = new InputSource(new StringReader(listingsXml));
            Document doc = dbf.parse(listingsXmlSource);
            episodeList = nodeListValue(XPATH_EPISODE_LIST, doc);
        } catch (XPathExpressionException | SAXException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, nfe.getMessage(), nfe);
            throw new TVRenamerIOException(ERROR_PARSING_NUMBERS, nfe);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, ioe.getMessage(), ioe);
            throw new TVRenamerIOException(DOWNLOADING_FAILED_MESSAGE, ioe);
        }
        return episodeList;
    }

    public static void getShowListing(final Show show)
        throws TVRenamerIOException
    {
        try {
            NodeList episodes = getEpisodeList(show);
            int episodeCount = episodes.getLength();

            EpisodeInfo[] episodeInfos = new EpisodeInfo[episodeCount];
            for (int i = 0; i < episodeCount; i++) {
                episodeInfos[i] = createEpisodeInfo(episodes.item(i));
            }
            show.addEpisodes(episodeInfos);

        } catch (DOMException dom) {
            logger.log(Level.WARNING, dom.getMessage(), dom);
            throw new TVRenamerIOException(ERROR_PARSING_XML, dom);
        } catch (NumberFormatException nfe) {
            logger.log(Level.WARNING, nfe.getMessage(), nfe);
            throw new TVRenamerIOException(ERROR_PARSING_NUMBERS, nfe);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, ioe.getMessage(), ioe);
            throw new TVRenamerIOException(DOWNLOADING_FAILED_MESSAGE, ioe);
        }
    }
}
