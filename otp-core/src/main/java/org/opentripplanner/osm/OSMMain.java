package org.opentripplanner.osm;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;

import crosby.binary.file.BlockInputStream;

// Compressed DB is about 1/3 smaller than uncompressed.
// Compact operation only reduces uncompressed DB size by about 1/6.
// Compact operation does not reduce compressed DB size at all.

public class OSMMain {

    private static final Logger LOG = LoggerFactory.getLogger(OSMMain.class);
    static final String INPUT = "/var/otp/graphs/trimet/portland.osm.pbf";
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
        osm.nodes = db.getTreeMap("nodes");
        osm.ways = db.getTreeMap("ways");
        osm.relations = db.getTreeMap("relations");
        try {
            Parser parser = new Parser(osm);
            FileInputStream input = new FileInputStream(INPUT);
            BlockInputStream bis = new BlockInputStream(input, parser);
            bis.process();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /* Find OSM intersection nodes. */
        LOG.info("Finding intersections.");
        // MapDB TreeSets are much faster than MapDB HashSets
        Set<Long> referenced = db.getTreeSet("nodes_referenced");
        Set<Long> intersections = db.getTreeSet("nodes_intersections");
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
//        for (Edge edge : edges) {
//            LOG.info("{}", edge);
//        }
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
