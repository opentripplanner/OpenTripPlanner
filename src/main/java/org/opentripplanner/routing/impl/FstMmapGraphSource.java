/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import com.conveyal.r5.util.ExpandingMMFBytez;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.io.ByteStreams;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;

/**
 * Implementation of GraphSource for loading FST-serialized graphs.
 */
public class FstMmapGraphSource implements GraphSource {

    public static final String GRAPH_FILENAME = "Graph.obj";

    private static final Logger LOG = LoggerFactory.getLogger(FstMmapGraphSource.class);

    private Router router;

    private String routerId;

    private File path;

    private StreetVertexIndexFactory streetVertexIndexFactory = new DefaultStreetVertexIndexFactory();

    /**
     * @return A GraphSource loading an FST-serialized graph from the file system under a base path.
     */
    public FstMmapGraphSource (String routerId, File path) {
        this.routerId = routerId;
        this.path = new File(path, routerId);
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public synchronized boolean reload(boolean force, boolean preEvict) {
        if (preEvict) {
            evict();
        }
        router = loadGraph();
        return true;
    }

    @Override
    public void evict() {
        synchronized (this) {
            if (router != null) {
                router.shutdown();
                router = null;
            }
        }
    }

    /**
     * Do the actual operation of graph loading. Load configuration if present, and startup the
     * router with the help of the router lifecycle manager.
     */
    private Router loadGraph() {
        final Graph graph;
        LOG.info("Loading graph...");
        try {
            graph = Graph.loadFromFSTMMF(new File(path, GRAPH_FILENAME));
            graph.routerId = routerId;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        JsonNode config = MissingNode.getInstance();
        try {
            File configFile = new File(path, Router.ROUTER_CONFIG_FILENAME);
            if (configFile.canRead()) {
                LOG.debug("Loading config from file '{}'", configFile.getPath());
                config = mapper.readTree(new FileInputStream(configFile));
            } else if (graph.routerConfig != null) {
                config = mapper.readTree(graph.routerConfig);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Router router = new Router(routerId, graph);
        router.startup(config);
        return router;
    }

    public static class Factory implements GraphSource.Factory {

        private File graphDirectory;

        public Factory(File graphDirectory) {
            this.graphDirectory = graphDirectory;
        }

        @Override
        public GraphSource createGraphSource(String routerId) {
            return new FstMmapGraphSource(routerId, graphDirectory);
        }

        @Override
        public boolean save(String routerId, InputStream is) {
            throw new UnsupportedOperationException();
        }

    }


}
