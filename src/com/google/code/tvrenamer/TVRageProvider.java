package com.google.code.tvrenamer;

import java.io.IOException;
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
import org.xml.sax.SAXException;

public class TVRageProvider {
  private static Logger logger = Logger.getLogger(TVRenamer.class);

  public static ArrayList<Show> getShowOptions(String showName) {
    ArrayList<Show> options = new ArrayList<Show>();

    // convert spaces to %20s, not necessary but nicer
    String searchURL = "http://www.tvrage.com/feeds/search.php?show="
        + showName.replace(" ", "%20");
    // TODO: determine other characters that need to be replaced (eg "'", "-")

    // logger.debug(searchURL);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(searchURL);

      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      XPathExpression expr = xpath.compile("//show");

      NodeList shows = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

      for (int i = 0; i < shows.getLength(); i++) {
        Node eNode = shows.item(i);
        expr = xpath.compile("showid");
        String optionId = ((Node) expr.evaluate(eNode, XPathConstants.NODE))
            .getTextContent();
        expr = xpath.compile("name");
        String optionName = ((Node) expr.evaluate(eNode, XPathConstants.NODE))
            .getTextContent();
        expr = xpath.compile("link");
        String optionUrl = ((Node) expr.evaluate(eNode, XPathConstants.NODE))
            .getTextContent();
        options.add(new Show(optionId, optionName, optionUrl));
      }

      /*
       * old code here:
       * 
       * NodeList shows = dom.getElementsByTagName("show"); for (int i = 0; i <
       * shows.getLength(); i++) { Node show = shows.item(i); String optionId =
       * show.getChildNodes().item(1).getTextContent(); String optionName =
       * show.getChildNodes().item(3).getTextContent(); String optionUrl =
       * show.getChildNodes().item(5).getTextContent(); options.add(new
       * Show(optionId, optionName, optionUrl)); // logger.debug(optionId + " -> " +
       * optionName); }
       */

    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }

    return options;
  }

  public static void getShowListing(Show show) {

    String showURL = "http://www.tvrage.com/feeds/episode_list.php?sid="
        + show.getId();

    // logger.debug(showURL);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(showURL);
      XPathFactory factory = XPathFactory.newInstance();
      XPath xpath = factory.newXPath();

      XPathExpression expr = xpath
          .compile("//Episodelist/*[starts-with(name(), 'Season')]");

      NodeList seasons = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

      for (int i = 0; i < seasons.getLength(); i++) {
        Node sNode = seasons.item(i);
        String nodeName = sNode.getNodeName().toLowerCase();
        String sNum = nodeName.replace("season", "");
        Season season = new Season(sNum);
        show.setSeason(sNum, season);

        expr = xpath.compile("*");
        NodeList episodes = (NodeList) expr.evaluate(sNode,
            XPathConstants.NODESET);
        for (int j = 0; j < episodes.getLength(); j++) {
          Node eNode = episodes.item(j);
          expr = xpath.compile("seasonnum");
          Node epNumNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
          expr = xpath.compile("title");
          Node epTitleNode = (Node) expr.evaluate(eNode, XPathConstants.NODE);
          // logger.debug("[" + sNum + "x" + epNumNode.getTextContent() + "] "
          // + epTitleNode.getTextContent());
          season.setEpisode(epNumNode.getTextContent(), epTitleNode
              .getTextContent());
        }
      }

      /*
       * old code here:
       * 
       * NodeList seasons = dom.getChildNodes().item(0).getChildNodes().item(5)
       * .getChildNodes();
       * 
       * for (int i = 0; i < seasons.getLength(); i++) {
       * logger.debug(seasons.item(i).getNodeName()); }
       * 
       * for (int i = 1; i < seasons.getLength(); i += 2) { Node node =
       * seasons.item(i); String nodeName = node.getNodeName(); if
       * (nodeName.toLowerCase().startsWith("season")) {
       * 
       * String sNum = nodeName.substring(6); Season season = new Season(sNum);
       * listing.put(sNum, season); logger.debug("" + i + " -> " + sNum);
       * 
       * NodeList children = node.getChildNodes();
       * 
       * for (int j = 1; j < children.getLength(); j += 2) { NodeList childs =
       * children.item(j).getChildNodes(); logger.debug("\t" + j + " -> " +
       * children.item(j).getNodeName()); String epNum =
       * childs.item(2).getTextContent(); String title =
       * childs.item(6).getTextContent(); logger.debug("\t\t" + epNum + " -> " +
       * title); season.setEpisode(epNum, title); } } }
       */

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
}
