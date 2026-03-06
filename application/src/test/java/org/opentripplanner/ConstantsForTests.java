package org.opentripplanner;

import com.csvreader.CsvReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.DirectoryDataSource;
import org.opentripplanner.ext.fares.service.gtfs.v1.DefaultFareServiceFactory;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.ConfiguredCompositeDataSource;
import org.opentripplanner.graph_builder.module.TestStreetLinkerModule;
import org.opentripplanner.graph_builder.module.TurnRestrictionModule;
import org.opentripplanner.graph_builder.module.ned.ElevationModule;
import org.opentripplanner.graph_builder.module.ned.GeotiffGridCoverageFactoryImpl;
import org.opentripplanner.graph_builder.module.osm.OsmModuleTestFactory;
import org.opentripplanner.graph_builder.module.transfer.DirectTransferGenerator;
import org.opentripplanner.gtfs.graphbuilder.GtfsBundleTestFactory;
import org.opentripplanner.gtfs.graphbuilder.GtfsModule;
import org.opentripplanner.model.calendar.LocalDateInterval;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.netex.NetexBundle;
import org.opentripplanner.netex.configure.NetexConfigure;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalEdge;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.OtpConfigLoader;
import org.opentripplanner.street.internal.DefaultStreetRepository;
import org.opentripplanner.street.model.edge.LinkingDirection;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.time.DurationUtils;

public class ConstantsForTests {

  private static final ResourceLoader RES = ResourceLoader.of(ConstantsForTests.class);

  public static final File CALTRAIN_GTFS = RES.file("/gtfs/caltrain_gtfs.zip");

  private static final File PORTLAND_GTFS = RES.file("/portland/portland.gtfs.zip");

  private static final File PORTLAND_CENTRAL_OSM = RES.file(
    "/portland/portland-central-filtered.osm.pbf"
  );

  private static final String PORTLAND_BIKE_SHARE_CSV =
    "src/test/resources/portland/portland-vehicle-rental.csv";

  private static final String PORTLAND_NED_WITH_NODATA =
    "src/test/resources/portland/portland-ned-nodata.tif";

  private static final File OSLO_EAST_OSM = RES.file("oslo-east-filtered.osm.pbf");

  public static final File SIMPLE_GTFS = RES.file("/gtfs/simple/");

  public static final File SHAPE_DIST_GTFS = RES.file("/gtfs/shape_dist_traveled/");

  private static final String NETEX_NORDIC_DIR = "src/test/resources/netex/nordic";

  private static final String NETEX_NORDIC_DIR_NAME = "netex_minimal/";
  private static final String NETEX_EPIP_DIR = "src/test/resources/netex/epip/";
  private static final String NETEX_EPIP_DATA_DIR = NETEX_EPIP_DIR + "netex_epip_minimal/";

  private static final CompositeDataSource NETEX_MINIMAL_DATA_SOURCE = new DirectoryDataSource(
    new File(NETEX_NORDIC_DIR, NETEX_NORDIC_DIR_NAME),
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

  public static NetexBundle createMinimalNetexNordicBundle() {
    var buildConfig = createNetexNordicBuilderParameters();
    var netexZipFile = new File(NETEX_NORDIC_DIR, NETEX_NORDIC_DIR_NAME);

    var dataSource = new DirectoryDataSource(netexZipFile, FileType.NETEX);
    var configuredDataSource = new ConfiguredCompositeDataSource<>(
      dataSource,
      buildConfig.netexDefaults
    );
    var transitService = new TransitDataImportBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );

    return new NetexConfigure(buildConfig).netexBundle(transitService, configuredDataSource);
  }

  public static NetexBundle createMinimalNetexEpipBundle() {
    var buildConfig = createNetexEpipBuilderParameters();

    var netexZipFile = new File(NETEX_EPIP_DATA_DIR);

    var dataSource = new DirectoryDataSource(netexZipFile, FileType.NETEX);
    var configuredDataSource = new ConfiguredCompositeDataSource<>(
      dataSource,
      buildConfig.netexDefaults
    );
    var transitService = new TransitDataImportBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP
    );

