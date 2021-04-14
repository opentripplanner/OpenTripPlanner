package org.opentripplanner;

import com.google.common.collect.Lists;
import java.io.IOException;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.graph_builder.module.AddTransitModelEntitiesToGraph;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.BuildConfig;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

public class ConstantsForTests {

    public static final String CALTRAIN_GTFS = "src/test/resources/caltrain_gtfs.zip";

    public static final String NETEX_MINIMAL = "src/test/resources/netex/netex_minimal.zip";

    private static final String PORTLAND_GTFS = "src/test/resources/google_transit.zip";

    private static final String PORTLAND_CENTRAL_OSM = "src/test/resources/portland-central-filtered.osm.pbf";

    private static final String OSLO_EAST_OSM = "src/test/resources/oslo-east-filtered.osm.pbf";

    public static final String KCM_GTFS = "src/test/resources/kcm_gtfs.zip";
    
    public static final String FAKE_GTFS = "src/test/resources/testagency.zip";

    public static final String FARE_COMPONENT_GTFS = "src/test/resources/farecomponent_gtfs.zip";

    private static final String NETEX_DIR = "src/test/resources/netex";

    private static final String NETEX_FILENAME = "netex_minimal.zip";

    /* Stuttgart area, Germany */
    public static final String DEUFRINGEN_OSM = "src/test/resources/germany/deufringen-minimal.osm.pbf";
    public static final String BOEBLINGEN_OSM = "src/test/resources/germany/boeblingen-minimal.osm.pbf";
    public static final String VVS_BUS_764_ONLY = "src/test/resources/germany/vvs-bus-764-only.gtfs.zip";
    public static final String VVS_BUS_751_ONLY = "src/test/resources/germany/vvs-bus-751-only.gtfs.zip";
    public static final String HERRENBERG_HINDENBURG_STR_UNDER_CONSTRUCTION_OSM = "src/test/resources/germany/herrenberg-hindenburgstr-under-construction.osm.pbf";

    private static final CompositeDataSource NETEX_MINIMAL_DATA_SOURCE = new ZipFileDataSource(
            new File(NETEX_DIR, NETEX_FILENAME),
            FileType.NETEX
    );

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
            // Add street data from OSM
            {
                File osmFile = new File(PORTLAND_CENTRAL_OSM);
                BinaryOpenStreetMapProvider osmProvider = new BinaryOpenStreetMapProvider(osmFile, false);
                OpenStreetMapModule osmModule = new OpenStreetMapModule(Lists.newArrayList(osmProvider));
                osmModule.skipVisibility = true;
                osmModule.buildGraph(portlandGraph, new HashMap<>());
            }
            // Add transit data from GTFS
            {
                portlandContext = contextBuilder(ConstantsForTests.PORTLAND_GTFS)
                        .withIssueStoreAndDeduplicator(portlandGraph)
                        .build();
                AddTransitModelEntitiesToGraph.addToGraph(portlandContext, portlandGraph);
                GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(portlandContext);
                factory.run(portlandGraph);
            }
            // Link transit stops to streets
            {
                GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
                streetTransitLinker.buildGraph(portlandGraph, new HashMap<>());
            }
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

    public static Graph buildOsmGraph(String osmPath) {

        try {
            var graph = new Graph();
            // Add street data from OSM
            File osmFile = new File(osmPath);
            BinaryOpenStreetMapProvider osmProvider =
                    new BinaryOpenStreetMapProvider(osmFile, true);
            OpenStreetMapModule osmModule =
                    new OpenStreetMapModule(Lists.newArrayList(osmProvider));
            osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
            osmModule.skipVisibility = true;
            osmModule.buildGraph(graph, new HashMap<>());
            return graph;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Graph buildGtfsGraph(String osmPath, String gtfsPath) throws IOException {
        var graph = buildOsmGraph(osmPath);

        var context = contextBuilder(gtfsPath)
                .withIssueStoreAndDeduplicator(graph)
                .build();
        AddTransitModelEntitiesToGraph.addToGraph(context, graph);
        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);
        // Link transit stops to streets
        GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
        streetTransitLinker.buildGraph(graph, new HashMap<>());
        graph.putService(
                CalendarServiceData.class,
                context.getCalendarServiceData()
        );
        return graph;
    }

    private void setupMinNetex() {
        try {
            minNetexGraph = new Graph();
            // Add street data from OSM
            {
                File osmFile = new File(OSLO_EAST_OSM);

                BinaryOpenStreetMapProvider osmProvider = new BinaryOpenStreetMapProvider(osmFile, false);
                OpenStreetMapModule osmModule = new OpenStreetMapModule(Lists.newArrayList(osmProvider));
                osmModule.skipVisibility = true;
                osmModule.buildGraph(minNetexGraph, new HashMap<>());
            }
            // Add transit data from Netex
            {
                BuildConfig buildParameters = createNetexBuilderParameters();
                List<DataSource> dataSources = Collections.singletonList(NETEX_MINIMAL_DATA_SOURCE);
                NetexModule module = NetexConfig.netexModule(buildParameters, dataSources);
                module.buildGraph(minNetexGraph, null);
            }
            // Link transit stops to streets
            {
                GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
                streetTransitLinker.buildGraph(minNetexGraph, new HashMap<>());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
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
        AddTransitModelEntitiesToGraph.addToGraph(context, graph);

        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );
        return graph;
    }


    private static BuildConfig createNetexBuilderParameters() {
        return new ConfigLoader(new File(ConstantsForTests.NETEX_DIR)).loadBuildConfig();
    }
}
