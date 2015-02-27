package org.opentripplanner.osm;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Envelope;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.NavigableSet;

/**
 * OTP representation of a subset of OpenStreetMap. One or more PBF files can be loaded into this
 * object, which serves as a simplistic database for fetching and iterating over OSM elements.
 */
public class OSM {

    private static final Logger LOG = LoggerFactory.getLogger(OSM.class);

    public Map<Long, Node> nodes;
    public Map<Long, Way> ways;
    public Map<Long, Relation> relations;
    public NavigableSet<Tuple3<Integer, Integer, Long>> index; // (x, y, wayId)

    /** The MapDB backing this OSM, if any. */
    DB db = null; // db.close(); ?

    /** If diskPath is null, OSM will be loaded into memory. */
    public OSM(String diskPath) {
        // Using DB TreeMaps is observed not to be slower than memory.
        // HashMaps are both bigger and slower.
        // It lets you run in 400MB instead of a few GB.
        if (diskPath != null) {
            LOG.info("OSM backed by file.");
            DB db = DBMaker.newFileDB(new File(diskPath))
                    .transactionDisable()
                    .asyncWriteEnable()
                    .compressionEnable()
                    .make();
            nodes = db.getTreeMap("nodes");
            ways = db.getTreeMap("ways");
            relations = db.getTreeMap("relations");
            index = db.getTreeSet("spatial_index");
        } else {
            // In-memory version
            // use newMemoryDB for higher speed?
            nodes = Maps.newHashMap();
            ways = Maps.newHashMap();
            relations = Maps.newHashMap();
        }
    }
    
    // boolean filterTags
    public static OSM fromPBF(String pbfFile) {
        LOG.info("Reading entire PBF file '{}'", pbfFile);
        Parser parser = new Parser(null);
        parser.parse(pbfFile);
        return parser.osm;
    }

    public static OSM fromPBF(String pbfFile, Envelope env) {
        LOG.info("Reading PBF file '{}' filtering with envelope {}", pbfFile, env);
        OSM osm = new OSM("/var/vex/osm");
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



}
