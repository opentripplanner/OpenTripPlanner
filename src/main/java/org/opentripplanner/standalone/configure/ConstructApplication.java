package org.opentripplanner.standalone.configure;

import jakarta.ws.rs.core.Application;
import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelAPI;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.framework.application.LogMDCSupport;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.OTPWebApplication;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.street.model.elevation.ElevationUtils;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.configure.UpdaterConfigurator;
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
  private final ConstructApplicationFactory factory;

  /**
   * Create a new OTP configuration instance for a given directory.
   */
  ConstructApplication(
    CommandLineParameters cli,
    Graph graph,
    TransitModel transitModel,
    WorldEnvelopeRepository worldEnvelopeRepository,
    ConfigModel config,
    GraphBuilderDataSources graphBuilderDataSources,
    DataImportIssueSummary issueSummary,
    EmissionsDataModel emissionsDataModel,
    @Nullable StopConsolidationRepository stopConsolidationRepository,
    StreetLimitationParameters streetLimitationParameters
  ) {
    this.cli = cli;
    this.graphBuilderDataSources = graphBuilderDataSources;

    // We create the optional GraphVisualizer here, because it would be significant more complex to
    // use Dagger DI to do it - passing in a parameter to enable it or not.
    var graphVisualizer = cli.visualize ? new GraphVisualizer(graph) : null;

    this.factory =
      DaggerConstructApplicationFactory
        .builder()
        .configModel(config)
        .graph(graph)
        .transitModel(transitModel)
        .graphVisualizer(graphVisualizer)
        .worldEnvelopeRepository(worldEnvelopeRepository)
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
      transitModel(),
      factory.worldEnvelopeRepository(),
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

    creatTransitLayerForRaptor(transitModel(), routerConfig().transitTuningConfig());

    /* Create updater modules from JSON config. */
    UpdaterConfigurator.configure(
      graph(),
      realtimeVehicleRepository(),
      vehicleRentalRepository(),
      transitModel(),
      routerConfig().updaterConfig()
    );

    initEllipsoidToGeoidDifference();

    initializeTransferCache(routerConfig().transitTuningConfig(), transitModel());

    if (OTPFeature.TransmodelGraphQlApi.isOn()) {
      TransmodelAPI.setUp(
        routerConfig().transmodelApi(),
        transitModel(),
        routerConfig().routingRequestDefaults()
      );
    }

    if (OTPFeature.SandboxAPIGeocoder.isOn()) {
      LOG.info("Creating debug client geocoder lucene index");
      LuceneIndex.forServer(createServerContext());
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
  public static void creatTransitLayerForRaptor(
    TransitModel transitModel,
    TransitTuningParameters tuningParameters
  ) {
    if (!transitModel.hasTransit() || transitModel.getTransitModelIndex() == null) {
      LOG.warn(
        "Cannot create Raptor data, that requires the graph to have transit data and be indexed."
      );
    }
    LOG.info("Creating transit layer for Raptor routing.");
    transitModel.setTransitLayer(TransitLayerMapper.map(tuningParameters, transitModel));
    transitModel.setRealtimeTransitLayer(new TransitLayer(transitModel.getTransitLayer()));
    transitModel.setTransitLayerUpdater(
      new TransitLayerUpdater(
        transitModel,
        transitModel.getTransitModelIndex().getServiceCodesRunningForDate()
      )
    );
  }

  public static void initializeTransferCache(
    TransitTuningParameters transitTuningConfig,
    TransitModel transitModel
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
        transitModel.getTransitLayer().getRaptorTransfersForRequest(request);

        //noinspection Convert2MethodRef
        progress.step(s -> LOG.info(s));
      });

      LOG.info(progress.completeMessage());
    }
  }

  public TransitModel transitModel() {
    return factory.transitModel();
  }

  public DataImportIssueSummary dataImportIssueSummary() {
    return factory.dataImportIssueSummary();
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
