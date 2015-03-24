package org.opentripplanner.osm;

import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import com.vividsolutions.jts.geom.Envelope;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun.Tuple3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.NavigableSet;
import java.util.zip.GZIPInputStream;

/**
 * OTP representation of a subset of OpenStreetMap. One or more PBF files can be loaded into this
 * object, which serves as a simplistic database for fetching and iterating over OSM elements.
 */
public class OSM {

    private static final Logger LOG = LoggerFactory.getLogger(OSM.class);

    public Map<Long, Node> nodes;
    public Map<Long, Way> ways;
    public Map<Long, Relation> relations;
    public NavigableSet<Tuple3<Integer, Integer, Long>> index; // (x_tile, y_tile, wayId)

    /** The MapDB backing this OSM, if any. */
    DB db = null; // db.close(); ?

    // Using DB TreeMaps is observed not to be slower than memory.
    // HashMaps are both bigger and slower.
    // It lets you run in 400MB instead of a few GB.

    /** If diskPath is null, OSM will be loaded into memory. */
    public OSM (String diskPath) {
        DBMaker dbMaker;
        if (diskPath == null) {
            LOG.info("OSM will be stored in a temporary file.");
            dbMaker = DBMaker.newTempFileDB();
        } else {
            if (diskPath.equals("__MEMORY__")) {
                LOG.info("OSM will be stored in memory.");
                dbMaker = DBMaker.newMemoryDB();
            } else {
                LOG.info("OSM will be stored in file {}.", diskPath);
                dbMaker = DBMaker.newFileDB(new File(diskPath));
            }
        }
        db = dbMaker
            .transactionDisable()
            .asyncWriteEnable()
            .compressionEnable()
            .make();
        nodes = db.getTreeMap("nodes");
        ways = db.getTreeMap("ways");
        relations = db.getTreeMap("relations");
        index = db.getTreeSet("spatial_index");
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

    /** Decode OSM gzipped text format produced by Vanilla Extract. */
    public void loadFromVexStream (InputStream vexStream) throws IOException {
        InputStream unzippedStream = new GZIPInputStream(vexStream);
        Reader decoded = new InputStreamReader(unzippedStream, Charsets.UTF_8); // UTF8 ENCODING is important
        BufferedReader bufferedReader = new BufferedReader(decoded);
        ArrayList<Long> nodesInWay = new ArrayList<>();
        Way way = null;
        Relation relation = null;
        long currentWayId = -1;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            String[] fields = line.trim().split("[ \t]");
            String etype = fields[0];
            long id = Long.parseLong(fields[1]);
            // LOG.info("{} {}", etype, id);
            if (etype.startsWith("W")) {
                if (way != null) {
                    way.nodes = Longs.toArray(nodesInWay);
                    ways.put(currentWayId, way);
                }
                way = new Way();
                nodesInWay.clear();
                currentWayId = id;
                if (fields.length > 2) {
                    way.tags = fields[2];
                }
            } else if ("N".equals(fields[0])) {
                double lat = Double.parseDouble(fields[2]);
                double lon = Double.parseDouble(fields[3]);
                Node node = new Node(lat, lon);
                if (fields.length > 4) {
                    node.tags = fields[4];
                }
                nodes.put(id, node);
                nodesInWay.add(id);
            } else {
                LOG.error("Unrecognized entity type {}", fields[0]);
            }
        }
        bufferedReader.close();
    }
}
