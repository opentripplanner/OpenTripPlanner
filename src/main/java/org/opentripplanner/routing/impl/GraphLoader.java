package org.opentripplanner.routing.impl;

import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.standalone.configure.OTPAppConstruction;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load a graph from the filesystem. Counterpart to the GraphBuilder for pre-built graphs. TODO OTP2
 * reframe this as a Provider and wire it into the application.
 */
public class GraphLoader {

  private static final Logger LOG = LoggerFactory.getLogger(GraphLoader.class);

  /**
   * Do the actual operation of graph loading. Load configuration if present, and startup the router
   * with the help of the router lifecycle manager.
   */
  public static Router loadGraph(OTPAppConstruction app) {
    app.validateConfigAndDataSources();
    LOG.info("Loading graph from file '{}'", app.store().getGraph());
    DataSource inputGraph = app.config().getCli().doLoadGraph()
        ? app.store().getGraph()
        : app.store().getStreetGraph();
    SerializedGraphObject obj = SerializedGraphObject.load(inputGraph);
    Graph newGraph = obj.graph;
    app.config().updateConfigFromSerializedGraph(obj.buildConfig, obj.routerConfig);

    Router newRouter = new Router(newGraph, app.config().routerConfig());
    newRouter.startup();
    return newRouter;

  }

}
