package org.opentripplanner.standalone.configure;

import javax.annotation.Nullable;
import javax.ws.rs.core.Application;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.GraphBuilder;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.server.GrizzlyServer;
import org.opentripplanner.standalone.server.MetricsLogging;
import org.opentripplanner.standalone.server.OTPApplication;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for creating the top level services like the {@link OTPServer}. The
 * purpose of this class is to wire the application, creating the necessary Services and modules
 * and putting them together. It is NOT responsible for starting or running the application. The
 * whole idea of this class is to separate application construction from running it.
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
  private OTPServer server = null;
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
  public GrizzlyServer createGrizzlyServer(Router router) {
    return new GrizzlyServer(cli, createApplication(router));
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

  private Application createApplication(Router router) {
    return new OTPApplication(server(router));
  }

  /**
   * Create the top-level objects that represent the OTP server. There is one server and it is
   * created lazy at the first invocation of this method.
   * <p>
   * The method is {@code public} to allow test access.
   */
  private OTPServer server(Router router) {
    if (server == null) {
      server = new OTPServer(cli, router);
      new MetricsLogging(server);
    }
    return server;
  }
}
