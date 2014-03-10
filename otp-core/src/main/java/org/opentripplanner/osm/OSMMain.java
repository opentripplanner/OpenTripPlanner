package org.opentripplanner.osm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.io.WKTWriter;

// Compressed DB is about 1/3 smaller than uncompressed.
// Compact operation only reduces uncompressed DB size by about 1/6.
// Compact operation does not reduce compressed DB size at all.

public class OSMMain {

    private static final Logger LOG = LoggerFactory.getLogger(OSMMain.class);
    static final String INPUT = "/var/otp/graphs/nl2/netherlands-latest.osm.pbf";
    static final String DB = "/home/abyrd/tmp/osm-mapdb";

    public static void main(String[] args) {
        File dbFile = new File(DB);
        if (dbFile.exists()) {
            LOG.error("Target file already exists.");
            System.exit(0);
        }
        DB db = DBMaker.newFileDB(dbFile)
                .transactionDisable()
                .asyncWriteEnable()
                .compressionEnable()
                .make();
        LOG.info("Reading PBF file '{}'", INPUT);
        OSM osm = new OSM();
        // Using DB TreeMaps is not really slower, so why not.
        // It lets you run in 400MB instead of a few GB.
        osm.nodes = db.getTreeMap("nodes");
        osm.ways = db.getTreeMap("ways");
        osm.relations = db.getTreeMap("relations");
//        osm.nodes = Maps.newHashMap();
//        osm.ways = Maps.newHashMap();
//        osm.relations = Maps.newHashMap();
        
        
        LOG.info("Finding nodes within the bounding geometry.");
        NodeGeomFilter ngf = new NodeGeomFilter(52.2, 4.4, 53.3, 5.5);
        ngf.parse(INPUT);
        LOG.info("Loading ways containing nodes found within the bounding geometry.");
        WayLoader wl = new WayLoader(osm, ngf.nodes);
        //WayLoader wl = new WayLoader(osm, NodeTracker.acceptEverything());
        wl.parse(INPUT);
        LOG.info("Loading nodes used in the retained ways.");
        NodeLoader nl = new NodeLoader(osm, wl.nodesInWays);
        nl.parse(INPUT);
        LOG.info("Loading relations (which ones?)");

        /* Find OSM intersection nodes. */
        LOG.info("Finding intersections.");
        // MapDB TreeSets are much faster than MapDB HashSets
        //Set<Long> referenced = db.getTreeSet("nodes_referenced");
        //Set<Long> intersections = db.getTreeSet("nodes_intersections");
        // NodeTrackers are much faster than MapDB TreeSets
        NodeTracker referenced = new NodeTracker();
        NodeTracker intersections = new NodeTracker();
        for (long wid : osm.ways.keySet()) {
            Way way = osm.ways.get(wid);
            for (long nid : way.nodes) {
                if (referenced.contains(nid)) {
                    intersections.add(nid);
                } else {
                    referenced.add(nid);
                }
            }
        }
        LOG.info("Done finding intersections.");
        LOG.info("Making edges from Ways.");
        /* Make edges from ways */
        List<Edge> edges = Lists.newArrayList();
        for (Entry<Long, Way> e : osm.ways.entrySet()) {
            Way way = e.getValue();
            Edge edge = new Edge();
            edge.way = e.getKey();
            edge.from = way.nodes[0];
            for (int n = 1; n < way.nodes.length; n++) {
                long node = way.nodes[n];
                if (n == (way.nodes.length - 1)) {
                    edge.to = node;
                    edges.add(edge);
                } else if (intersections.contains(node)) {
                    edge.to = node;
                    edges.add(edge);
                    edge = new Edge();
                    edge.way = e.getKey();
                    edge.from = node;
                }
            }
        }
        LOG.info("Done making {} edges from {} ways.", edges.size(), osm.ways.size());
        PrintStream ps;
        try {
            ps = new PrintStream(new FileOutputStream("/home/abyrd/edges.wkt"));
            for (Edge edge : edges) {
                Node fromNode = osm.nodes.get(edge.from);
                Node toNode = osm.nodes.get(edge.to);
                ps.printf("LINESTRING(%f %f,%f %f))\n", fromNode.lon, fromNode.lat, toNode.lon, toNode.lat);
            }   
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        db.close();
    }

    /** As a test, find a certain tag on ways. */
    public static void findTracks(OSM osm) {
        for (Entry<Long, Way> e : osm.ways.entrySet()) {
            if ("track".equals(e.getValue().getTag("highway"))) {
                LOG.info("{} is a track.", e.getKey());
            }
        }
    }    

}
