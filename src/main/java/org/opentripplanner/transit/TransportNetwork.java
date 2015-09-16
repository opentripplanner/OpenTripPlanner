package org.opentripplanner.transit;

import com.conveyal.osmlib.OSM;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.streets.LinkedPointSet;
import org.opentripplanner.streets.StreetLayer;
import org.opentripplanner.streets.StreetRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This is a completely new replacement for Graph, Router etc.
 * It uses a lot less object pointers and can be built, read, and written orders of magnitude faster.
 * @author abyrd
 */
public class TransportNetwork implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetwork.class);

    public StreetLayer streetLayer;

    public TransitLayer transitLayer;

    public void write (OutputStream stream) throws IOException {
        LOG.info("Writing transport network...");
        FSTObjectOutput out = new FSTObjectOutput(stream);
        out.writeObject(this, TransportNetwork.class );
        out.close();
        LOG.info("Done writing.");
    }

    public static TransportNetwork read (InputStream stream) throws Exception {
        LOG.info("Reading transport network...");
        FSTObjectInput in = new FSTObjectInput(stream);
        TransportNetwork result = (TransportNetwork) in.readObject(TransportNetwork.class);
        in.close();
        result.streetLayer.buildEdgeLists();
        result.streetLayer.indexStreets();
        result.transitLayer.rebuildTransientIndexes();
        result.transitLayer.buildStopTree();
        LOG.info("Done reading.");
        return result;
    }

    public static void main (String[] args) {
        // Round-trip serialize the transit layer and test its speed after deserialization.
        // TransportNetwork transportNetwork = TransportNetwork.fromFiles(args[0], args[1]);
        TransportNetwork transportNetwork = TransportNetwork.fromDirectory(new File("."));

        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("network.dat"));
            transportNetwork.write(outputStream);
            outputStream.close();
            // Be careful to release the original reference to be sure to have heap space
            transportNetwork = null;
            InputStream inputStream = new BufferedInputStream(new FileInputStream("network.dat"));
            transportNetwork = TransportNetwork.read(inputStream);
            inputStream.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        transportNetwork.testRouting();
        // transportNetwork.streetLayer.testRouting(false, transitLayer);
        // transportNetwork.streetLayer.testRouting(true, transitLayer);
    }

    /**
     * OSM PBF files are fragments of a single global database with a single namespace. Therefore it is valid to load
     * more than one PBF file into a single OSM storage object. However they might be from different points in time,
     * so it may be cleaner to just map one PBF file to one OSM object.
     *
     * On the other hand, GTFS feeds each have their own namespace. Each GTFS object is for one specific feed, and this
     * distinction should be maintained for various reasons.
     */
    public static TransportNetwork fromFiles (String osmSourceFile, String gtfsSourceFile) {

        // Load OSM data into MapDB
        OSM osm = new OSM(null);
        osm.intersectionDetection = true;
        osm.readFromFile(osmSourceFile);

        // Make street layer from OSM data in MapDB
        StreetLayer streetLayer = new StreetLayer();
        streetLayer.loadFromOsm(osm);
        osm.close();

        // Load transit data TODO remove need to supply street layer at this stage
        TransitLayer transitLayer = TransitLayer.fromGtfs(gtfsSourceFile);

        // The street index is needed for associating transit stops with the street network.
        streetLayer.indexStreets();
        streetLayer.associateStops(transitLayer, 500);
        // Edge lists must be built after all inter-layer linking has occurred.
        streetLayer.buildEdgeLists();
        transitLayer.rebuildTransientIndexes();
        transitLayer.buildStopTree();

        // Create transfers
        new TransferFinder(transitLayer, streetLayer, 1000).findTransfers();

        // Create and serialize a transport network
        TransportNetwork transportNetwork = new TransportNetwork();
        transportNetwork.streetLayer = streetLayer;
        transportNetwork.transitLayer = transitLayer;

        return transportNetwork;
    }

    public static TransportNetwork fromDirectory (File directory) {
        File osmFile = null;
        File gtfsFile = null;
        for (File file : directory.listFiles()) {
            switch (InputFileType.forFile(file)) {
                case GTFS:
                    LOG.info("Found GTFS file {}", file);
                    if (gtfsFile == null) {
                        gtfsFile = file;
                    } else {
                        LOG.warn("Can only load one GTFS file at a time.");
                    }
                    break;
                case OSM:
                    LOG.info("Found OSM file {}", file);
                    if (osmFile == null) {
                        osmFile = file;
                    } else {
                        LOG.warn("Can only load one OSM file at a time.");
                    }
                    break;
                case DEM:
                    LOG.warn("DEM file '{}' not yet supported.", file);
                    break;
                case OTHER:
                    LOG.warn("Skipping non-input file '{}'", file);
            }
        }
        return fromFiles(osmFile.getAbsolutePath(), gtfsFile.getAbsolutePath());
    }

    /**
     * Represents the different types of files that might be present in a router / graph build directory.
     * We want to detect even those that are not graph builder inputs so we can effectively warn when unrecognized file
     * types are present. This helps point out when config files have been misnamed.
     */
    private static enum InputFileType {
        GTFS, OSM, DEM, CONFIG, OUTPUT, OTHER;
        public static InputFileType forFile(File file) {
            String name = file.getName();
            if (name.endsWith(".zip")) {
                try {
                    ZipFile zip = new ZipFile(file);
                    ZipEntry stopTimesEntry = zip.getEntry("stop_times.txt");
                    zip.close();
                    if (stopTimesEntry != null) return GTFS;
                } catch (Exception e) { /* fall through */ }
            }
            if (name.endsWith(".pbf")) return OSM;
            if (name.endsWith(".tif") || name.endsWith(".tiff")) return DEM; // Digital elevation model (elevation raster)
            if (name.endsWith("network.dat")) return OUTPUT;
            return OTHER;
        }
    }

    /**
     * Test combined street and transit routing.
     */
    public void testRouting () {
        LOG.info("Street and transit routing from random street corners...");
        StreetRouter streetRouter = new StreetRouter(streetLayer);
        streetRouter.distanceLimitMeters = 1500;
        TransitRouter transitRouter = new TransitRouter(transitLayer);
        long startTime = System.currentTimeMillis();
        final int N = 1_000;
        final int nStreetIntersections = streetLayer.getVertexCount();
        Random random = new Random();
        for (int n = 0; n < N; n++) {
            // Do one street search around a random origin and destination, initializing the transit router
            // with the stops that were reached.
            int from = random.nextInt(nStreetIntersections);
            int to = random.nextInt(nStreetIntersections);
            streetRouter.setOrigin(from);
            streetRouter.route();
            streetRouter.setOrigin(to);
            streetRouter.route();
            transitRouter.reset();
            transitRouter.setOrigins(streetRouter.getReachedStops(), 8 * 60 * 60);
            transitRouter.route();
            if (n != 0 && n % 100 == 0) {
                LOG.info("    {}/{} searches", n, N);
            }
        }
        double eTime = System.currentTimeMillis() - startTime;
        LOG.info("average response time {} msec", eTime / N);
    }

    /**
     * TODO cache this grid.
     * @return an efficient implicit grid PointSet for this TransportNetwork.
     */
    public PointSet getGridPointSet() {
        LOG.error("Grid pointset not implemeted yet.");
        return null; // new WebMercatorGridPointSet(this);
    }

    /**
     * TODO cache this grid.
     * @return an efficient implicit grid PointSet for this TransportNetwork, pre-linked to the street layer.
     */
    public LinkedPointSet getLinkedGridPointSet() {
        return getGridPointSet().link(streetLayer);
    }

}
