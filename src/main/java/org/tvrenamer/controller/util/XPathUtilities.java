package org.tvrenamer.controller.util;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class XPathUtilities {

    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    // We just create this one XPath object and use it for all shows, all episodes.
    // TODO: is there any issue with this?
    private static final XPath STD_XPATH = XPATH_FACTORY.newXPath();

    public static NodeList nodeListValue(String name, Node eNode)
        throws XPathExpressionException
    {
        XPathExpression expr = STD_XPATH.compile(name);
        return (NodeList) expr.evaluate(eNode, XPathConstants.NODESET);
    }

    public static String nodeTextValue(String name, Node eNode)
        throws XPathExpressionException
    {
        XPathExpression expr = STD_XPATH.compile(name);
        Node node = (Node) expr.evaluate(eNode, XPathConstants.NODE);
        if (node == null) {
            return null;
        }
        return node.getTextContent();
    }
}
