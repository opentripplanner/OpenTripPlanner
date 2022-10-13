package org.opentripplanner;

import static org.opentripplanner.graph_builder.DataImportIssueStore.noopIssueStore;

import com.csvreader.CsvReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.ZipFileDataSource;
import org.opentripplanner.ext.fares.impl.DefaultFareServiceFactory;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.linking.LinkingDirection;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundle;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.netex.configure.NetexConfigure;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetVehicleRentalLink;
import org.opentripplanner.routing.edgetype.VehicleRentalEdge;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.routing.vertextype.VehicleRentalPlaceVertex;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

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
  private TestOtpModel portlandGraph = null;
  private TestOtpModel portlandGraphWithElevation = null;

  private ConstantsForTests() {}

  public static ConstantsForTests getInstance() {
    if (instance == null) {
      instance = new ConstantsForTests();
    }
    return instance;
  }

  public static NetexBundle createMinimalNetexBundle() {
    return NetexConfigure.netexBundleForTest(
      createNetexBuilderParameters(),
      new File(ConstantsForTests.NETEX_DIR, ConstantsForTests.NETEX_FILENAME)
    );
  }

  /**
   * Builds a new graph using the Portland test data.
   */
  public static TestOtpModel buildNewPortlandGraph(boolean withElevation) {
    try {
      var deduplicator = new Deduplicator();
      var graph = new Graph(deduplicator);
      var transitModel = new TransitModel(new StopModel(), deduplicator);
      // Add street data from OSM
      {
        File osmFile = new File(PORTLAND_CENTRAL_OSM);
        OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, false);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(
          List.of(osmProvider),
          Set.of(),
          // Need to use a mutable set here, since it is used
          graph,
          transitModel.getTimeZone(),
          noopIssueStore()
        );
        osmModule.staticBikeParkAndRide = true;
        osmModule.staticParkAndRide = true;
        osmModule.skipVisibility = true;
        osmModule.buildGraph();
      }
      // Add transit data from GTFS
      {
        addGtfsToGraph(graph, transitModel, PORTLAND_GTFS, new DefaultFareServiceFactory(), "prt");
      }
      // Link transit stops to streets
      StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

      // Add elevation data
      if (withElevation) {
        var elevationModule = new ElevationModule(
          new GeotiffGridCoverageFactoryImpl(new File(PORTLAND_NED_WITH_NODATA)),
          graph
        );
        elevationModule.buildGraph();
      }

      graph.hasStreets = true;

      addPortlandVehicleRentals(graph);

      transitModel.index();
      graph.index(transitModel.getStopModel());

      return new TestOtpModel(graph, transitModel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static TestOtpModel buildOsmGraph(String osmPath) {
    try {
      var deduplicator = new Deduplicator();
      var stopModel = new StopModel();
      var graph = new Graph(deduplicator);
      var transitModel = new TransitModel(stopModel, deduplicator);
      // Add street data from OSM
      File osmFile = new File(osmPath);
      OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, true);
      OpenStreetMapModule osmModule = new OpenStreetMapModule(
        List.of(osmProvider),
        Set.of(),
        graph,
        transitModel.getTimeZone(),
        noopIssueStore()
      );
      osmModule.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
      osmModule.skipVisibility = true;
      osmModule.buildGraph();
      return new TestOtpModel(graph, transitModel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static TestOtpModel buildOsmAndGtfsGraph(String osmPath, String gtfsPath) {
    var otpModel = buildOsmGraph(osmPath);

    addGtfsToGraph(
      otpModel.graph(),
      otpModel.transitModel(),
      gtfsPath,
      new DefaultFareServiceFactory(),
      null
    );

    // Link transit stops to streets
    StreetLinkerModule.linkStreetsForTestOnly(otpModel.graph(), otpModel.transitModel());

    return otpModel;
  }

  public static TestOtpModel buildGtfsGraph(String gtfsPath) {
    return buildGtfsGraph(gtfsPath, new DefaultFareServiceFactory());
  }

  public static TestOtpModel buildGtfsGraph(
    String gtfsPath,
    FareServiceFactory fareServiceFactory
  ) {
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);
    addGtfsToGraph(graph, transitModel, gtfsPath, fareServiceFactory, null);
    return new TestOtpModel(graph, transitModel);
  }

  public static TestOtpModel buildNewMinimalNetexGraph() {
    try {
      var deduplicator = new Deduplicator();
      var stopModel = new StopModel();
      var graph = new Graph(deduplicator);
      var transitModel = new TransitModel(stopModel, deduplicator);
      // Add street data from OSM
      {
        File osmFile = new File(OSLO_EAST_OSM);

        OpenStreetMapProvider osmProvider = new OpenStreetMapProvider(osmFile, false);
        OpenStreetMapModule osmModule = new OpenStreetMapModule(
          List.of(osmProvider),
          Set.of(),
          graph,
          transitModel.getTimeZone(),
          noopIssueStore()
        );
        osmModule.skipVisibility = true;
        osmModule.buildGraph();
      }
      // Add transit data from Netex
      {
        var buildConfig = createNetexBuilderParameters();
        var netexConfig = buildConfig.netexDefaults
          .copyOf()
          .withSource(NETEX_MINIMAL_DATA_SOURCE.uri())
          .build();
        var sources = List.of(new ConfiguredDataSource<>(NETEX_MINIMAL_DATA_SOURCE, netexConfig));

        new NetexConfigure(buildConfig)
          .createNetexModule(sources, transitModel, graph, noopIssueStore())
          .buildGraph();
      }
      // Link transit stops to streets
      StreetLinkerModule.linkStreetsForTestOnly(graph, transitModel);

      return new TestOtpModel(graph, transitModel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a cached copy of the Portland graph, which may have been initialized.
   */
  public synchronized TestOtpModel getCachedPortlandGraph() {
    if (portlandGraph == null) {
      portlandGraph = buildNewPortlandGraph(false);
    }
    return portlandGraph;
  }

  /**
   * Returns a cached copy of the Portland graph, which may have been initialized.
   */
  public synchronized TestOtpModel getCachedPortlandGraphWithElevation() {
    if (portlandGraphWithElevation == null) {
      portlandGraphWithElevation = buildNewPortlandGraph(true);
    }
    return portlandGraphWithElevation;
  }

  public static void addGtfsToGraph(
    Graph graph,
    TransitModel transitModel,
    String file,
    FareServiceFactory fareServiceFactory,
    @Nullable String feedId
  ) {
    var bundle = new GtfsBundle(new File(file));
    bundle.setFeedId(new GtfsFeedId.Builder().id(feedId).build());

    var module = new GtfsModule(
      List.of(bundle),
      transitModel,
      graph,
      noopIssueStore(),
      ServiceDateInterval.unbounded(),
      fareServiceFactory,
      false,
      true,
      300
    );

    module.buildGraph();

    transitModel.index();
    graph.index(transitModel.getStopModel());
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
        station.isArrivingInRentalVehicleAtDestinationAllowed = true;

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
