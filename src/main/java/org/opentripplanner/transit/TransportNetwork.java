package org.opentripplanner.transit;

import com.conveyal.osmlib.OSM;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.opentripplanner.streets.StreetLayer;
import org.opentripplanner.streets.StreetRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Random;

/**
 *
 */
public class TransportNetwork implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TransportNetwork.class);

    StreetLayer streetLayer;
    TransitLayer transitLayer;

    /**
     * Serialize the transport network to disk and re-load it to make sure serialiation works right.
     */
    public static TransportNetwork roundTripTest (TransportNetwork original) {
        // Round-trip serialize the transit layer and test its speed
        try {
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream("network.dat"));
            original.write(outputStream);
            outputStream.close();
            InputStream inputStream = new BufferedInputStream(new FileInputStream("network.dat"));
            TransportNetwork copy = TransportNetwork.read(inputStream);
            inputStream.close();
            return copy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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
        LOG.info("Done reading.");
        return result;
    }

    public static void main (String[] args) {
        TransportNetwork transportNetwork = TransportNetwork.fromFiles(args[0], args[1]);
        transportNetwork = roundTripTest(transportNetwork);
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

        // The street index is needed for linking transit stops to the street network.
        streetLayer.indexStreets();
        streetLayer.linkStops(transitLayer);
        // Edge lists must be built after all inter-layer linking has occurred.
        streetLayer.buildEdgeLists();
        transitLayer.rebuildTransientIndexes();

        // Create transfers
        new TransferFinder(transitLayer, streetLayer, 1500).findTransfers();

        // Create and serialize a transport network
        TransportNetwork transportNetwork = new TransportNetwork();
        transportNetwork.streetLayer = streetLayer;
        transportNetwork.transitLayer = transitLayer;

        return transportNetwork;
    }

    /**
     * Test combined street and transit routing.
     */
    public void testRouting () {
        LOG.info("Street and transit routing from random street corners...");
        StreetRouter streetRouter = new StreetRouter(streetLayer, transitLayer);
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
            transitRouter.reset();
            streetRouter.route(from, StreetRouter.ALL_VERTICES);
            transitRouter.setOrigins(streetRouter.timesToReachedStops());
            streetRouter.route(to, StreetRouter.ALL_VERTICES);
            transitRouter.setTargets(streetRouter.timesToReachedStops().keySet());
            transitRouter.route();
            if (n != 0 && n % 100 == 0) {
                LOG.info("    {}/{} searches", n, N);
            }
        }
        double eTime = System.currentTimeMillis() - startTime;
        LOG.info("average response time {} msec", eTime / N);
    }


}
