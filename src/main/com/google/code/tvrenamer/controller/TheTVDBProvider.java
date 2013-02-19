package com.google.code.tvrenamer.controller;

import java.io.IOException;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.code.tvrenamer.controller.util.StringUtils;
import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.TVRenamerIOException;

public class TheTVDBProvider {
	public static final String IMDB_BASE_URL = "http://www.imdb.com/title/";

	private static final String ERROR_PARSING_XML = "Error parsing XML";
	private static final String ERROR_DOWNLOADING_SHOW_INFORMATION = "Error downloading show information. Check internet or proxy settings";

	private static Logger logger = Logger.getLogger(TheTVDBProvider.class.getName());

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
				expr = xpath.compile(XPATH_IMDB);
				String optionUrl = IMDB_BASE_URL + ((Node) expr.evaluate(eNode, XPathConstants.NODE)).getTextContent();
				options.add(new Show(optionId, optionName, optionUrl));
			}
			Collections.sort(options);
			return options;
		} catch (ConnectException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
		} catch (UnknownHostException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
		} catch (ParserConfigurationException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (XPathExpressionException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (SAXException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		}
	}

	public static void getShowListing(Show show) throws TVRenamerIOException {
		String showURL = BASE_LIST_URL + show.getId() + BASE_LIST_FILENAME;

		logger.info("Retrieving episode listing from " + showURL);

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
					season = new Season(seasonNum);
					show.setSeason(seasonNum, season);
				}

				expr = xpath.compile(XPATH_EPISODE_NUM);
				Node epNumNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
				expr = xpath.compile(XPATH_EPISODE_NAME);
				Node epNameNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
				expr = xpath.compile(XPATH_AIRDATE);
				Node airdateNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
				logger.finer("[" + seasonNumNode.getTextContent() + "x" + epNumNode.getTextContent() + "] "
					+ epNameNode.getTextContent());
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				season.addEpisode(Integer.parseInt(epNumNode.getTextContent()), epNameNode.getTextContent(),
								  df.parse(airdateNode.getTextContent()));
			}
		} catch (ConnectException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
		} catch (UnknownHostException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_DOWNLOADING_SHOW_INFORMATION, e);
		} catch (ParserConfigurationException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (XPathExpressionException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (SAXException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (IOException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (NumberFormatException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (DOMException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		} catch (ParseException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
			throw new TVRenamerIOException(ERROR_PARSING_XML, e);
		}
	}

}
