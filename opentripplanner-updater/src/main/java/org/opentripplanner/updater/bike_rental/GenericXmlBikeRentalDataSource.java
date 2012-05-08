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

package org.opentripplanner.updater.bike_rental;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class GenericXmlBikeRentalDataSource implements BikeRentalDataSource {

    private static final Logger log = LoggerFactory.getLogger(BixiBikeRentalDataSource.class);

    private String url;

    ArrayList<BikeRentalStation> stations = new ArrayList<BikeRentalStation>();

    private XPathExpression xpathExpr;

    public GenericXmlBikeRentalDataSource(String path) {
        XPathFactory factory = XPathFactory.newInstance();

        XPath xpath = factory.newXPath();
        try {
            xpathExpr = xpath.compile(path);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean update() {
        try {
            InputStream data = HttpUtils.getData(url);
            if (data == null) {
                throw new RuntimeException("Failed to get data from url " + url);
            }
            parseXML(data);
        } catch (IOException e) {
            log.warn("Eror reading bike rental feed from " + url, e);
            return false;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            log.warn("Eror parsing bike rental feed from " + url + "(bad XML of some sort)", e);
            return false;
        }
        return true;
    }

    private void parseXML(InputStream data) throws ParserConfigurationException, SAXException,
            IOException {
        ArrayList<BikeRentalStation> out = new ArrayList<BikeRentalStation>();

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
            while (child != null) {
                if (!(child instanceof Element)) {
                    child = child.getNextSibling();
                    continue;
                }
                attributes.put(child.getNodeName(), child.getTextContent());
                child = child.getNextSibling();
            }
            BikeRentalStation brstation = makeStation(attributes);
            if (brstation != null)
                out.add(brstation);
        }
        synchronized(this) {
            stations = out;
        }
    }

    @Override
    public synchronized List<BikeRentalStation> getStations() {
        return stations;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public abstract BikeRentalStation makeStation(Map<String, String> attributes);

}