    return new NetexConfigure(buildConfig).netexBundle(transitService, configuredDataSource);
  }

  /**
   * Builds a new graph using the Portland test data.
   */
  public static TestOtpModel buildNewPortlandGraph(boolean withElevation) {
    try {
      var deduplicator = new Deduplicator();
      var graph = new Graph();
      var timetableRepository = new TimetableRepository(new SiteRepository(), deduplicator);
      var fareFactory = new DefaultFareServiceFactory();
      // Add street data from OSM
      {
        var osmModule = OsmModuleTestFactory.of(new DefaultOsmProvider(PORTLAND_CENTRAL_OSM, false))
          .withGraph(graph)
          .builder()
          .withStaticParkAndRide(true)
          .withStaticBikeParkAndRide(true)
          .build();
        osmModule.buildGraph();
      }
      // Add transit data from GTFS
      {
        addGtfsToGraph(graph, timetableRepository, PORTLAND_GTFS, fareFactory, "prt");
      }
      // Link transit stops to streets
      TestStreetLinkerModule.link(graph, timetableRepository);

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

      var transferRepository = TransferServiceTestFactory.defaultTransferRepository();
      new DirectTransferGenerator(
        graph,
        timetableRepository,
        transferRepository,
        DataImportIssueStore.NOOP,
        Duration.ofMinutes(30),
        List.of(RouteRequest.defaultValue())
      ).buildGraph();

      graph.index();

      return new TestOtpModel(graph, timetableRepository, transferRepository, fareFactory);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static TestOtpModel buildOsmGraph(File osmFile) {
    try {
      var deduplicator = new Deduplicator();
      var siteRepository = new SiteRepository();
      var graph = new Graph();
      var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
      // Add street data from OSM
      var osmProvider = new DefaultOsmProvider(osmFile, true);
      var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
      var streetRepository = new DefaultStreetRepository();
      var vehicleParkingRepository = new DefaultVehicleParkingRepository();

      var osmModule = OsmModuleTestFactory.of(osmProvider)
        .withGraph(graph)
        .withOsmInfoGraphBuildRepository(osmInfoRepository)
        .withStreetRepository(streetRepository)
        .withVehicleParkingRepository(vehicleParkingRepository)
        .builder()
        .build();
      osmModule.buildGraph();
      TurnRestrictionModule turnRestrictionModule = new TurnRestrictionModule(
        graph,
        osmInfoRepository
      );
      turnRestrictionModule.buildGraph();
      TransferRepository transferRepository;
      if (OTPFeature.FlexRouting.isOn()) {
        transferRepository = TransferServiceTestFactory.withFlex();
      } else {
        transferRepository = TransferServiceTestFactory.defaultTransferRepository();
      }
      return new TestOtpModel(graph, timetableRepository, transferRepository);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static TestOtpModel buildOsmAndGtfsGraph(File osmPath, File gtfsPath) {
    var otpModel = buildOsmGraph(osmPath);

    addGtfsToGraph(
      otpModel.graph(),
      otpModel.timetableRepository(),
      gtfsPath,
      new DefaultFareServiceFactory(),
      null
    );

    // Link transit stops to streets
    TestStreetLinkerModule.link(otpModel.graph(), otpModel.timetableRepository());

    return otpModel;
  }

  public static TestOtpModel buildGtfsGraph(File gtfsPath) {
    return buildGtfsGraph(gtfsPath, new DefaultFareServiceFactory());
  }

  public static TestOtpModel buildGtfsGraph(File gtfsFile, FareServiceFactory fareServiceFactory) {
    var deduplicator = new Deduplicator();
    var siteRepository = new SiteRepository();
    var graph = new Graph();
    var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
    var transferRepository = TransferServiceTestFactory.defaultTransferRepository();
    addGtfsToGraph(graph, timetableRepository, gtfsFile, fareServiceFactory, null);
    return new TestOtpModel(graph, timetableRepository, transferRepository, fareServiceFactory);
  }

  public static TestOtpModel buildNewMinimalNetexGraph() {
    try {
      var deduplicator = new Deduplicator();
      var siteRepository = new SiteRepository();
      var parkingRepository = new DefaultVehicleParkingRepository();
      var graph = new Graph();
      var timetableRepository = new TimetableRepository(siteRepository, deduplicator);
      var streetDetailsRepository = new DefaultStreetDetailsRepository();
      // Add street data from OSM
      {
        var osmProvider = new DefaultOsmProvider(OSLO_EAST_OSM, false);
        var osmModule = OsmModuleTestFactory.of(osmProvider)
          .withGraph(graph)
          .withVehicleParkingRepository(parkingRepository)
          .builder()
          .build();
        osmModule.buildGraph();
      }
      // Add transit data from Netex
      {
        var buildConfig = createNetexNordicBuilderParameters();
        var netexConfig = buildConfig.netexDefaults
          .copyOf()
          .withSource(NETEX_MINIMAL_DATA_SOURCE.uri())
          .build();
        var sources = List.of(
          new ConfiguredCompositeDataSource<>(NETEX_MINIMAL_DATA_SOURCE, netexConfig)
        );

        new NetexConfigure(buildConfig)
          .createNetexModule(
            sources,
            timetableRepository,
            parkingRepository,
            streetDetailsRepository,
            graph,
            deduplicator,
            DataImportIssueStore.NOOP
          )
          .buildGraph();
      }
      // Link transit stops to streets
      TestStreetLinkerModule.link(graph, timetableRepository);

      return new TestOtpModel(
        graph,
        timetableRepository,
        TransferServiceTestFactory.defaultTransferRepository()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void addGtfsToGraph(
    Graph graph,
    TimetableRepository timetableRepository,
    File file,
    FareServiceFactory fareServiceFactory,
    @Nullable String feedId
  ) {
    var bundle = GtfsBundleTestFactory.forTest(file, feedId);

    var module = new GtfsModule(
      List.of(bundle),
      timetableRepository,
      new DefaultStreetDetailsRepository(),
      graph,
      new Deduplicator(),
      DataImportIssueStore.NOOP,
      LocalDateInterval.unbounded(),
      fareServiceFactory,
      150.0,
      DurationUtils.durationInSeconds("2m")
    );

    module.buildGraph();

    timetableRepository.index();
    graph.index();
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

  private static void addPortlandVehicleRentals(Graph graph) {
    try {
      VertexLinker linker = VertexLinkerTestFactory.of(graph);
      CsvReader reader = new CsvReader(PORTLAND_BIKE_SHARE_CSV, ',', StandardCharsets.UTF_8);
      reader.readHeaders();
      while (reader.readRecord()) {
        RentalVehicleType vehicleType = RentalVehicleType.getDefaultType(reader.get("network"));
        Map<RentalVehicleType, Integer> availability = Map.of(vehicleType, 2);

        VehicleRentalStation station = VehicleRentalStation.of()
          .withId(new FeedScopedId(reader.get("network"), reader.get("osm_id")))
          .withLatitude(Double.parseDouble(reader.get("lat")))
          .withLongitude(Double.parseDouble(reader.get("lon")))
          .withName(new NonLocalizedString(reader.get("osm_id")))
          .withVehicleTypesAvailable(availability)
          .withVehicleSpacesAvailable(availability)
          .withRealTimeData(false)
          .withIsArrivingInRentalVehicleAtDestinationAllowed(true)
          .build();

        VehicleRentalPlaceVertex stationVertex = new VehicleRentalPlaceVertex(station);
        graph.addVertex(stationVertex);
        VehicleRentalEdge.createVehicleRentalEdge(stationVertex, vehicleType.formFactor());

        linker.linkVertexPermanently(
          stationVertex,
          new TraverseModeSet(TraverseMode.WALK),
          LinkingDirection.BIDIRECTIONAL,
          (vertex, streetVertex) ->
            List.of(
              StreetVehicleRentalLink.createStreetVehicleRentalLink(
                (VehicleRentalPlaceVertex) vertex,
                streetVertex
              ),
              StreetVehicleRentalLink.createStreetVehicleRentalLink(
                streetVertex,
                (VehicleRentalPlaceVertex) vertex
              )
            )
        );
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static BuildConfig createNetexNordicBuilderParameters() {
    return new OtpConfigLoader(new File(ConstantsForTests.NETEX_NORDIC_DIR)).loadBuildConfig();
  }

  private static BuildConfig createNetexEpipBuilderParameters() {
    return new OtpConfigLoader(new File(ConstantsForTests.NETEX_EPIP_DIR)).loadBuildConfig();
  }
}
