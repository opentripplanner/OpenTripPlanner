package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.standalone.config.GraphConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Load a graph from the filesystem. Counterpart to the GraphBuilder for pre-built graphs.
 * TODO OTP2 reframe this as a Provider and wire it into the application.
 */
public class GraphLoader {

    private static final String GRAPH_FILENAME = "graph.obj";

    private static final Logger LOG = LoggerFactory.getLogger(GraphLoader.class);

    /**
     * Do the actual operation of graph loading. Load configuration if present, and startup the
     * router with the help of the router lifecycle manager.
     */
    public static Router loadGraph(GraphConfig config) {
        File graphFile = new File(config.getPath(), GRAPH_FILENAME);
        LOG.info("Loading graph from file '{}'", graphFile.getPath());
        Graph newGraph = loadGraph(graphFile);
        // Load configuration from disk or use the embedded configuration as fallback.
        JsonNode jsonConfig = config.routerConfig(newGraph.routerConfig);
        Router newRouter = new Router(newGraph);
        newRouter.startup(jsonConfig);
        return newRouter;
    }


    /**
     * Load graph, but skip loading routing config. This is public to allow other usages
     * than running the OTPMain, which uses the {@link #loadGraph(GraphConfig)}.
     */
    public static Graph loadGraph(File graphFile) {
        LOG.info("Loading graph from file '{}'", graphFile.getPath());
        try (InputStream is = new FileInputStream(graphFile)) {
            return Graph.load(is);
        } catch (IOException e) {
            LOG.error("Exception while loading graph: " + e.getLocalizedMessage(), e);
            throw  new RuntimeException(e.getLocalizedMessage(), e);
        }
    }
}
