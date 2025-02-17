package org.opentripplanner.standalone.configure;

import jakarta.ws.rs.core.Application;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelAPI;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.framework.application.LogMDCSupport;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorTransitDataMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.DebugUiConfig;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.OTPWebApplication;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.model.elevation.ElevationUtils;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.configure.UpdaterConfigurator;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.opentripplanner.visualizer.GraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for creating the top level services like the {@link OTPWebApplication}
 * and {@link GraphBuilder}. The purpose of this class is to wire the application, creating the
 * necessary Services and modules and putting them together. It is NOT responsible for starting or
 * running the application. The whole idea of this class is to separate application construction
 * from running it.
 * <p>
 * The top level construction class(this class) may delegate to other construction classes
 * to inject configuration and services into submodules. An instance of this class is created
 * using the {@link LoadApplication} - An application is constructed AFTER config and input files
 * are loaded.
 * <p>
 * THIS CLASS IS NOT THREAD SAFE - THE APPLICATION SHOULD BE CREATED IN ONE THREAD. This
 * should be really fast, since the only IO operations are reading config files and logging. Loading
 * transit or map data should NOT happen during this phase.
 */
public class ConstructApplication {

  private static final Logger LOG = LoggerFactory.getLogger(ConstructApplication.class);

  private final CommandLineParameters cli;
  private final GraphBuilderDataSources graphBuilderDataSources;
  /**
   * The OSM Info is injected into the graph-builder, but not the web-server; Hence not part of
   * the application context.
   */
  private final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private final ConstructApplicationFactory factory;

  /**
   * Create a new OTP configuration instance for a given directory.
   */
  ConstructApplication(
    CommandLineParameters cli,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    TimetableRepository timetableRepository,
    WorldEnvelopeRepository worldEnvelopeRepository,
    ConfigModel config,
    GraphBuilderDataSources graphBuilderDataSources,
    DataImportIssueSummary issueSummary,
    EmissionsDataModel emissionsDataModel,
    VehicleParkingRepository vehicleParkingRepository,
    @Nullable StopConsolidationRepository stopConsolidationRepository,
    StreetLimitationParameters streetLimitationParameters
  ) {
    this.cli = cli;
    this.graphBuilderDataSources = graphBuilderDataSources;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;

    // We create the optional GraphVisualizer here, because it would be significant more complex to
    // use Dagger DI to do it - passing in a parameter to enable it or not.
    var graphVisualizer = cli.visualize ? new GraphVisualizer(graph) : null;

    this.factory =
      DaggerConstructApplicationFactory
        .builder()
        .configModel(config)
        .graph(graph)
        .timetableRepository(timetableRepository)
        .graphVisualizer(graphVisualizer)
        .worldEnvelopeRepository(worldEnvelopeRepository)
        .vehicleParkingRepository(vehicleParkingRepository)
        .emissionsDataModel(emissionsDataModel)
        .dataImportIssueSummary(issueSummary)
        .stopConsolidationRepository(stopConsolidationRepository)
        .streetLimitationParameters(streetLimitationParameters)
        .build();
  }

  public ConstructApplicationFactory getFactory() {
    return factory;
  }

  /**
   * Create a new Grizzly server - call this method once, the new instance is created every time
   * this method is called.
   */
  public GrizzlyServer createGrizzlyServer() {
    return new GrizzlyServer(
      cli,
      createApplication(),
      routerConfig().server().apiProcessingTimeout()
    );
  }

  /**
   * Create the default graph builder.
   */
  public GraphBuilder createGraphBuilder() {
    LOG.info("Wiring up and configuring graph builder task.");
    return GraphBuilder.create(
      buildConfig(),
      graphBuilderDataSources,
      graph(),
      osmInfoGraphBuildRepository,
      factory.timetableRepository(),
      factory.worldEnvelopeRepository(),
      factory.vehicleParkingRepository(),
      factory.emissionsDataModel(),
      factory.stopConsolidationRepository(),
      factory.streetLimitationParameters(),
      cli.doLoadStreetGraph(),
      cli.doSaveStreetGraph()
    );
  }

  /**
   * The output data source to use for saving the serialized graph.
   * <p>
   * This method will return {@code null} if the graph should NOT be saved. The business logic to
   * make that decision is in the {@link GraphBuilderDataSources}.
   */
  @Nullable
  public DataSource graphOutputDataSource() {
    return graphBuilderDataSources.getOutputGraph();
  }

  private Application createApplication() {
    LOG.info("Wiring up and configuring server.");
    setupTransitRoutingServer();
    return new OTPWebApplication(routerConfig().server(), this::createServerContext);
  }

