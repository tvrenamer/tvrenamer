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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.code.tvrenamer.model.Season;
import com.google.code.tvrenamer.model.Show;
import com.google.code.tvrenamer.model.util.Constants;
import com.google.code.tvrenamer.view.UIStarter;

public class TVRageProvider {
	private static Logger logger = Logger.getLogger(TVRenamer.class);
	private static final String BASE_SEARCH_URL = "http://www.tvrage.com/feeds/search.php?show=";
	private static final String XPATH_SHOW = "//show";
	private static final String XPATH_SHOWID = "showid";
	private static final String XPATH_NAME = "name";
	private static final String XPATH_LINK = "link";

	private static final String BASE_LIST_URL = "http://www.tvrage.com/feeds/search.php?show=";
	private static final String XPATH_ALL = "*";
	private static final String XPATH_EPISODE_LIST = "//Episodelist/*[starts-with(name(), 'Season')]";
	private static final String XPATH_SEASON_NUM = "seasonnum";
	private static final String XPATH_TITLE = "title";

	public static ArrayList<Show> getShowOptions(String showName) {
		ArrayList<Show> options = new ArrayList<Show>();

		logger.debug(BASE_SEARCH_URL + showName);
		String searchURL = BASE_SEARCH_URL + encodeSpecialCharacters(showName);

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

			URL url = new URL(searchURL);
			logger.debug("The show URL is: " + url.toString());
			InputStream inputStream = url.openStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			String s;
			String xml = "";
			while ((s = reader.readLine()) != null) {
				xml += s;
			}
			logger.debug("Before encoding XML");
			s = encodeSpecialCharacters(s);

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
		}
		catch (UnknownHostException e) {
			UIStarter.showMessageBox(Constants.ERROR,
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

	public static void getShowListing(Show show) {

		String showURL = BASE_LIST_URL + show.getId();

		// logger.debug(showURL);

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
				String nodeName = sNode.getNodeName().toLowerCase();
				String sNum = nodeName.replace("season", "");
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
					logger.debug("[" + sNum + "x" + epNumNode.getTextContent() + "] "
					 + epTitleNode.getTextContent());
					season.setEpisode(epNumNode.getTextContent(), epTitleNode.getTextContent());
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

	private static String encodeSpecialCharacters(String input) {
		if(input == null || input.length() == 0) {
			return "";
		}

		// TODO: determine other characters that need to be replaced (eg "'", "-")
		logger.debug("Input before encoding: [" +  input + "]");
		input = input.replaceAll("& ", "&amp; ");
		input = input.replaceAll(" ", "%20");
		logger.debug("Input after encoding: [" +  input + "]");
		return input;
	}
}
