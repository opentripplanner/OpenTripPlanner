package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.standalone.config.GraphConfig;
import org.opentripplanner.standalone.config.OTPConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Load a graph from the filesystem. Counterpart to the GraphBuilder for pre-built graphs.
 * TODO OTP2 reframe this as a Provider and wire it into the application.
 */
public class GraphLoader {

    static final String GRAPH_FILENAME = "Graph.obj";

    private static final Logger LOG = LoggerFactory.getLogger(GraphLoader.class);

    /**
     * Do the actual operation of graph loading. Load configuration if present, and startup the
     * router with the help of the router lifecycle manager.
     */
    public static Router loadGraph(GraphConfig config) {
        File graphFile = new File(config.getPath(), GRAPH_FILENAME);
        LOG.info("Loading graph from file '{}'", graphFile.getPath());
        try (InputStream is = new FileInputStream(graphFile)) {
            final Graph newGraph  = Graph.load(is);
            // Load configuration from disk or use the embedded configuration as fallback.
            JsonNode jsonConfig = config.routerConfig(newGraph.routerConfig);
            Router newRouter = new Router(newGraph);
            newRouter.startup(jsonConfig);
            return newRouter;
        } catch (Exception e) {
            LOG.error("Exception while loading graph: {}", e);
            return null;
        }
    }

}
