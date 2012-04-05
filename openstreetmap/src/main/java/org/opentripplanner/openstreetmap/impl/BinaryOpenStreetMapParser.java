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
import org.opentripplanner.openstreetmap.model.*;

import java.util.List;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;

/**
 * Parser for the OpenStreetMap PBF Format.
 *
 * @since 0.4
 */
public class BinaryOpenStreetMapParser extends BinaryParser {
    private OpenStreetMapContentHandler _handler;
    private boolean _nodesOnly = false;
    private boolean _noNodes   = false;

    public BinaryOpenStreetMapParser(OpenStreetMapContentHandler handler) {
        _handler = handler;
    }

    /**
     * Should only nodes be parsed?
     *
     * @see org.opentripplanner.graph_builder.services/.sm.OpenStreetMapContentHandler#biPhase
     */
    public void nodesOnly(boolean nodesOnly) {
        _nodesOnly = nodesOnly;

        if(nodesOnly && _noNodes) {
            _noNodes = false;
        }
    }

    /**
     * Should only non-nodes (ways and relations) be parsed?
     *
     * @see org.opentripplanner.graph_builder.services/.sm.OpenStreetMapContentHandler#biPhase
     */
    public void noNodes(boolean noNodes) {
        _noNodes = noNodes;

        if(noNodes && _nodesOnly) {
            _nodesOnly = false;
        }
    }

    public void complete() {
        // Jump in circles
    }

    @Override
    protected void parseNodes(List<Osmformat.Node> nodes) {
        if(_noNodes) {
            return;
        }

        for (Osmformat.Node i : nodes) {
            OSMNode tmp = new OSMNode();
            tmp.setId(i.getId());
            tmp.setLat(parseLat(i.getLat()));
            tmp.setLon(parseLon(i.getLon()));

            for (int j = 0; j < i.getKeysCount(); j++) {
                String key = getStringById(i.getKeys(j)).intern();
                // if _handler.retain_tag(key) // TODO: filter tags
                String value = getStringById(i.getVals(j)).intern();
                OSMTag tag = new OSMTag();
                tag.setK(key);
                tag.setV(value);
                tmp.addTag(tag);
            }

            _handler.addNode(tmp);
        }
    }

    @Override
    protected void parseDense(Osmformat.DenseNodes nodes) {
        long lastId = 0, lastLat = 0, lastLon = 0;
        int j = 0; // Index into the keysvals array.

        if(_noNodes) {
            return;
        }

        for (int i = 0; i < nodes.getIdCount(); i++) {
            OSMNode tmp = new OSMNode();

            long lat = nodes.getLat(i) + lastLat;
            lastLat = lat;
            long lon = nodes.getLon(i) + lastLon;
            lastLon = lon;
            long id = nodes.getId(i) + lastId;
            lastId = id;
            double latf = parseLat(lat), lonf = parseLon(lon);

            tmp.setId(id);
            tmp.setLat(latf);
            tmp.setLon(lonf);

            // If empty, assume that nothing here has keys or vals.
            if (nodes.getKeysValsCount() > 0) {
                while (nodes.getKeysVals(j) != 0) {
                    int keyid = nodes.getKeysVals(j++);
                    int valid = nodes.getKeysVals(j++);

                    OSMTag tag = new OSMTag();
                    String key = getStringById(keyid).intern();
                    String value = getStringById(valid).intern();
                    tag.setK(key);
                    tag.setV(value);
                    tmp.addTag(tag);
                }
                j++; // Skip over the '0' delimiter.
            }

            _handler.addNode(tmp);
        }
    }

    @Override
    protected void parseWays(List<Osmformat.Way> ways) {
        if(_nodesOnly) {
            return;
        }

        for (Osmformat.Way i : ways) {
            OSMWay tmp = new OSMWay();
            tmp.setId(i.getId());

            for (int j = 0; j < i.getKeysCount(); j++) {
                OSMTag tag = new OSMTag();
                String key = getStringById(i.getKeys(j)).intern();
                String value = getStringById(i.getVals(j)).intern();
                tag.setK(key);
                tag.setV(value);
                tmp.addTag(tag);
            }

            long lastId = 0;
            for (long j : i.getRefsList()) {
                OSMNodeRef nodeRef = new OSMNodeRef();
                nodeRef.setRef(j + lastId);
                tmp.addNodeRef(nodeRef);

                lastId = j + lastId;
            }

            _handler.addWay(tmp);
        }
    }

    @Override
    protected void parseRelations(List<Osmformat.Relation> rels) {
        if(_nodesOnly) {
            return;
        }

        for (Osmformat.Relation i : rels) {
            OSMRelation tmp = new OSMRelation();
            tmp.setId(i.getId());

            for (int j = 0; j < i.getKeysCount(); j++) {
                OSMTag tag = new OSMTag();
                String key = getStringById(i.getKeys(j)).intern();
                String value = getStringById(i.getVals(j)).intern();
                tag.setK(key);
                tag.setV(value);
                tmp.addTag(tag);
            }

            long lastMid = 0;
            for (int j = 0; j < i.getMemidsCount(); j++) {
                OSMRelationMember relMember = new OSMRelationMember();
                long mid = lastMid + i.getMemids(j);

                relMember.setRef(mid);
                lastMid = mid;

                relMember.setRole(getStringById(i.getRolesSid(j)).intern());

                if (i.getTypes(j) == Osmformat.Relation.MemberType.NODE) {
                    relMember.setType("node");
                } else if (i.getTypes(j) == Osmformat.Relation.MemberType.WAY) {
                    relMember.setType("way");
                } else if (i.getTypes(j) == Osmformat.Relation.MemberType.RELATION) {
                    relMember.setType("relation");
                } else {
                    assert false; // TODO; Illegal file?
                }

                tmp.addMember(relMember);
            }

            _handler.addRelation(tmp);
        }
    }

    @Override
    public void parse(Osmformat.HeaderBlock block) {
        for (String s : block.getRequiredFeaturesList()) {
            if (s.equals("OsmSchema-V0.6")) {
                continue; // We can parse this.
            }
            if (s.equals("DenseNodes")) {
                continue; // We can parse this.
            }
            throw new IllegalStateException("File requires unknown feature: " + s);
        }
    }
}