  private void setupTransitRoutingServer() {
    enableRequestTraceLogging();
    createMetricsLogging();

    createRaptorTransitData(timetableRepository(), routerConfig().transitTuningConfig());

    /* Create updater modules from JSON config. */
    UpdaterConfigurator.configure(
      graph(),
      realtimeVehicleRepository(),
      vehicleRentalRepository(),
      vehicleParkingRepository(),
      timetableRepository(),
      snapshotManager(),
      routerConfig().updaterConfig()
    );

    initEllipsoidToGeoidDifference();

    initializeTransferCache(routerConfig().transitTuningConfig(), timetableRepository());

    if (OTPFeature.TransmodelGraphQlApi.isOn()) {
      TransmodelAPI.setUp(
        routerConfig().transmodelApi(),
        timetableRepository(),
        routerConfig().routingRequestDefaults(),
        routerConfig().server().apiDocumentationProfile(),
        routerConfig().transitTuningConfig()
      );
    }

    if (OTPFeature.SandboxAPIGeocoder.isOn()) {
      LOG.info("Initializing geocoder");
      // eagerly initialize the geocoder
      this.factory.luceneIndex();
    }
  }

  private void initEllipsoidToGeoidDifference() {
    try {
      var c = factory.worldEnvelopeService().envelope().orElseThrow().center();
      double value = ElevationUtils.computeEllipsoidToGeoidDifference(c.latitude(), c.longitude());
      graph().initEllipsoidToGeoidDifference(value, c.latitude(), c.longitude());
    } catch (Exception e) {
      LOG.error("Error computing ellipsoid/geoid difference");
    }
  }

  /**
   * Create transit layer for Raptor routing. Here we map the scheduled timetables.
   */
  public static void createRaptorTransitData(
    TimetableRepository timetableRepository,
    TransitTuningParameters tuningParameters
  ) {
    if (!timetableRepository.hasTransit() || !timetableRepository.isIndexed()) {
      LOG.warn(
        "Cannot create Raptor data, that requires the graph to have transit data and be indexed."
      );
    }
    LOG.info("Creating transit layer for Raptor routing.");
    timetableRepository.setRaptorTransitData(
      RaptorTransitDataMapper.map(tuningParameters, timetableRepository)
    );
    timetableRepository.setRealtimeRaptorTransitData(
      new RaptorTransitData(timetableRepository.getRaptorTransitData())
    );
  }

  public static void initializeTransferCache(
    TransitTuningParameters transitTuningConfig,
    TimetableRepository timetableRepository
  ) {
    var transferCacheRequests = transitTuningConfig.transferCacheRequests();
    if (!transferCacheRequests.isEmpty()) {
      var progress = ProgressTracker.track(
        "Creating initial raptor transfer cache",
        1,
        transferCacheRequests.size()
      );

      LOG.info(progress.startMessage());

      transferCacheRequests.forEach(request -> {
        timetableRepository.getRaptorTransitData().initTransferCacheForRequest(request);

        //noinspection Convert2MethodRef
        progress.step(s -> LOG.info(s));
      });

      LOG.info(progress.completeMessage());
    }
  }

  public TimetableRepository timetableRepository() {
    return factory.timetableRepository();
  }

  public DataImportIssueSummary dataImportIssueSummary() {
    return factory.dataImportIssueSummary();
  }

  public OsmInfoGraphBuildRepository osmInfoGraphBuildRepository() {
    return osmInfoGraphBuildRepository;
  }

  public StopConsolidationRepository stopConsolidationRepository() {
    return factory.stopConsolidationRepository();
  }

  public RealtimeVehicleRepository realtimeVehicleRepository() {
    return factory.realtimeVehicleRepository();
  }

  public VehicleRentalRepository vehicleRentalRepository() {
    return factory.vehicleRentalRepository();
  }

  private TimetableSnapshotManager snapshotManager() {
    return factory.timetableSnapshotManager();
  }

  public VehicleParkingService vehicleParkingService() {
    return factory.vehicleParkingService();
  }

  public VehicleParkingRepository vehicleParkingRepository() {
    return factory.vehicleParkingRepository();
  }

  public Graph graph() {
    return factory.graph();
  }

  public WorldEnvelopeRepository worldEnvelopeRepository() {
    return factory.worldEnvelopeRepository();
  }

  public OtpConfig otpConfig() {
    return factory.config().otpConfig();
  }

  public RouterConfig routerConfig() {
    return factory.config().routerConfig();
  }

  public BuildConfig buildConfig() {
    return factory.config().buildConfig();
  }

  public DebugUiConfig debugUiConfig() {
    return factory.config().debugUiConfig();
  }

  public RaptorConfig<TripSchedule> raptorConfig() {
    return factory.raptorConfig();
  }

  public GraphVisualizer graphVisualizer() {
    return factory.graphVisualizer();
  }

  private OtpServerRequestContext createServerContext() {
    return factory.createServerContext();
  }

  private void enableRequestTraceLogging() {
    if (routerConfig().server().requestTraceLoggingEnabled()) {
      LogMDCSupport.enable();
    }
  }

  private void createMetricsLogging() {
    factory.metricsLogging();
  }

  public EmissionsDataModel emissionsDataModel() {
    return factory.emissionsDataModel();
  }

  public StreetLimitationParameters streetLimitationParameters() {
    return factory.streetLimitationParameters();
  }
}
