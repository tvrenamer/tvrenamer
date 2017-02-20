package org.tvrenamer.controller;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Season;
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
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class TheTVDBProvider {
    public static final String IMDB_BASE_URL = "http://www.imdb.com/title/";

    private static final String ERROR_PARSING_XML = "Error parsing XML";
    private static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading show information. Check internet or proxy settings";

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
    private static final String BASE_LIST_FILENAME = "/all/en.xml";
    private static final String XPATH_EPISODE_LIST = "/Data/Episode";
    private static final String XPATH_SEASON_NUM = "SeasonNumber";
    private static final String XPATH_EPISODE_NUM = "EpisodeNumber";
    private static final String XPATH_EPISODE_NAME = "EpisodeName";
    private static final String XPATH_AIRDATE = "FirstAired";

    private static final String XPATH_DVD_EPISODE_NUM = "DVD_episodenumber";

    public static ArrayList<Show> getShowOptions(String showName) throws TVRenamerIOException {
        ArrayList<Show> options = new ArrayList<>();
        String searchURL = BASE_SEARCH_URL + StringUtils.encodeSpecialCharacters(showName);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            logger.fine("About to retrieve search results from " + searchURL);

            String searchXml = new HttpConnectionHandler().downloadUrl(searchURL);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(searchXml)));

            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            XPathExpression expr = xpath.compile(XPATH_SHOW);

            NodeList shows = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < shows.getLength(); i++) {
                Node eNode = shows.item(i);
                expr = xpath.compile(XPATH_SHOWID);
                String optionId = ((Node) expr.evaluate(eNode, XPathConstants.NODE)).getTextContent();
                expr = xpath.compile(XPATH_NAME);
                String optionName = ((Node) expr.evaluate(eNode, XPathConstants.NODE)).getTextContent();
                expr = xpath.compile(XPATH_IMDB);
                Node exprNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                String optionUrl = "";
                if (exprNode != null) {
                    optionUrl = IMDB_BASE_URL + exprNode.getTextContent();
                }
                options.add(new Show(optionId, optionName, optionUrl));
            }
            return options;
        } catch (ConnectException | UnknownHostException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
        } catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }

    public static void getShowListing(Show show) throws TVRenamerIOException {
        String showURL = BASE_LIST_URL + show.getId() + BASE_LIST_FILENAME;

        logger.fine("Retrieving episode listing from " + showURL);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        String showXml;
        try {
            showXml = new HttpConnectionHandler().downloadUrl(showURL);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(showXml)));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            XPathExpression expr = xpath.compile(XPATH_EPISODE_LIST);

            NodeList episodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < episodes.getLength(); i++) {
                Node eNode = episodes.item(i);
                expr = xpath.compile(XPATH_SEASON_NUM);
                Node seasonNumNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);

                int seasonNum = Integer.parseInt(seasonNumNode.getTextContent());
                Season season = show.getSeason(seasonNum);
                if (season == null) {
                    season = new Season(show, seasonNum);
                    show.setSeason(seasonNum, season);
                }

                int epNum = Integer.MIN_VALUE;

                expr = xpath.compile(XPATH_DVD_EPISODE_NUM);
                Node epNumNodeDvd = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                if (epNumNodeDvd != null) {
                    try {
                        BigDecimal bd = new BigDecimal(epNumNodeDvd.getTextContent());
                        epNum = bd.intValueExact();
                    } catch (ArithmeticException e) {
                        // not an integer, fall back to episode number
                    } catch (NumberFormatException e) {
                        // not a number, fall back to episode number
                    }
                }
                if (epNum == Integer.MIN_VALUE) {
                    expr = xpath.compile(XPATH_EPISODE_NUM);
                    Node epNumNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                    BigDecimal bd = new BigDecimal(epNumNode.getTextContent());
                    try {
                        epNum = bd.intValueExact();
                    } catch (ArithmeticException e) {
                        // not an integer, need to skip this episode?
                        continue;
                    } catch (NumberFormatException e) {
                        // not a number, need to skip this episode?
                        continue;
                    }
                }
                expr = xpath.compile(XPATH_EPISODE_NAME);
                Node epNameNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                expr = xpath.compile(XPATH_AIRDATE);
                Node airdateNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                logger.finer("[" + seasonNumNode.getTextContent() + "x" + epNum + "] " + epNameNode.getTextContent());
                String airdate = airdateNode.getTextContent();
                LocalDate date = null;
                if (StringUtils.isNotBlank(airdate)) {
                    try {
                        date = LocalDate.parse(airdate, DATE_FORMATTER);
                    } catch (DateTimeParseException e) {
                        // While a null or empty string is taken to mean "now",
                        // a badly formatted string is an error and will not
                        // be translated into any date.
                        logger.warning("could not parse as date: " + airdate);
                        date = null;
                    }
                } else {
                    // When no value (good or bad) is found, use "now".
                    date = LocalDate.now();
                }
                season.addEpisode(epNum, epNameNode.getTextContent(), date);
            }
        } catch (ParserConfigurationException | XPathExpressionException | SAXException | IOException | NumberFormatException | DOMException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_XML, e);
        }
    }
}
