package org.opentripplanner;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import com.csvreader.CsvReader;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.AddTransitModelEntitiesToGraph;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.openstreetmap.BinaryOpenStreetMapProvider;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetVehicleRentalLink;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.util.NonLocalizedString;

public class ConstantsForTests {

    public static final String CALTRAIN_GTFS = "src/test/resources/caltrain_gtfs.zip";

    public static final String NETEX_MINIMAL = "src/test/resources/netex/netex_minimal.zip";

    private static final String PORTLAND_GTFS = "src/test/resources/google_transit.zip";

    private static final String PORTLAND_CENTRAL_OSM = "src/test/resources/portland-central-filtered.osm.pbf";

    private static final String PORTLAND_BIKE_SHARE_CSV = "src/test/resources/portland-vehicle-rental.csv";

    private static final String OSLO_EAST_OSM = "src/test/resources/oslo-east-filtered.osm.pbf";

    public static final String KCM_GTFS = "src/test/resources/kcm_gtfs.zip";
    
    public static final String FAKE_GTFS = "src/test/resources/testagency";

    public static final String FARE_COMPONENT_GTFS = "src/test/resources/farecomponent_gtfs.zip";

    private static final String NETEX_DIR = "src/test/resources/netex";

    private static final String NETEX_FILENAME = "netex_minimal.zip";

    /* Stuttgart area, Germany */
    public static final String DEUFRINGEN_OSM = "src/test/resources/germany/deufringen-minimal.osm.pbf";
    public static final String BOEBLINGEN_OSM = "src/test/resources/germany/boeblingen-minimal.osm.pbf";
    public static final String VVS_BUS_764_ONLY = "src/test/resources/germany/vvs-bus-764-only.gtfs.zip";
    public static final String VVS_BUS_751_ONLY = "src/test/resources/germany/vvs-bus-751-only.gtfs.zip";
    public static final String HERRENBERG_HINDENBURG_STR_UNDER_CONSTRUCTION_OSM = "src/test/resources/germany/herrenberg-hindenburgstr-under-construction.osm.pbf";
    public static final String HERRENBERG_BARRIER_GATES_OSM = "src/test/resources/germany/herrenberg-barrier-gates.osm.pbf";
    public static final String HERRENBERG_OSM = "src/test/resources/germany/herrenberg-minimal.osm.pbf";

    private static final CompositeDataSource NETEX_MINIMAL_DATA_SOURCE = new ZipFileDataSource(
            new File(NETEX_DIR, NETEX_FILENAME),
            FileType.NETEX
    );

    private static ConstantsForTests instance = null;

    private Graph portlandGraph = null;

    private Graph minNetexGraph = null;

    private ConstantsForTests() {

    }

    public static ConstantsForTests getInstance() {
        if (instance == null) {
            instance = new ConstantsForTests();
        }
        return instance;
    }

    public static NetexBundle createMinimalNetexBundle() {
        return NetexConfig.netexBundleForTest(
                createNetexBuilderParameters(),
                new File(ConstantsForTests.NETEX_DIR, ConstantsForTests.NETEX_FILENAME)
        );
    }

    /**
     * Returns a cached copy of the Minimal NeTEx graph, which may have been initialized.
     */
    public synchronized Graph getCachedMinimalNetexGraph() {
        if (minNetexGraph == null) {
            minNetexGraph = buildNewMinimalNetexGraph();
        }
        return minNetexGraph;
    }

    /**
     * Returns a cached copy of the Portland graph, which may have been initialized.
     */
    public synchronized Graph getCachedPortlandGraph() {
        if (portlandGraph == null) {
            portlandGraph = buildNewPortlandGraph();
        }
        return portlandGraph;
    }

