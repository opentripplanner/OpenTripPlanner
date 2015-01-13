package org.opentripplanner.osm;

import java.util.Map;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Envelope;

/**
 * OTP representation of a subset of OpenStreetMap. One or more PBF files can be loaded into this
 * object, which serves as a simplistic database for fetching and iterating over OSM elements.
 */
public class OSM {

    private static final Logger LOG = LoggerFactory.getLogger(OSM.class);

    public Map<Long, Node> nodes;
    public Map<Long, Way> ways;
    public Map<Long, Relation> relations;

    /** The nodes which are referenced more than once by ways in this OSM. */
    public NodeTracker intersections;
    
    /** The MapDB backing this OSM, if any. */
    DB db = null; // db.close(); ?
            
    public OSM(boolean diskBacked) {
        // Using DB TreeMaps is observed not to be slower than memory.
        // It lets you run in 400MB instead of a few GB.
        if (diskBacked) {
            LOG.info("OSM backed by temporary file.");
            DB db = DBMaker.newTempFileDB()
                    .transactionDisable()
                    .asyncWriteEnable()
                    .compressionEnable()
                    .make();
            nodes = db.getTreeMap("nodes");
            ways = db.getTreeMap("ways");
            relations = db.getTreeMap("relations");
        } else {
            // In-memory version
            nodes = Maps.newHashMap();
            ways = Maps.newHashMap();
            relations = Maps.newHashMap();            
        }
    }
    
    // boolean filterTags
    public static OSM fromPBF(String pbfFile) {
        LOG.info("Reading entire PBF file '{}'", pbfFile);
        FullParser fp = new FullParser();
        fp.parse(pbfFile);
        return fp.osm;
    }

    public static OSM fromPBF(String pbfFile, Envelope env) {
        LOG.info("Reading PBF file '{}' filtering with envelope {}", pbfFile, env);
        OSM osm = new OSM(true);
        LOG.info("Finding nodes within the bounding geometry.");
        NodeGeomFilter ngf = new NodeGeomFilter(env);
        ngf.parse(pbfFile);
        LOG.info("Loading ways containing nodes found within the bounding geometry.");
        WayLoader wl = new WayLoader(osm, ngf.nodesInGeom);
        wl.parse(pbfFile);
        LOG.info("Loading nodes used in the retained ways.");
        NodeLoader nl = new NodeLoader(osm, wl.nodesInWays);
        nl.parse(pbfFile);
        LOG.info("Loading relations (which ones?)");
        return osm;
    }
    
    /**
     * Find nodes referenced by more than one way. NodeTracker intersections will be null until this
     * is called. MapDB TreeSets are much faster than MapDB HashSets, but in-memory NodeTrackers are
     * much faster than MapDB TreeSets.
     */
    public void findIntersections() {
        LOG.info("Finding intersections.");
        intersections = new NodeTracker();
        NodeTracker referenced = new NodeTracker();
        for (Way way : ways.values()) {
            for (long nid : way.nodes) {
                if (referenced.contains(nid)) {
                    intersections.add(nid); // seen more than once
                } else {
                    referenced.add(nid); // seen for the first time
                }
            }
        }
        LOG.info("Done finding intersections.");        
    }

}
