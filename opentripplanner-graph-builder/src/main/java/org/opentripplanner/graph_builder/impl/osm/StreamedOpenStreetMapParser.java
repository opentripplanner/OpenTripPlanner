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
 
package org.opentripplanner.graph_builder.impl.osm;

import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMNodeRef;
import org.opentripplanner.graph_builder.model.osm.OSMTag;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * @author Vincent Privat
 * @since 1.0
 */
public class StreamedOpenStreetMapParser {

    private static final QName qNode = new QName("node");
    private static final QName qWay  = new QName("way");
    private static final QName qNd   = new QName("nd");
    private static final QName qTag  = new QName("tag");
    
    private static final QName qId   = new QName("id");
    private static final QName qLat  = new QName("lat");
    private static final QName qLon  = new QName("lon");
    private static final QName qRef  = new QName("ref");
    private static final QName qKey  = new QName("k");
    private static final QName qVal  = new QName("v");
    
    public static void parseMap(final File path, OpenStreetMapContentHandler map) throws IOException,
            XMLStreamException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));
        parseMap(in, map);
    }

    public static void parseMap(final InputStream in, OpenStreetMapContentHandler map) throws IOException,
            XMLStreamException {

        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLEventReader xmlEventReader = inputFactory.createXMLEventReader(in);

        OSMNode osmNode = null;
        OSMWay  osmWay  = null;

        while (xmlEventReader.hasNext()) {
            /* todo: process relations */
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            if (xmlEvent.isStartElement()) {
                StartElement element = xmlEvent.asStartElement();
                if (element.getName().equals(qNode)) {
                    osmNode = new OSMNode();
                    osmNode.setId(Integer.parseInt(element.getAttributeByName(qId).getValue()));
                    osmNode.setLat(Double.parseDouble(element.getAttributeByName(qLat).getValue()));
                    osmNode.setLon(Double.parseDouble(element.getAttributeByName(qLon).getValue()));
                    
                } else if (element.getName().equals(qWay)) {
                    osmWay = new OSMWay();
                    
                } else if (element.getName().equals(qNd)) {
                    OSMNodeRef nodeRef = new OSMNodeRef();
                    nodeRef.setRef(Integer.parseInt(element.getAttributeByName(qRef).getValue()));
                    osmWay.addNodeRef(nodeRef);
                    
                } else if (element.getName().equals(qTag)) {
                    OSMTag tag = new OSMTag();
                    String key = element.getAttributeByName(qKey).getValue();
                    tag.setK(key.intern());
                    String value = element.getAttributeByName(qVal).getValue();
                    if (key.equals("name") || key.equals("highway")) {
                        value = value.intern();
                    }
                    tag.setV(value);
                    if (osmNode != null) {
                        osmNode.addTag(tag);
                    } else if (osmWay != null) {
                        osmWay.addTag(tag);
                    }
                }

            } else if (xmlEvent.isEndElement()) {
                EndElement element = xmlEvent.asEndElement();
                if (element.getName().equals(qNode)) {
                    map.addNode(osmNode);
                    osmNode = null;
                }  else if (element.getName().equals(qWay)) {
                    map.addWay(osmWay);
                    osmWay = null;
                }
            }
        }

        xmlEventReader.close();
    }
}
