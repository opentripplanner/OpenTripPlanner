package org.opentripplanner;

import com.csvreader.CsvReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.netex.NetexModule;
import org.opentripplanner.netex.configure.NetexConfig;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetVehicleRentalLink;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vertextype.VehicleRentalPlaceVertex;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.util.NonLocalizedString;

public class ConstantsForTests {

  public static final String CALTRAIN_GTFS = "src/test/resources/gtfs/caltrain_gtfs.zip";

  public static final String NETEX_MINIMAL = "src/test/resources/netex/netex_minimal.zip";

  private static final String PORTLAND_GTFS = "src/test/resources/portland/portland.gtfs.zip";

  private static final String PORTLAND_CENTRAL_OSM =
    "src/test/resources/portland/portland-central-filtered.osm.pbf";

  private static final String PORTLAND_BIKE_SHARE_CSV =
    "src/test/resources/portland/portland-vehicle-rental.csv";

  private static final String PORTLAND_NED = "src/test/resources/portland/portland-ned.tif";

  private static final String PORTLAND_NED_WITH_NODATA =
    "src/test/resources/portland/portland-ned-nodata.tif";

  private static final String OSLO_EAST_OSM = "src/test/resources/oslo-east-filtered.osm.pbf";

  public static final String KCM_GTFS = "src/test/resources/gtfs/kcm_gtfs.zip";

  public static final String FAKE_GTFS = "src/test/resources/testagency";

  public static final String FARE_COMPONENT_GTFS =
    "src/test/resources/gtfs/farecomponents.gtfs.zip";

  private static final String NETEX_DIR = "src/test/resources/netex";

  private static final String NETEX_FILENAME = "netex_minimal.zip";

  /* Stuttgart area, Germany */
  public static final String DEUFRINGEN_OSM =
    "src/test/resources/germany/deufringen-minimal.osm.pbf";
  public static final String BOEBLINGEN_OSM =
    "src/test/resources/germany/boeblingen-minimal.osm.pbf";
  public static final String VVS_BUS_764_ONLY =
    "src/test/resources/germany/vvs-bus-764-only.gtfs.zip";
  public static final String VVS_BUS_751_ONLY =
    "src/test/resources/germany/vvs-bus-751-only.gtfs.zip";
  public static final String HERRENBERG_HINDENBURG_STR_UNDER_CONSTRUCTION_OSM =
    "src/test/resources/germany/herrenberg-hindenburgstr-under-construction.osm.pbf";
  public static final String HERRENBERG_BARRIER_GATES_OSM =
    "src/test/resources/germany/herrenberg-barrier-gates.osm.pbf";
  public static final String HERRENBERG_OSM =
    "src/test/resources/germany/herrenberg-minimal.osm.pbf";
  public static final String STUTTGART_SCHWABSTR_OSM =
    "src/test/resources/germany/stuttgart-schwabstrasse.osm.pbf";
  public static final String ISLAND_PRUNE_OSM =
    "src/test/resources/germany/herrenberg-island-prune-nothru.osm.pbf";

  private static final CompositeDataSource NETEX_MINIMAL_DATA_SOURCE = new ZipFileDataSource(
    new File(NETEX_DIR, NETEX_FILENAME),
    FileType.NETEX
  );

  private static ConstantsForTests instance = null;
  private Graph portlandGraph = null;
  private Graph portlandGraphWithElevation = null;

  private ConstantsForTests() {}

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
   * Builds a new graph using the Portland test data.
   */
  public static Graph buildNewPortlandGraph(boolean withElevation) {
    try {
      Graph graph = new Graph();
      // Add street data from OSM
      {
        File osmFile = new File(PORTLAND_CENTRAL_OSM);
        OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, false);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProvider);
        osmModule.staticBikeParkAndRide = true;
        osmModule.staticParkAndRide = true;
        osmModule.skipVisibility = true;
        osmModule.buildGraph(graph, new HashMap<>());
      }
      // Add transit data from GTFS
      {
        addGtfsToGraph(graph, PORTLAND_GTFS, new DefaultFareServiceFactory(), "prt");
      }
      // Link transit stops to streets
      {
        GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
        streetTransitLinker.buildGraph(graph, new HashMap<>());
      }
      // Add elevation data
      if (withElevation) {
        var elevationModule = new ElevationModule(
          new GeotiffGridCoverageFactoryImpl(new File(PORTLAND_NED_WITH_NODATA))
        );
        elevationModule.buildGraph(graph, new HashMap<>());
      }

