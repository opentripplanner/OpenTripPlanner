package org.opentripplanner.jags.test;

import java.io.File;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.edgetype.loader.GTFSPatternHopLoader;
import org.opentripplanner.jags.edgetype.loader.NetworkLinker;
import org.opentripplanner.jags.gtfs.GtfsContext;
import org.opentripplanner.jags.gtfs.GtfsLibrary;

public class ConstantsForTests {

    public static final String CALTRAIN_GTFS = "src/test/resources/caltrain_gtfs.zip";

    public static final String PORTLAND_GTFS = "src/test/resources/google_transit.zip";

    public static final String FAKE_GTFS = "src/test/resources/testagency.zip";

    public static final double WALKING_SPEED = 1.33; // meters/sec
                                                     // (http://en.wikipedia.org/wiki/Walking),

    // roughly 3mph

    public static final String NY_GTFS = "src/test/resources/subway.zip";

    private static ConstantsForTests instance = null;

    private Graph portlandGraph = null;

    private GtfsContext portlandContext = null;

    private ConstantsForTests() {

    }

    public static ConstantsForTests getInstance() {
        if (instance == null) {
            instance = new ConstantsForTests();
        }
        return instance;
    }

    public GtfsContext getPortlandContext() {
        if (portlandGraph == null) {
            setupPortland();
        }
        return portlandContext;
    }

    public Graph getPortlandGraph() {
        if (portlandGraph == null) {
            setupPortland();
        }
        return portlandGraph;
    }

    private void setupPortland() {
        try {
            portlandContext = GtfsLibrary.readGtfs(new File(ConstantsForTests.PORTLAND_GTFS));
            portlandGraph = new Graph();
            GTFSPatternHopLoader hl = new GTFSPatternHopLoader(portlandGraph, portlandContext);
            hl.load();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        NetworkLinker nl = new NetworkLinker(portlandGraph);
        nl.createLinkage();
    }
}
