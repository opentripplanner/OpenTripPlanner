package org.opentripplanner.osm;

/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

import java.util.List;

import org.opentripplanner.osm.Relation.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;

/**
 * Parser for the OpenStreetMap PBF Format. Implements callbacks for the crosby.binary OSMPBF
 * library.
 */
public class Parser extends BinaryParser {

    private static final Logger LOG = LoggerFactory.getLogger(Parser.class);

    // no need to internalize strings. they will be serialized out to disk anyway.
    // private Map<String, String> stringTable = new HashMap<String, String>();

    private OSM osm;
    int nodeCount = 0;
    int wayCount = 0;
    
    public Parser(OSM osm) {
        this.osm = osm;
    }
    
    private static final String[] retainKeys = new String[] {
        "highway", "parking", "bicycle"
    };

    private boolean retainTag(String key) {
        for (String s : retainKeys) {
            if (s.equals(key)) return true;
        }
        // Accepting all tags increases size by < 1/10
        // when storing all elements.
        // not storing elements that lack interesting tags
        // reduces size by 80%.
        //return false;
        return true;
    }

    private void addTag(StringBuilder sb, String key, String val) {
//        if (retainTag(key)) {
            if (sb.length() > 0) sb.append(';');
            sb.append(key);
            if (val != null && ! val.isEmpty()) {
                sb.append('=');
                sb.append(val);
            }
//        }
    }

    @Override
    protected void parseNodes(List<Osmformat.Node> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Osmformat.Node n : nodes) {
            if (nodeCount++ % 100000 == 0) {
                LOG.info("node {}", human(nodeCount));
            }
            Node node = new Node();
            node.lat = (float) parseLat(n.getLat());
            node.lon = (float) parseLon(n.getLon());
            sb.setLength(0); // empty buffer
            for (int k = 0; k < n.getKeysCount(); k++) {
                String key = getStringById(n.getKeys(k));
                String val = getStringById(n.getVals(k));
                addTag(sb, key, val);
            }
            node.tags = sb.toString();
            // Store nodes regardless of whether they have tags
            osm.nodes.put(n.getId(), node);
        }
    }

    @Override
    protected void parseDense(Osmformat.DenseNodes nodes) {
        long lastId = 0, lastLat = 0, lastLon = 0;
        int kv = 0; // index into the keysvals array
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < nodes.getIdCount(); n++) {
            if (nodeCount++ % 100000 == 0) {
                LOG.info("node {}", human(nodeCount));
            }
            Node node = new Node();
            long id  = nodes.getId(n)  + lastId;
            long lat = nodes.getLat(n) + lastLat;
            long lon = nodes.getLon(n) + lastLon;
            lastId  = id;
            lastLat = lat;
            lastLon = lon;
            node.lat = (float) parseLat(lat);
            node.lon = (float) parseLon(lon);
            // Check whether any node has tags.
            if (nodes.getKeysValsCount() > 0) {
                sb.setLength(0); // empty buffer
                while (nodes.getKeysVals(kv) != 0) {
                    int kid = nodes.getKeysVals(kv++);
                    int vid = nodes.getKeysVals(kv++);
                    String key = getStringById(kid);
                    String val = getStringById(vid);
                    addTag(sb, key, val);
                }
                kv++; // Skip over the '0' delimiter.
            }
            node.tags = sb.toString();
            // Store nodes regardless of whether they have tags
            osm.nodes.put(id, node);
        }
    }

    @Override
    protected void parseWays(List<Osmformat.Way> ways) {
        StringBuilder sb = new StringBuilder();
        for (Osmformat.Way w : ways) {
            if (wayCount++ % 50000 == 0) {
                LOG.info("way {}", human(wayCount));
            }
            Way way = new Way();
            /* Handle tags */
            sb.setLength(0); // empty buffer
            for (int k = 0; k < w.getKeysCount(); k++) {
                String key = getStringById(w.getKeys(k));
                String val = getStringById(w.getVals(k));
                addTag(sb, key, val);
            }
            way.tags = sb.toString();
            /* Handle nodes */
            List<Long> rl = w.getRefsList();
            long[] nodes = new long[rl.size()];
            long ref = 0;
            for (int n = 0; n < nodes.length; n++) {
                ref += rl.get(n);
                nodes[n] = ref;
            }
            way.nodes = nodes;
            if (!way.tagless()) {
                osm.ways.put(w.getId(), way);
            }
        }
    }

    @Override
    protected void parseRelations(List<Osmformat.Relation> rels) {
        StringBuilder sb = new StringBuilder();
        for (Osmformat.Relation r : rels) {
            Relation rel = new Relation();
            sb.setLength(0);
            /* Handle Tags */
            for (int k = 0; k < r.getKeysCount(); k++) {
                String key = getStringById(r.getKeys(k));
                String val = getStringById(r.getVals(k));
                addTag(sb, key, val);
            }
            rel.tags = sb.toString();
            /* Handle members of the relation */
            long mid = 0; // member ids, delta coded
            for (int m = 0; m < r.getMemidsCount(); m++) {
                Relation.Member member = new Relation.Member();
                mid += r.getMemids(m);
                member.id = mid;
                member.role = getStringById(r.getRolesSid(m));
                switch (r.getTypes(m)) {
                case NODE:
                    member.type = Type.NODE;
                    break;
                case WAY:
                    member.type = Type.WAY;
                    break;
                case RELATION:
                    member.type = Type.RELATION;
                    break;
                default:
                    LOG.error("Relation type is unexpected.");
                }
                rel.members.add(member);
            }
            if (!rel.tagless()) {
                osm.relations.put(r.getId(), rel);
            }
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

    @Override
    public void complete() {
        LOG.info("Done loading PBF.");
    }

    private static String human(int n) {
        if (n > 1000000)
            return String.format("%.1fM", n / 1000000.0);
        if (n > 1000)
            return String.format("%dk", n / 1000);
        else
            return String.format("%d", n);
    }

}
