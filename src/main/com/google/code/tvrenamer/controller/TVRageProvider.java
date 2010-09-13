package com.google.code.tvrenamer.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.view.UIStarter;

/**
 * This class encapsulates the interactions between the application and the
 * TVRage XML Feeds
 * 
 * @author Vipul Delwadia
 * @author Dave Harris
 * 
 */
public class TVRageProvider {
	// private static Logger logger = Logger.getLogger(TVRageProvider.class);
	private static final String BASE_SEARCH_URL = "http://www.tvrage.com/feeds/search.php?show=";
	private static final String XPATH_SHOW = "//show";
	private static final String XPATH_SHOWID = "showid";
	private static final String XPATH_NAME = "name";
	private static final String XPATH_LINK = "link";

	private TVRageProvider() {
		// Prevents instantiation
	}

	/**
	 * Uses the TVRage search tool to retrieve a list of possible shows based on
	 * the show name. The list returned is in the order returned by the TVRage
	 * search.
	 * 
	 * @param showName
	 *            the show to search for
	 * @return a list of matching shows in the order returned by the TVRage
	 *         search.
	 */
	public static ArrayList<Show> getShowOptions(String showName) {
		ArrayList<Show> options = new ArrayList<Show>();
		showName = showName.replaceAll(" ", "%20");
		// logger.debug(BASE_SEARCH_URL + showName);
		String searchURL = BASE_SEARCH_URL + showName;

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			URL url = new URL(searchURL);

			// logger.info("Retrieving search results from \"" + url.toString() +
			// "\"");
			InputStream inputStream = url.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			// logger.debug("Before encoding XML");

			String s;
			String xml = "";
			while ((s = reader.readLine()) != null) {
				// logger.debug(s);
				xml += encodeSpecialCharacters(s);
			}

			// logger.debug("xml:\n" + xml);

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
		} catch (UnknownHostException e) {
			UIStarter.showMessageBox(Constants.SWTMessageBoxType.ERROR,
									 "Unable to connect to http://www.tvrage.com, check your internet connection.");
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return options;
	}

	private static final String BASE_LIST_URL = "http://www.tvrage.com/feeds/episode_list.php?sid=";
	private static final String XPATH_ALL = "*";
	private static final String XPATH_EPISODE_LIST = "/Show/Episodelist/Season";
	private static final String XPATH_SEASON_NUM = "seasonnum";
	private static final String XPATH_SEASON_ATTR = "no";
	private static final String XPATH_TITLE = "title";

	/**
	 * Uses the TVRage episode listings to populate the Show object with Season
	 * and Episode Data
	 * 
	 * @param show
	 *            the Show object to populate with season and episode data
	 */
	public static void getShowListing(Show show) {

		String showURL = BASE_LIST_URL + show.getId();

		// logger.info("Retrieving episode listing from " + showURL);

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
					// logger.debug("[" + sNum + "x" + epNumNode.getTextContent() + "] "
					// + epTitleNode.getTextContent());
					season.setEpisode(Integer.parseInt(epNumNode.getTextContent()), epTitleNode.getTextContent());
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
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
		// logger.debug("Input before encoding: [" + input + "]");
		input = input.replaceAll("& ", "&amp; ");
		// logger.debug("Input after encoding: [" + input + "]");
		return input;
	}
}
