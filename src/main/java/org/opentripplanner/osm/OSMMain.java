package org.opentripplanner.osm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Envelope;

// Compressed DB is about 1/3 smaller than uncompressed.
// Compact operation only reduces uncompressed DB size by about 1/6.
// Compact operation does not reduce compressed DB size at all.

public class OSMMain {

    private static final Logger LOG = LoggerFactory.getLogger(OSMMain.class);
    static final String INPUT = "/var/otp/graphs/nl2/netherlands-latest.osm.pbf";
    static final Envelope ENV = new Envelope(4.4, 5.5, 52.2, 53.3);

    public static void main(String[] args) {
        /* Load OSM PBF with spatial filtering. */
        OSM osm = OSM.fromPBF(INPUT, ENV);
        List<Edge> edges = makeEdges(osm);
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
    }

    public static List<Edge> makeEdges(OSM osm) {
        osm.findIntersections();
        LOG.info("Making edges from Ways.");
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
                } else if (osm.intersections.contains(node)) {
                    edge.to = node;
                    edges.add(edge);
                    edge = new Edge();
                    edge.way = e.getKey();
                    edge.from = node;
                }
            }
        }
        LOG.info("Done making {} edges from {} ways.", edges.size(), osm.ways.size());        
        return edges;
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
