package org.opentripplanner.standalone.configure;

import javax.annotation.Nullable;
import javax.ws.rs.core.Application;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.transmodelapi.TransmodelAPI;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.OTPWebApplication;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.configure.UpdaterConfigurator;
import org.opentripplanner.util.OTPFeature;
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
    ConfigModel config,
    GraphBuilderDataSources graphBuilderDataSources
  ) {
    this.cli = cli;
    this.graphBuilderDataSources = graphBuilderDataSources;

    // We create the optional GraphVisualizer here, because it would be significant more complex to
    // use Dagger DI to do it - passing in a parameter to enable it or not.
    var graphVisualizer = cli.visualize
      ? new GraphVisualizer(graph, config.routerConfig().streetRoutingTimeout())
      : null;

    this.factory =
      DaggerConstructApplicationFactory
        .builder()
        .configModel(config)
        .graph(graph)
        .transitModel(transitModel)
        .graphVisualizer(graphVisualizer)
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
    return new GrizzlyServer(cli, createApplication());
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
    return new OTPWebApplication(this::createServerContext);
  }

  private void setupTransitRoutingServer() {
    // Create MetricsLogging
    factory.metricsLogging();

    creatTransitLayerForRaptor(transitModel(), routerConfig().transitTuningConfig());

    /* Create updater modules from JSON config. */
    UpdaterConfigurator.configure(graph(), transitModel(), routerConfig().updaterConfig());

    graph().initEllipsoidToGeoidDifference();

    if (OTPFeature.SandboxAPITransmodelApi.isOn()) {
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

  public TransitModel transitModel() {
    return factory.transitModel();
  }

  public Graph graph() {
    return factory.graph();
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
}
