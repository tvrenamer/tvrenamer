package org.tvrenamer.controller;

import static org.tvrenamer.controller.util.XPathUtilities.nodeListValue;
import static org.tvrenamer.controller.util.XPathUtilities.nodeTextValue;
import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.TVRenamerIOException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public class TheTVDBProvider {
    private static Logger logger = Logger.getLogger(TheTVDBProvider.class.getName());

    private static final String EPISODE_DATE_FORMAT = "yyyy-MM-dd";
    // Unlike java.text.DateFormat, DateTimeFormatter is thread-safe, so we can create just one instance here.
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(EPISODE_DATE_FORMAT);

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

    private static final String XPATH_SEASON_NUM = "SeasonNumber";
    private static final String XPATH_EPISODE_NUM = "EpisodeNumber";
    private static final String XPATH_EPISODE_NAME = "EpisodeName";
    private static final String XPATH_DVD_EPISODE_NUM = "DVD_episodenumber";
    private static final String XPATH_AIRDATE = "FirstAired";

    // sets whether or not to prefer DVD episode number from TVDB listings over plain episode number
    private static boolean preferDVDEpNum = true;

    private static String getShowSearchXml(final String showName)
        throws TVRenamerIOException
    {
        String searchURL = BASE_SEARCH_URL + StringUtils.encodeSpecialCharacters(showName);

        logger.info("About to download search results from " + searchURL);

        String searchXmlText = new HttpConnectionHandler().downloadUrl(searchURL);
        return searchXmlText;
    }

    private static String getShowListingXml(final Show show)
        throws TVRenamerIOException
    {
        String showURL = BASE_LIST_URL + show.getId() + BASE_LIST_FILENAME;

        logger.info("Downloading episode listing from " + showURL);

        String listingXmlText = new HttpConnectionHandler().downloadUrl(showURL);
        return listingXmlText;
    }

    private static List<Show> collectShowOptions(final NodeList shows)
        throws XPathExpressionException
    {
        List<Show> options = new ArrayList<>();

        for (int i = 0; i < shows.getLength(); i++) {
            Node eNode = shows.item(i);
            String seriesName = nodeTextValue(XPATH_NAME, eNode);
            String tvdbId = nodeTextValue(XPATH_SHOWID, eNode);
            String imdbId = nodeTextValue(XPATH_IMDB, eNode);

            if (SERIES_NOT_PERMITTED.equals(seriesName)) {
                // Unfortunately we don't have the show name handy here,
                // and we'd have to thread it through a few methods just
                // for the sake of an unlikely warning, so I prefer to
                // leave this message a little uninformative.
                logger.warning("ignoring unpermitted option");
            } else {
                Show show = new Show(tvdbId, seriesName,
                                     (imdbId == null) ? "" : IMDB_BASE_URL + imdbId);
                options.add(show);
            }
        }

        return options;
    }

    public static List<Show> readShowsFromInputSource(final DocumentBuilder bld,
                                                      final InputSource searchXmlSource)
        throws TVRenamerIOException
    {
        try {
            Document doc = bld.parse(searchXmlSource);
            NodeList shows = nodeListValue(XPATH_SHOW, doc);
            return collectShowOptions(shows);
        } catch (SAXException | XPathExpressionException | DOMException | IOException e) {
            logger.log(Level.WARNING, ERROR_PARSING_XML, e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    public static List<Show> getShowOptions(final String showName)
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
            return readShowsFromInputSource(bld, source);
        } catch (TVRenamerIOException tve) {
            String msg  = "error parsing XML from " + searchXml + " for series " + showName;
            logger.log(Level.WARNING, msg, tve);
            throw new TVRenamerIOException(msg, tve);
        }
    }

    private static Integer getEpisodeNumberFromNode(final String name, final Node eNode)
        throws XPathExpressionException
    {
        String epNumText = nodeTextValue(name, eNode);
        return StringUtils.stringToInt(epNumText);
    }

    private static Integer getEpisodeNumber(final Node eNode)
        throws XPathExpressionException
    {
        String choice = preferDVDEpNum ? XPATH_DVD_EPISODE_NUM : XPATH_EPISODE_NUM;
        Integer epNum = getEpisodeNumberFromNode(choice, eNode);

        if (epNum != null) {
            return epNum;
        }

        choice = preferDVDEpNum ? XPATH_EPISODE_NUM : XPATH_DVD_EPISODE_NUM;
        return getEpisodeNumberFromNode(choice, eNode);
    }

    private static LocalDate getEpisodeDate(final Node eNode)
        throws XPathExpressionException
    {
        String airdate = nodeTextValue(XPATH_AIRDATE, eNode);
        LocalDate date = null;
        if (StringUtils.isBlank(airdate)) {
            // When no value (good or bad) is found, use "now".
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(airdate, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                // While a null or empty string is taken to mean "now",
                // a badly formatted string is an error and will not
                // be translated into any date.
                logger.warning("could not parse as date: " + airdate);
                date = null;
            }
        }

        return date;
    }

    private static void addEpisodeToShow(final Node eNode, final Show show) {
        try {
            Integer epNum = getEpisodeNumber(eNode);
            if (epNum == null) {
                logger.info("ignoring episode with no epnum: " + eNode);
                return;
            }

            String seasonNumString = nodeTextValue(XPATH_SEASON_NUM, eNode);
            String episodeName = nodeTextValue(XPATH_EPISODE_NAME, eNode);
            logger.finer("[" + seasonNumString + "x" + epNum + "] " + episodeName);

            LocalDate date = getEpisodeDate(eNode);

            show.addEpisode(seasonNumString, epNum, episodeName, date);
        } catch (Exception e) {
            logger.warning("exception parsing episode of " + show);
            logger.warning(e.toString());
        }
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
        } catch (XPathExpressionException | SAXException | IOException | NumberFormatException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
        return episodeList;
    }

    public static void getShowListing(final Show show)
        throws TVRenamerIOException
    {
        try {
            NodeList episodes = getEpisodeList(show);
            for (int i = 0; i < episodes.getLength(); i++) {
                addEpisodeToShow(episodes.item(i), show);
            }
        } catch (IOException | NumberFormatException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    /**
     * Sets whether or not prefer the DVD episode number from TVDB over the plain
     * episode number.
     *
     * @param preferDVDEpNum TRUE or FALSE depending upon preferences of DVD number
     *             over plain episode number.
     */
    public static void setPreferDVDEpNum(boolean preferDVDEpNum) {
        TheTVDBProvider.preferDVDEpNum = preferDVDEpNum;
    }
}
