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

package org.opentripplanner.openstreetmap.impl;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMNodeRef;
import org.opentripplanner.openstreetmap.model.OSMRelation;
import org.opentripplanner.openstreetmap.model.OSMRelationMember;
import org.opentripplanner.openstreetmap.model.OSMTag;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class OpenStreetMapParser {

    public void parseMap(File path, OpenStreetMapContentHandler map) throws IOException,
            SAXException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));
        parseMap(in, map);
    }

    public void parseMap(InputStream in, OpenStreetMapContentHandler map) throws IOException,
            SAXException {

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(in);
            processDocument(doc, map, 1);
            map.doneFirstPhaseRelations();
            processDocument(doc, map, 2);
            map.doneSecondPhaseWays();
            processDocument(doc, map, 3);
            map.doneThirdPhaseNodes();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void processDocument(Document doc, OpenStreetMapContentHandler map, int phase) {
        Node osm = doc.getFirstChild();
        Node node = osm.getFirstChild();
        while (node != null) {
            if (!(node instanceof Element)) {
                node = node.getNextSibling();
                continue;
            }
            Element element = (Element) node;
            if (phase == 3 && element.getTagName().equals("node")) {
                OSMNode osmNode = new OSMNode();

                osmNode.setId(Long.parseLong(element.getAttribute("id")));
                osmNode.lat = Double.parseDouble(element.getAttribute("lat"));
                osmNode.lon = Double.parseDouble(element.getAttribute("lon"));

                processTags(osmNode, element);
                map.addNode(osmNode);
            } else if (phase == 2 && element.getTagName().equals("way")) {
                OSMWay osmWay = new OSMWay();
                osmWay.setId(Long.parseLong(element.getAttribute("id")));
                processTags(osmWay, element);

                Node node2 = element.getFirstChild();
                while (node2 != null) {
                    if (!(node2 instanceof Element)) {
                        node2 = node2.getNextSibling();
                        continue;
                    }
                    Element element2 = (Element) node2;
                    if (element2.getNodeName().equals("nd")) {
                        OSMNodeRef nodeRef = new OSMNodeRef();
                        nodeRef.setRef(Long.parseLong(element2.getAttribute("ref")));
                        osmWay.addNodeRef(nodeRef);
                    }
                    node2 = node2.getNextSibling();
                }

                map.addWay(osmWay);
            } else if (phase == 1 && element.getTagName().equals("relation")) {
                OSMRelation osmRelation = new OSMRelation();
                osmRelation.setId(Long.parseLong(element.getAttribute("id")));
                processTags(osmRelation, element);

                Node node2 = element.getFirstChild();
                while (node2 != null) {
                    if (!(node2 instanceof Element)) {
                        node2 = node2.getNextSibling();
                        continue;
                    }
                    Element element2 = (Element) node2;
                    if (element2.getNodeName().equals("member")) {
                        OSMRelationMember member = new OSMRelationMember();
                        member.setRole(element2.getAttribute("role"));
                        member.setType(element2.getAttribute("type"));
                        member.setRef(Long.parseLong(element2.getAttribute("ref")));
                        osmRelation.addMember(member);
                    }
                    node2 = node2.getNextSibling();
                }
                map.addRelation(osmRelation);
            }
            node = node.getNextSibling();
        }
    }

    private void processTags(OSMWithTags osm, Element element) {
        Node node = element.getFirstChild();
        while (node != null) {
            if (!(node instanceof Element)) {
                node = node.getNextSibling();
                continue;
            }
            Element child = (Element) node;
            if (child.getTagName().equals("tag")) {
                OSMTag tag = new OSMTag();
                String key = child.getAttribute("k");
                tag.setK(key.intern());
                String value = child.getAttribute("v");
                if (key.equals("name") || key.equals("highway")) {
                    value = value.intern();
                }
                tag.setV(value);
                osm.addTag(tag);
            }
            node = node.getNextSibling();
        }
    }

}
