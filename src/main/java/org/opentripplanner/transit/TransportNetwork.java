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

        // Load OSM data
        String osmSourceFile = args[0];
        OSM osm = new OSM(null);
        osm.intersectionDetection = true;
        osm.readFromFile(osmSourceFile);
        StreetLayer streetLayer = new StreetLayer();
        streetLayer.loadFromOsm(osm);
        osm.close();
        streetLayer.indexStreets(); // needed for linking transit stops

        // Load transit data
        String gtfsSourceFile = args[1];
        TransitLayer transitLayer = TransitLayer.fromGtfs(gtfsSourceFile, streetLayer);

        // Must build edge lists after all inter-layer linking has occurred.
        streetLayer.buildEdgeLists();
        transitLayer.rebuildTransientIndexes();

        // Create transfers
        new TransferFinder(transitLayer, streetLayer, 1500).findTransfers();

        // Create and serialize a transport network
        TransportNetwork transportNetwork = new TransportNetwork();
        transportNetwork.streetLayer = streetLayer;
        transportNetwork.transitLayer = transitLayer;
        transportNetwork = roundTripTest(transportNetwork);


        // Do some routing.
        transportNetwork.testRouting();
        // transportNetwork.streetLayer.testRouting(false, transitLayer);
        // transportNetwork.streetLayer.testRouting(true, transitLayer);

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
