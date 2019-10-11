package org.opentripplanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.netex.loader.NetexBundle;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.GraphBuilderParameters;
import org.opentripplanner.standalone.config.OTPConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

public class ConstantsForTests {

    public static final String CALTRAIN_GTFS = "src/test/resources/caltrain_gtfs.zip";

    private static final String PORTLAND_GTFS = "src/test/resources/google_transit.zip";

    public static final String KCM_GTFS = "src/test/resources/kcm_gtfs.zip";
    
    public static final String FAKE_GTFS = "src/test/resources/testagency.zip";

    public static final String FARE_COMPONENT_GTFS = "src/test/resources/farecomponent_gtfs.zip";

    private static final String NETEX_DIR = "src/test/resources/netex";

    private static final String NETEX_FILENAME = "netex_minimal.zip";


    private static ConstantsForTests instance = null;

    private Graph portlandGraph = null;

    private Graph minNetexGraph = null;

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

    public Graph getMinimalNetexGraph() {
        if (minNetexGraph == null) {
            setupMinNetex();
        }
        return minNetexGraph;
    }

    public static NetexBundle createMinimalNetexBundle() {
        return NetexConfig.netexBundleForTest(
                createNetexBuilderParameters(),
                new File(ConstantsForTests.NETEX_DIR, ConstantsForTests.NETEX_FILENAME)
        );
    }

    private void setupPortland() {
        try {
            portlandGraph = new Graph();
            portlandContext = contextBuilder(ConstantsForTests.PORTLAND_GTFS)
                    .withGraphBuilderAnnotationsAndDeduplicator(portlandGraph)
                    .build();
            PatternHopFactory factory = new PatternHopFactory(portlandContext);
            factory.run(portlandGraph);
            // TODO: eliminate GTFSContext
            // this is now making a duplicate calendarservicedata but it's oh so practical
            portlandGraph.putService(
                    CalendarServiceData.class,
                    portlandContext.getCalendarServiceData()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private void setupMinNetex() {
        try {
            GraphBuilderParameters builderParameters = createNetexBuilderParameters();

            minNetexGraph = new Graph();

            NetexModule module = NetexConfig.netexModule(builderParameters, Collections.singletonList(
                    new File(ConstantsForTests.NETEX_DIR, ConstantsForTests.NETEX_FILENAME)
            ));

            module.buildGraph(minNetexGraph, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
//        StreetLinkerModule ttsnm = new StreetLinkerModule();
//        ttsnm.buildGraph(minNetexGraph, new HashMap<Class<?>, Object>());
    }

    public static Graph buildGraph(String path) {
        Graph graph = new Graph();
        GtfsContext context;
        try {
            context = contextBuilder(path).build();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        PatternHopFactory factory = new PatternHopFactory(context);
        factory.run(graph);
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );
        return graph;
    }


    private static GraphBuilderParameters createNetexBuilderParameters() {
        JsonNode buildConfig = new OTPConfiguration(null)
                .getGraphConfig(new File(ConstantsForTests.NETEX_DIR))
                .builderConfig();

        return new GraphBuilderParameters(buildConfig);
    }
}
