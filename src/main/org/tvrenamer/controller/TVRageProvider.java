package org.tvrenamer.controller;

import org.tvrenamer.controller.util.StringUtils;
import org.tvrenamer.model.Season;
import org.tvrenamer.model.Show;
import org.tvrenamer.model.TVRenamerIOException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

/**
 * This class encapsulates the interactions between the application and the TVRage XML Feeds
 *
 * @author Vipul Delwadia
 * @author Dave Harris
 *
 */
public class TVRageProvider {
    private static final String ERROR_PARSING_TV_RAGE_XML = "Error parsing TVRage XML";
    private static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading show information.  Check internet or proxy settings";

    private static Logger logger = Logger.getLogger(TVRageProvider.class.getName());

    private static final String BASE_SEARCH_URL = "http://services.tvrage.com/feeds/search.php?show=";
    private static final String XPATH_SHOW = "//show";
    private static final String XPATH_SHOWID = "showid";
    private static final String XPATH_NAME = "name";
    private static final String XPATH_LINK = "link";
    private static final String BASE_LIST_URL = "http://services.tvrage.com/feeds/episode_list.php?sid=";
    private static final String XPATH_ALL = "*";
    private static final String XPATH_EPISODE_LIST = "/Show/Episodelist/Season";
    private static final String XPATH_SEASON_NUM = "seasonnum";
    private static final String XPATH_SEASON_ATTR = "no";
    private static final String XPATH_TITLE = "title";
    private static final String XPATH_AIRDATE = "airdate";

    private TVRageProvider() {
        // Prevents instantiation
    }

    /**
     * Uses the TVRage search tool to retrieve a list of possible shows based on the show name. The list returned is in
     * the order returned by the TVRage search.
     *
     * @param showName
     *            the show to search for
     * @return a list of matching shows in the order returned by the TVRage search.
     * @throws ConnectException
     *             thrown when we are connected to <strong>a</strong> network, but cannot connect to remote host, maybe
     *             offline or behind a proxy
     * @throws UnknownHostException
     *             when we don't have a network connection
     */
    public static ArrayList<Show> getShowOptions(String showName) throws TVRenamerIOException {
        ArrayList<Show> options = new ArrayList<Show>();
        String searchURL = BASE_SEARCH_URL + StringUtils.encodeSpecialCharacters(showName);

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            logger.info("About to retrieve search results from " + searchURL);

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
                expr = xpath.compile(XPATH_LINK);
                String optionUrl = ((Node) expr.evaluate(eNode, XPathConstants.NODE)).getTextContent();
                options.add(new Show(optionId, optionName, optionUrl));
            }
            return options;
        } catch (ConnectException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_TV_RAGE_XML, e);
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_TV_RAGE_XML, e);
        } catch (SAXException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_TV_RAGE_XML, e);
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new TVRenamerIOException(ERROR_PARSING_TV_RAGE_XML, e);
        }
    }

    /**
     * Uses the TVRage episode listings to populate the Show object with Season and Episode Data
     *
     * @param show
     *            the Show object to populate with season and episode data
     */
    public static void getShowListing(Show show) {

        String showURL = BASE_LIST_URL + show.getId();

        logger.info("Retrieving episode listing from " + showURL);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            String showXml = new HttpConnectionHandler().downloadUrl(showURL);

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(showXml)));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xpath = factory.newXPath();

            XPathExpression expr = xpath.compile(XPATH_EPISODE_LIST);

            NodeList seasons = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

            for (int i = 0; i < seasons.getLength(); i++) {
                Node sNode = seasons.item(i);
                int sNum = Integer.parseInt(sNode.getAttributes().getNamedItem(XPATH_SEASON_ATTR).getNodeValue());
                Season season = new Season(sNum);
                show.setSeason(sNum, season);

                expr = xpath.compile(XPATH_ALL);
                NodeList episodes = (NodeList) expr.evaluate(sNode, XPathConstants.NODESET);
                for (int j = 0; j < episodes.getLength(); j++) {
                    Node eNode = episodes.item(j);
                    expr = xpath.compile(XPATH_SEASON_NUM);
                    Node epNumNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                    expr = xpath.compile(XPATH_TITLE);
                    Node epTitleNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                    expr = xpath.compile(XPATH_AIRDATE);
                    Node airdateNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
                    logger.finer("[" + sNum + "x" + epNumNode.getTextContent() + "] " + epTitleNode.getTextContent());
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    season.addEpisode(Integer.parseInt(epNumNode.getTextContent()),
                                      epTitleNode.getTextContent(),
                                      df.parse(airdateNode.getTextContent()));
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Caught exception when attempting to parse show detail xml", e);
        }
    }
}
