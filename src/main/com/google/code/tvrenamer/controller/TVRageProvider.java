package com.google.code.tvrenamer.controller;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;

/**
 * This class encapsulates the interactions between the application and the TVRage XML Feeds
 * 
 * @author Vipul Delwadia
 * @author Dave Harris
 * 
 */
public class TVRageProvider {
	private static Logger logger = Logger.getLogger(TVRageProvider.class.getName());

	private static final String BASE_SEARCH_URL = "http://www.tvrage.com/feeds/search.php?show=";
	private static final String XPATH_SHOW = "//show";
	private static final String XPATH_SHOWID = "showid";
	private static final String XPATH_NAME = "name";
	private static final String XPATH_LINK = "link";
	private static final String BASE_LIST_URL = "http://www.tvrage.com/feeds/episode_list.php?sid=";
	private static final String XPATH_ALL = "*";
	private static final String XPATH_EPISODE_LIST = "/Show/Episodelist/Season";
	private static final String XPATH_SEASON_NUM = "seasonnum";
	private static final String XPATH_SEASON_ATTR = "no";
	private static final String XPATH_TITLE = "title";

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
	public static ArrayList<Show> getShowOptions(String showName) throws ConnectException, UnknownHostException {
		ArrayList<Show> options = new ArrayList<Show>();
		showName = showName.replaceAll(" ", "%20");
		String searchURL = BASE_SEARCH_URL + showName;

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			URL url = new URL(searchURL);

			logger.info("About to retrieving search results from " + url.toString());

			InputStream inputStream = url.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			logger.finer("Before encoding XML");

			String s;
			String xml = "";
			while ((s = reader.readLine()) != null) {
				if (logger.isLoggable(Level.FINEST)) {
					logger.finest(s);
				}
				xml += encodeSpecialCharacters(s);
			}

			logger.finest("xml:\n" + xml);

			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(xml)));

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
		} catch (ConnectException ce) {
			logger.log(Level.WARNING, "ConnectionException when connecting to " + searchURL);
			throw ce;
		} catch (UnknownHostException uhe) {
			logger.log(Level.WARNING, "UnknownHostException when connecting to " + searchURL);
			throw uhe;
		} catch (Exception e) {
			logger.log(Level.WARNING, "Caught exception when attempting to download and parse search xml", e);
		}

		return options;
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
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(showURL);
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
					logger.fine("[" + sNum + "x" + epNumNode.getTextContent() + "] " + epTitleNode.getTextContent());
					season.setEpisode(Integer.parseInt(epNumNode.getTextContent()), epTitleNode.getTextContent());
				}
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "Caught exception when attempting to parse show detail xml", e);
		}
	}

	/**
	 * Replaces unsafe HTML Characters with HTML Entities
	 * 
	 * @param input
	 *            string to encode
	 * @return HTML safe representation of input
	 */
	private static String encodeSpecialCharacters(String input) {
		if (input == null || input.length() == 0) {
			return "";
		}

		// TODO: determine other characters that need to be replaced (eg "'", "-")
		logger.finest("Input before encoding: [" + input + "]");
		input = input.replaceAll("& ", "&amp; ");
		logger.finest("Input after encoding: [" + input + "]");
		return input;
	}
}
