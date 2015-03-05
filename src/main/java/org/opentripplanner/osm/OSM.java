package org.opentripplanner.osm;

import com.google.common.base.Charsets;
import com.google.common.primitives.Longs;
import com.vividsolutions.jts.geom.Envelope;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun.Tuple3;
import org.opentripplanner.osm.serializer.NodeSerializer;
import org.opentripplanner.osm.serializer.WaySerializer;
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
 * Using DB TreeMaps is often not any slower than memory. HashMaps are both bigger and slower.
 * This is probably because our keys are so small. A hashmap needs to store both the long key and its hash.
 */
public class OSM {

    private static final Logger LOG = LoggerFactory.getLogger(OSM.class);

    public Map<Long, Node> nodes;
    public Map<Long, Way> ways;
    public Map<Long, Relation> relations;
    public NavigableSet<Tuple3<Integer, Integer, Long>> index; // (x_tile, y_tile, wayId)

    /** The MapDB backing this OSM, if any. */
    DB db = null;

    /** 
     * Construct a new MapDB-based random-access OSM data store.
     * If diskPath is null, OSM will be loaded into a temporary file and deleted on shutdown.
     * If diskPath is the string "__MEMORY__" the OSM will be stored entirely in memory. 
     * 
     * @param diskPath - the file in which to save the data, null for a temp file, or "__MEMORY__" for in-memory.
     */
    public OSM (String diskPath) {
        DBMaker dbMaker;
        if (diskPath == null) {
            LOG.info("OSM will be stored in a temporary file.");
            dbMaker = DBMaker.newTempFileDB().deleteFilesAfterClose();
        } else {
            if (diskPath.equals("__MEMORY__")) {
                LOG.info("OSM will be stored in memory.");
                // 'direct' means off-heap memory, no garbage collection overhead
                dbMaker = DBMaker.newMemoryDirectDB(); 
            } else {
                LOG.info("OSM will be stored in file {}.", diskPath);
                dbMaker = DBMaker.newFileDB(new File(diskPath));
            }
        }
        
        // Compression has no appreciable effect on speed but reduces file size by about 16 percent.
        
        db = dbMaker.asyncWriteEnable()
                .transactionDisable()
                .compressionEnable()
                .cacheSize(50 * 1024 * 1024)
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .make();

        nodes = db.createTreeMap("nodes")
                .valueSerializer(new NodeSerializer())
                .makeLongMap();
        
        ways =  db.createTreeMap("ways")
                .valueSerializer(new WaySerializer())
                .makeLongMap();
        
        relations = db.createTreeMap("relations")
                .makeLongMap();

        // Serializer delta-compresses the tuple as a whole and variable-width packs ints,
        // but does not recursively delta-code its elements.
        index = db.createTreeSet("spatial_index")
                .serializer(BTreeKeySerializer.TUPLE3) 
                .make();
    }
    
    // boolean to filter entities on tags, or list of tag keys to retain?
    public void loadFromPBFStream (InputStream in) {
        LOG.info("Reading PBF stream.");
        Parser parser = new Parser(this);
        parser.parse(in);
    }

    public void loadFromPBFFile (String filePath) {
        try {
            LOG.info("Reading PBF from file '{}'.", filePath);
            Parser parser = new Parser(this);
            parser.parse(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            LOG.error("Error occurred while parsing PBF file '{}'", filePath);
            e.printStackTrace();
        }
    }

    // TODO we know that the ordering of entity types within a PBF file is almost always nodes, then ways,
    // then relations. Here we are doing three passes over the whole file, but we could get away with one
    // and a half by combining the NodeGeomFilter and the WayLoader, then bailing out of the NodeLoader
    // as soon as it sees a Way.
    // In any case we can't spatially filter PBF data coming from a stream because we'd need to backtrack.
    public void loadFromPBF (String pbfFile, Envelope env) {
        LOG.info("Reading PBF file '{}' filtering with envelope {}", pbfFile, env);
        LOG.info("Finding nodes within the bounding geometry.");
        NodeGeomFilter ngf = new NodeGeomFilter(env);
        ngf.parse(pbfFile);
        LOG.info("LOAD RELATIONS HERE");
        LOG.info("Loading ways containing nodes found within the bounding geometry.");
        WayLoader wl = new WayLoader(this, ngf.nodesInGeom);
        wl.parse(pbfFile);
        LOG.info("Loading nodes used in the retained ways.");
        NodeLoader nl = new NodeLoader(this, wl.nodesInWays);
        nl.parse(pbfFile);
    }

    /** 
     * Decode OSM gzipped text format produced by Vanilla Extract.
     * It remains to be determined whether this VEX text format is better or worse than the slightly 
     * more complicated VEX binary format, but it's certainly simpler and cleaner than PBF.
     */
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
                    way.setTagsFromString(fields[2]);
                }
            } else if ("N".equals(fields[0])) {
                double lat = Double.parseDouble(fields[2]);
                double lon = Double.parseDouble(fields[3]);
                Node node = new Node(lat, lon);
                if (fields.length > 4) {
                    node.setTagsFromString(fields[4]);
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