    /**
     * Builds a new graph using the Portland test data.
     */
    public static Graph buildNewPortlandGraph() {
        try {
            Graph graph = new Graph();
            // Add street data from OSM
            {
                File osmFile = new File(PORTLAND_CENTRAL_OSM);
                BinaryOpenStreetMapProvider osmProvider = new BinaryOpenStreetMapProvider(osmFile, false);
                OpenStreetMapModule osmModule = new OpenStreetMapModule(List.of(osmProvider));
                osmModule.staticBikeParkAndRide = true;
                osmModule.staticParkAndRide = true;
                osmModule.skipVisibility = true;
                osmModule.buildGraph(graph, new HashMap<>());
            }
            // Add transit data from GTFS
            {
                GtfsBundle gtfsBundle = new GtfsBundle(new File(PORTLAND_GTFS));
                gtfsBundle.setFeedId(new GtfsFeedId.Builder().id("prt").build());
                GtfsModule module = new GtfsModule(
                        List.of(gtfsBundle),
                        new ServiceDateInterval(new ServiceDate(2009, 9, 1), new ServiceDate(2010, 3, 1))
                );
                module.buildGraph(graph, new HashMap<>());
            }
            // Link transit stops to streets
            {
                GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
                streetTransitLinker.buildGraph(graph, new HashMap<>());
            }

            graph.hasStreets = true;
            graph.hasTransit = true;

            addPortlandVehicleRentals(graph);

            graph.index();

            return graph;
        } catch (Exception e) {
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

    public static Graph buildNewMinimalNetexGraph() {
        try {
            Graph graph = new Graph();
            // Add street data from OSM
            {
                File osmFile = new File(OSLO_EAST_OSM);

                BinaryOpenStreetMapProvider osmProvider = new BinaryOpenStreetMapProvider(osmFile, false);
                OpenStreetMapModule osmModule = new OpenStreetMapModule(Lists.newArrayList(osmProvider));
                osmModule.skipVisibility = true;
                osmModule.buildGraph(graph, new HashMap<>());
            }
            // Add transit data from Netex
            {
                BuildConfig buildParameters = createNetexBuilderParameters();
                List<DataSource> dataSources = Collections.singletonList(NETEX_MINIMAL_DATA_SOURCE);
                NetexModule module = NetexConfig.netexModule(buildParameters, dataSources);
                module.buildGraph(graph, null);
            }
            // Link transit stops to streets
            {
                GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
                streetTransitLinker.buildGraph(graph, new HashMap<>());
            }
            return graph;
        } catch (Exception e) {
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


    private static void addPortlandVehicleRentals(Graph graph) {
        try {
            VertexLinker linker = graph.getLinker();
            CsvReader reader = new CsvReader(PORTLAND_BIKE_SHARE_CSV, ',', StandardCharsets.UTF_8);
            reader.readHeaders();
            while (reader.readRecord()) {
                VehicleRentalStation station = new VehicleRentalStation();
                station.id = new FeedScopedId(reader.get("network"), reader.get("osm_id"));
                station.latitude = Double.parseDouble(reader.get("lat"));
                station.longitude = Double.parseDouble(reader.get("lon"));
                station.name = new NonLocalizedString(reader.get("osm_id"));
                station.realTimeData = false;
                station.isKeepingVehicleRentalAtDestinationAllowed = true;

                VehicleRentalStationVertex stationVertex = new VehicleRentalStationVertex(graph, station);
                new VehicleRentalEdge(stationVertex);

                linker.linkVertexPermanently(
                        stationVertex,
                        new TraverseModeSet(TraverseMode.WALK),
                        LinkingDirection.BOTH_WAYS,
                        (vertex, streetVertex) -> List.of(
                                new StreetVehicleRentalLink((VehicleRentalStationVertex) vertex, streetVertex),
                                new StreetVehicleRentalLink(streetVertex, (VehicleRentalStationVertex) vertex)
                        )
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BuildConfig createNetexBuilderParameters() {
        return new ConfigLoader(new File(ConstantsForTests.NETEX_DIR)).loadBuildConfig();
    }
}
