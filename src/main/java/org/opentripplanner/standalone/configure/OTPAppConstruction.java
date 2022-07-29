package org.opentripplanner.standalone.configure;

import io.micrometer.core.instrument.Metrics;
import javax.annotation.Nullable;
import javax.ws.rs.core.Application;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.geocoder.LuceneIndex;
import org.opentripplanner.ext.transmodelapi.TransmodelAPI;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerContext;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.DefaultServerContext;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.MetricsLogging;
import org.opentripplanner.standalone.server.OTPWebApplication;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
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
  private DefaultServerContext context;
  private GraphVisualizer graphVisualizer;

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
   * After the graph and transitModel is read from file or build, then it should be set here,
   * so it can be used during construction of the web server.
   */
  public void updateModel(Graph graph, TransitModel transitModel) {
    getFactory().graph().set(graph);
    getFactory().transitModel().set(transitModel);

    this.context =
      DefaultServerContext.create(
        factory.configModel().routerConfig(),
        factory.raptorConfig(),
        factory.graph().get(),
        factory.transitModel().get(),
        Metrics.globalRegistry,
        traverseVisitor()
      );
  }

  public OtpServerContext serverContext() {
    return context;
  }

  /**
   * Create a new Grizzly server - call this method once, the new instance is created every time
   * this method is called.
   */
  public GrizzlyServer createGrizzlyServer() {
    return new GrizzlyServer(cli, createApplication());
  }

  public void validateConfigAndDataSources() {
    // Load Graph Builder Data Sources to validate it.
    graphBuilderDataSources();
  }

  /**
   * Create the default graph builder.
   */
  public GraphBuilder createGraphBuilder() {
    LOG.info("Wiring up and configuring graph builder task.");
    return GraphBuilder.create(
      buildConfig(),
      graphBuilderDataSources(),
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
    return graphBuilderDataSources().getOutputGraph();
  }

  private GraphBuilderDataSources graphBuilderDataSources() {
    if (graphBuilderDataSources == null) {
      graphBuilderDataSources =
        GraphBuilderDataSources.create(cli, buildConfig(), factory.datastore());
    }
    return graphBuilderDataSources;
  }

  private Application createApplication() {
    LOG.info("Wiring up and configuring server.");
    setupTransitRoutingServer();
    return new OTPWebApplication(() -> context.createHttpRequestScopedCopy());
  }

  public GraphVisualizer graphVisualizer() {
    if (cli.visualize && graphVisualizer == null) {
      graphVisualizer =
        new GraphVisualizer(
          factory.graph().get(),
          factory.configModel().routerConfig().streetRoutingTimeout()
        );
    }
    return graphVisualizer;
  }

  public TraverseVisitor traverseVisitor() {
    var gv = graphVisualizer();
    return gv == null ? null : gv.traverseVisitor;
  }

  private void setupTransitRoutingServer() {
    new MetricsLogging(transitModel(), raptorConfig());

    creatTransitLayerForRaptor(transitModel(), routerConfig());

    /* Create Graph updater modules from JSON config. */
    GraphUpdaterConfigurator.setupGraph(graph(), transitModel(), routerConfig().updaterConfig());

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

  public RaptorConfig<TripSchedule> raptorConfig() {
    return factory.raptorConfig();
  }

  public TransitModel transitModel() {
    return getFactory().transitModel().get();
  }

  public Graph graph() {
    return getFactory().graph().get();
  }

  public Deduplicator deduplicator() {
    return getFactory().deduplicator();
  }

  private BuildConfig buildConfig() {
    return getFactory().configModel().buildConfig();
  }

  private RouterConfig routerConfig() {
    return getFactory().configModel().routerConfig();
  }
}