      graph.hasStreets = true;

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
      OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, true);
      OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProvider);
      osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
      osmModule.skipVisibility = true;
      osmModule.buildGraph(graph, new HashMap<>());
      return graph;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Graph buildOsmAndGtfsGraph(String osmPath, String gtfsPath) {
    var graph = buildOsmGraph(osmPath);

    addGtfsToGraph(graph, gtfsPath, new DefaultFareServiceFactory(), null);

    // Link transit stops to streets
    GraphBuilderModule streetTransitLinker = new StreetLinkerModule();
    streetTransitLinker.buildGraph(graph, new HashMap<>());
    return graph;
  }

  public static Graph buildGtfsGraph(String gtfsPath) {
    return buildGtfsGraph(gtfsPath, new DefaultFareServiceFactory());
  }

  public static Graph buildGtfsGraph(String gtfsPath, FareServiceFactory fareServiceFactory) {
    var graph = new Graph();
    addGtfsToGraph(graph, gtfsPath, fareServiceFactory, null);
    return graph;
  }

  public static Graph buildNewMinimalNetexGraph() {
    try {
      Graph graph = new Graph();
      // Add street data from OSM
      {
        File osmFile = new File(OSLO_EAST_OSM);

        OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, false);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(osmProvider);
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

  /**
   * Returns a cached copy of the Portland graph, which may have been initialized.
   */
  public synchronized Graph getCachedPortlandGraph() {
    if (portlandGraph == null) {
      portlandGraph = buildNewPortlandGraph(false);
    }
    return portlandGraph;
  }

  /**
   * Returns a cached copy of the Portland graph, which may have been initialized.
   */
  public synchronized Graph getCachedPortlandGraphWithElevation() {
    if (portlandGraphWithElevation == null) {
      portlandGraphWithElevation = buildNewPortlandGraph(true);
    }
    return portlandGraphWithElevation;
  }

  private static void addGtfsToGraph(
    Graph graph,
    String file,
    FareServiceFactory fareServiceFactory,
    @Nullable String feedId
  ) {
    var bundle = new GtfsBundle(new File(file));
    bundle.setFeedId(new GtfsFeedId.Builder().id(feedId).build());

    var module = new GtfsModule(List.of(bundle), ServiceDateInterval.unbounded());
    module.setFareServiceFactory(fareServiceFactory);

    module.buildGraph(graph, new HashMap<>());

    graph.index();
    graph.hasTransit = true;
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
        RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(reader.get("network"));
        Map<RentalVehicleType, Integer> availability = Map.of(vehicleType, 2);
        station.vehicleTypesAvailable = availability;
        station.vehicleSpacesAvailable = availability;
        station.realTimeData = false;
        station.isKeepingVehicleRentalAtDestinationAllowed = true;

        VehicleRentalPlaceVertex stationVertex = new VehicleRentalPlaceVertex(graph, station);
        new VehicleRentalEdge(stationVertex, vehicleType.formFactor);

        linker.linkVertexPermanently(
          stationVertex,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BOTH_WAYS,
          (vertex, streetVertex) ->
            List.of(
              new StreetVehicleRentalLink((VehicleRentalPlaceVertex) vertex, streetVertex),
              new StreetVehicleRentalLink(streetVertex, (VehicleRentalPlaceVertex) vertex)
            )
        );
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static BuildConfig createNetexBuilderParameters() {
    return new ConfigLoader(new File(ConstantsForTests.NETEX_DIR)).loadBuildConfig();
  }
}
