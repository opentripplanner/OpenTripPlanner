package org.opentripplanner.osm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * A proof of concept for loading OSM data from PBF into MapDB, then finding intersections and generating edges from
 * the disk-backed data. The intent is to allow working with very large OSM files without consuming too much memory
 * during the graph build process -- it is part of the same set of experiments that loads GTFS into MapDB.
 *
 * Loading the ways into a MapDB may not even be necessary. It should be possible to generate the edges on the fly, one
 * way at a time. The same is true of the nodes: we could just scan through them and only track which are within the
 * bounding geometry, then which are intersections, then immediately create vertices rather than ever saving the node
 * objects (as long as these intersection vertices are indexed by OSM ID).
 *
 * Some observations:
 * The Netherlands PBF is 900MB, and a MapDB using Treemaps is 2.1GB.
 * Loading the PBF into MapDB takes 6 minutes for nodes, 1 minute for ways, and 35 seconds to find intersections.
 * However, this is loading
 * During tests, the JVM ran out of memory when making edges because currently we keeps all edges in memory.
 * Note that these maps contain only routable ways, not all ways (see org.opentripplanner.osm.Parser#retainKeys).
 *
 * A "compressed" DB is about 1/3 smaller than an uncompressed one.
 * The "compact" operation only reduces an uncompressed DB's size by about 1/6.
 * The "compact" operation does not reduce the size of the compressed DB at all.
 *
 * @author abyrd
 */
public class OSMMain {

    private static final Logger LOG = LoggerFactory.getLogger(OSMMain.class);
    static final Envelope ENV = new Envelope(4.4, 5.5, 52.2, 53.3);

    public static void main(String[] args) {
        /** This main method will convert a PBF file to VEX using an intermediate MapDB datastore. */
        OSM osm = OSM.fromPBF(args[0]);//, ENV);
        try (OutputStream fout = new FileOutputStream("test.vex")) {
            LOG.info("begin writing vex");
            new VexFormatCodec().writeVex(osm, fout);
            LOG.info("end writing vex");
        } catch (FileNotFoundException ex) {
            LOG.error("FNFEX");
        } catch (IOException ex) {
            LOG.error("IOEX");
        }
        System.exit(0);

        List<Edge> edges = makeEdges(osm);
        PrintStream ps;
        try {
            ps = new PrintStream(new FileOutputStream("/home/abyrd/edges.wkt"));
            for (Edge edge : edges) {
                Node fromNode = osm.nodes.get(edge.from);
                Node toNode = osm.nodes.get(edge.to);
                ps.printf("LINESTRING(%f %f,%f %f))\n", fromNode.getLon(), fromNode.getLat(), toNode.getLon(), toNode.getLat());
            }   
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    public static void analyzeTags(OSM osm) {
        Multimap<String, String> kv = HashMultimap.create();
        for (Way way : osm.ways.values()) {
            for (Tagged.Tag tag : way.getTags()) {
                kv.put(tag.key, tag.value);
            }
        }
        for (String k : kv.keySet()) {
            LOG.info("{} = {}", k, kv.get(k));
        }
    }

    public static List<Edge> makeEdges(OSM osm) {
//        osm.findIntersections();
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
//                } else if (osm.intersections.contains(node)) {
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
