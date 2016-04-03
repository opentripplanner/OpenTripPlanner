/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.util.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 * Helper class to build a list of objects out of generic XML data.
 * 
 * @param <T> The class of the data elements that will be built.
 */
public class XmlDataListDownloader<T> {

    public interface XmlDataFactory<T> {
        public T build(Map<String, String> attributes);
    }

    private static final Logger LOG = LoggerFactory.getLogger(XmlDataListDownloader.class);

    private String path;

    private XPathExpression xpathExpr;

    private XmlDataFactory<T> dataFactory;

    // if true, read attributes of elements, instead of the text of their child elements
    private boolean readAttributes = false;

    public void setReadAttributes(boolean readAttributes) {
        this.readAttributes = readAttributes;
    }

    public void setPath(String path) {
        this.path = path;
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        try {
            xpathExpr = xpath.compile(path);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDataFactory(XmlDataFactory<T> dataFactory) {
        this.dataFactory = dataFactory;
    }

    public List<T> download(String url, boolean zip) {
        try {
            InputStream inputStream;
            URL url2 = new URL(url);
            String proto = url2.getProtocol();
            if (proto.equals("http") || proto.equals("https")) {
                inputStream = HttpUtils.getData(url);
            } else {
                // Local file probably, try standard java
                inputStream = url2.openStream();
            }
            if (inputStream == null) {
                LOG.warn("Failed to get data from url " + url);
                return null;
            } else if (zip) {
                ZipInputStream zipInputStream = new ZipInputStream(inputStream);
                zipInputStream.getNextEntry();
                inputStream = zipInputStream;
            }
            return parseXML(inputStream);
        } catch (IOException e) {
            LOG.warn("Error reading XML feed from " + url, e);
            return null;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            LOG.warn("Error parsing XML feed from " + url + "(bad XML of some sort)", e);
            return null;
        }
    }

    private List<T> parseXML(InputStream data) throws ParserConfigurationException, SAXException,
            IOException {
        List<T> out = new ArrayList<T>();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); // never forget this!
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(data);

        NodeList nodes;
        try {
            Object result = xpathExpr.evaluate(doc, XPathConstants.NODESET);
            nodes = (NodeList) result;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < nodes.getLength(); ++i) {
            Node node = nodes.item(i);
            if (!(node instanceof Element)) {
                continue;
            }
            HashMap<String, String> attributes = new HashMap<String, String>();
            Node child = node.getFirstChild();

            if (readAttributes) {
                // read XML attributes of selected nodes
                NamedNodeMap attrs = node.getAttributes();
                int lastAttr = attrs.getLength() - 1;
                for (int j = lastAttr; j--> 0; ) {
                    Attr attr = (Attr) attrs.item(j);
                    attributes.put(attr.getNodeName(), attr.getNodeValue());
                }
            } else {
                // read text of child nodes
                while (child != null) {
                    if (!(child instanceof Element)) {
                        child = child.getNextSibling();
                        continue;
                    }
                    attributes.put(child.getNodeName(), child.getTextContent());
                    child = child.getNextSibling();
                }
            }

            T t = dataFactory.build(attributes);
            if (t != null)
                out.add(t);
        }
        return out;
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + path + ")";
    }
}
