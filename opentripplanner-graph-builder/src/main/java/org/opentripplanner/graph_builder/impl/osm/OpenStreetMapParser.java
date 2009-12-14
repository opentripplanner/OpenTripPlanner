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

import org.apache.commons.digester.Digester;
import org.opentripplanner.graph_builder.model.osm.OSMNode;
import org.opentripplanner.graph_builder.model.osm.OSMNodeRef;
import org.opentripplanner.graph_builder.model.osm.OSMRelation;
import org.opentripplanner.graph_builder.model.osm.OSMRelationMember;
import org.opentripplanner.graph_builder.model.osm.OSMTag;
import org.opentripplanner.graph_builder.model.osm.OSMWay;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class OpenStreetMapParser {

    public void parseMap(File path, OpenStreetMapContentHandler map) throws IOException,
            SAXException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));
        parseMap(in,map);
    }

    public void parseMap(InputStream in, OpenStreetMapContentHandler map) throws IOException,
            SAXException {

        Digester d = new Digester();
        d.push(map);

        addNodeRules(d);
        addWayRules(d);
        addRelationRules(d);

        d.parse(in);
    }

    /****
     * Protected Methods
     ****/

    protected void addNodeRules(Digester d) {
        d.addObjectCreate("osm/node", OSMNode.class);
        d.addSetProperties("osm/node");
        d.addSetNext("osm/node", "addNode");

        addTagRules(d, "osm/node");
    }

    protected void addWayRules(Digester d) {
        d.addObjectCreate("osm/way", OSMWay.class);
        d.addSetProperties("osm/way");
        d.addSetNext("osm/way", "addWay");

        d.addObjectCreate("osm/way/nd", OSMNodeRef.class);
        d.addSetProperties("osm/way/nd");
        d.addSetNext("osm/way/nd", "addNodeRef");

        addTagRules(d, "osm/way");
    }

    protected void addRelationRules(Digester d) {

        d.addObjectCreate("osm/relation", OSMRelation.class);
        d.addSetProperties("osm/relation");
        d.addSetNext("osm/relation", "addRelation");

        d.addObjectCreate("osm/relation/member", OSMRelationMember.class);
        d.addSetProperties("osm/relation/member");
        d.addSetNext("osm/relation/member", "addMember");

        addTagRules(d, "osm/relation");
    }

    protected void addTagRules(Digester d, String prefix) {
        d.addObjectCreate(prefix + "/tag", OSMTag.class);
        d.addSetProperties(prefix + "/tag");
        d.addSetNext(prefix + "/tag", "addTag");
    }
}
