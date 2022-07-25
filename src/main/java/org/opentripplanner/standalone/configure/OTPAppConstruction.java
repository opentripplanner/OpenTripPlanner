package org.opentripplanner.standalone.configure;

import javax.annotation.Nullable;
import javax.ws.rs.core.Application;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.transmodelapi.TransmodelAPI;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerContext;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerContext;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.MetricsLogging;
import org.opentripplanner.standalone.server.OTPWebApplication;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.util.OTPFeature;
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
 * to inject configuration and services into sub-modules.
 * <p>
 * THIS CLASS IS NOT THREAD SAFE - THE APPLICATION SHOULD BE CREATED IN ONE THREAD. This
 * should be really fast, since the only IO operations are reading config files and logging. Loading
 * transit or map data should NOT happen during this phase.
 */
public class OTPAppConstruction {

  private static final Logger LOG = LoggerFactory.getLogger(OTPAppConstruction.class);

  private final CommandLineParameters cli;
  private final OTPApplicationFactory factory;
  private GraphBuilderDataSources graphBuilderDataSources = null;

  /**
   * Create a new OTP configuration instance for a given directory.
   */
  public OTPAppConstruction(CommandLineParameters commandLineParameters) {
    this.cli = commandLineParameters;
    this.factory =
      DaggerOTPApplicationFactory.builder().baseDirectory(this.cli.getBaseDirectory()).build();
  }

  public OTPApplicationFactory getFactory() {
    return factory;
  }

  /**
   * Create a new Grizzly server - call this method once, the new instance is created every time
   * this method is called.
   */
  public GrizzlyServer createGrizzlyServer(DefaultServerContext serverContext) {
    return new GrizzlyServer(cli, createApplication(serverContext));
  }

  public void validateConfigAndDataSources() {
    // Load Graph Builder Data Sources to validate it.
    graphBuilderDataSources();
  }

  /**
   * Create the default graph builder.
   *
   * @param baseGraph the base graph to add more data on to of.
   */
  public GraphBuilder createGraphBuilder(Graph baseGraph) {
    LOG.info("Wiring up and configuring graph builder task.");
    return GraphBuilder.create(
      factory.buildConfig(),
      graphBuilderDataSources(),
      baseGraph,
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
    return graphBuilderDataSources().getOutputGraph();
  }

  private GraphBuilderDataSources graphBuilderDataSources() {
    if (graphBuilderDataSources == null) {
      graphBuilderDataSources =
        GraphBuilderDataSources.create(cli, factory.buildConfig(), factory.datastore());
    }
    return graphBuilderDataSources;
  }

  private Application createApplication(DefaultServerContext serverContext) {
    LOG.info("Wiring up and configuring server.");
    setupTransitRoutingServer(serverContext);
    return new OTPWebApplication(serverContext);
  }

  private void setupTransitRoutingServer(OtpServerContext context) {
    new MetricsLogging(context);

    creatTransitLayerForRaptor(context.transitModel(), context.routerConfig());

    /* Create Graph updater modules from JSON config. */
    GraphUpdaterConfigurator.setupGraph(
      context.graph(),
      context.transitModel(),
      context.routerConfig().updaterConfig()
    );

    context.graph().initEllipsoidToGeoidDifference();

    if (OTPFeature.SandboxAPITransmodelApi.isOn()) {
      TransmodelAPI.setUp(
        context.routerConfig().transmodelApi(),
        context.transitModel(),
        context.defaultRoutingRequest()
      );
    }

    if (OTPFeature.SandboxAPIGeocoder.isOn()) {
      LOG.info("Creating debug client geocoder lucene index");
      LuceneIndex.forServer(context);
    }
  }

  /**
   * Create transit layer for Raptor routing. Here we map the scheduled timetables.
   */
  public static void creatTransitLayerForRaptor(
    TransitModel transitModel,
    RouterConfig routerConfig
  ) {
    if (!transitModel.hasTransit() || transitModel.getTransitModelIndex() == null) {
      LOG.warn(
        "Cannot create Raptor data, that requires the graph to have transit data and be indexed."
      );
    }
    LOG.info("Creating transit layer for Raptor routing.");
    transitModel.setTransitLayer(
      TransitLayerMapper.map(routerConfig.transitTuningParameters(), transitModel)
    );
    transitModel.setRealtimeTransitLayer(new TransitLayer(transitModel.getTransitLayer()));
    transitModel.setTransitLayerUpdater(
      new TransitLayerUpdater(
        transitModel,
        transitModel.getTransitModelIndex().getServiceCodesRunningForDate()
      )
    );
  }
}
