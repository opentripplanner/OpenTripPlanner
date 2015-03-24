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

import crosby.binary.BinaryParser;
import crosby.binary.Osmformat;
import crosby.binary.file.BlockInputStream;
import org.opentripplanner.osm.Relation.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Parser for the OpenStreetMap PBF Format. Implements callbacks for the crosby.binary OSMPBF
 * library. It loads OSM into the OTP model classes, then defers to implementations of handleNode,
 * handleWay, and handleRelation. Each block of a PBF file can be of a different type, so if we want
 * to examine nodes, then ways we must parse the entire file several times. This is just the nature
 * of OSM PBF.
 *
 * Subclasses of Parser that wish to skip certain OSM element types should override parseWays,
 * parseDense, etc. rather than the corresponding handle* methods to avoid ever converting the
 * low-level PBF objects into objects using OTP's internal OSM model.
 */
public class Parser extends BinaryParser {

    protected static final Logger LOG = LoggerFactory.getLogger(Parser.class);

    OSM osm;
    // no need to internalize strings. they will be serialized out to disk anyway.
    // private Map<String, String> stringTable = new HashMap<String, String>();    
    long nodeCount = 0;
    long wayCount = 0;

    public Parser () {
        osm = new OSM(null);
    }

    public Parser (String diskPath) {
        osm = new OSM(diskPath);
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
        // Not storing elements that lack interesting tags
        // reduces size by 80%.
        // return true;
        return true;
    }

    // Load ways first, then skip loading all nodes which are not tracked.
    // Also include bounding box filter.

    // move to Tagged
    private void addTag(StringBuilder sb, String key, String val) {
        if (retainTag(key)) {
            if (sb.length() > 0) sb.append(';');
            sb.append(key);
            if (val != null && ! val.isEmpty()) {
                sb.append('=');
                sb.append(val);
            }
        }
    }

    /** Note that in many PBF files this never gets called because nodes are dense. */
    @Override
    protected void parseNodes(List<Osmformat.Node> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Osmformat.Node n : nodes) {
            if (nodeCount++ % 10000000 == 0) {
                LOG.info("node {}", human(nodeCount));
            }
            Node node = new Node(parseLat(n.getLat()), parseLon(n.getLon()));
            sb.setLength(0); // empty buffer
            for (int k = 0; k < n.getKeysCount(); k++) {
                String key = getStringById(n.getKeys(k));
                String val = getStringById(n.getVals(k));
                addTag(sb, key, val);
            }
            node.tags = sb.toString();
            handleNode(n.getId(), node);
        }
    }

    @Override
    protected void parseDense(Osmformat.DenseNodes nodes) {
        long lastId = 0, lastLat = 0, lastLon = 0;
        int kv = 0; // index into the keysvals array
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < nodes.getIdCount(); n++) {
            if (nodeCount++ % 5000000 == 0) {
                LOG.info("node {}", human(nodeCount));
            }
            Node node = new Node();
            long id  = nodes.getId(n)  + lastId;
            long lat = nodes.getLat(n) + lastLat;
            long lon = nodes.getLon(n) + lastLon;
            lastId  = id;
            lastLat = lat;
            lastLon = lon;
            node.setLatLon(parseLat(lat), parseLon(lon));
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
            handleNode(id, node);
        }
    }

    @Override
    protected void parseWays(List<Osmformat.Way> ways) {
        StringBuilder sb = new StringBuilder();
        for (Osmformat.Way w : ways) {
            if (wayCount++ % 1000000 == 0) {
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
            handleWay(w.getId(), way);
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
            handleRelation(r.getId(), rel);
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
        LOG.info("Done parsing PBF.");
    }

    private static String human(long n) {
        if (n > 1000000)
            return String.format("%.1fM", n / 1000000.0);
        if (n > 1000)
            return String.format("%dk", n / 1000);
        else
            return String.format("%d", n);
    }

    /** Open the given OSM PBF file and run this parser on it. */
    public void parse(String filename) {
        try {
            FileInputStream input = new FileInputStream(filename);
            new BlockInputStream(input, this).process();
            input.close();
        } catch (IOException e) {
            throw new RuntimeException("Error parsing OSM PBF.", e);
        }
    }
    
    /** 
     * Override this method to tell the parser what to do to with each node,
     * once it has been parsed into OTP's internal OSM model.
     */
    public void handleNode(long id, Node node) {
        osm.nodes.put(id, node);
    };

    /** 
     * Override this method to tell the parser what to do to with each way,
     * once it has been parsed into OTP's internal OSM model.
     */
    public void handleWay(long id, Way way) {
        osm.ways.put(id, way);
    };

    /** 
     * Override this method to tell the parser what to do to with each relation,
     * once it has been parsed into OTP's internal OSM model.
     */
    public void handleRelation(long id, Relation relation) {
        osm.relations.put(id, relation);
    };
    
}
