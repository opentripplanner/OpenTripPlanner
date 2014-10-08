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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.prefs.Preferences;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.updater.PropertiesPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The primary implementation of the GraphSource interface. The graph is loaded from a serialized
 * graph file in a given directory.
 * 
 * TODO: Implement graph auto-reload when source file has changed.
 */
public class FileGraphSource implements GraphSource {

    private static final Logger LOG = LoggerFactory.getLogger(FileGraphSource.class);

    public static final String GRAPH_FILENAME = "Graph.obj";

    public static final String CONFIG_FILENAME = "Graph.properties";

    private Graph graph;

    private String routerId;

    private String path;

    private LoadLevel loadLevel;

    // TODO Why do we need a factory? There is a single one implementation.
    private StreetVertexIndexFactory streetVertexIndexFactory = new DefaultStreetVertexIndexFactory();

    private GraphUpdaterConfigurator decorator = new GraphUpdaterConfigurator();

    public FileGraphSource(String routerId, String path, LoadLevel loadLevel) {
        this.routerId = routerId;
        this.path = path;
        this.loadLevel = loadLevel;
        this.reload(false);
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public boolean reload(boolean preEvict) {
        synchronized (this) {
            if (preEvict) {
                Graph oldGraph = graph;
                graph = null;
                if (oldGraph != null)
                    decorator.shutdownGraph(oldGraph);
                graph = loadGraph();
            } else {
                Graph newGraph = loadGraph();
                Graph oldGraph = graph;
                graph = newGraph;
                if (oldGraph != null)
                    decorator.shutdownGraph(oldGraph);
            }
            return (graph != null);
        }
    }

    @Override
    public void evict() {
        if (graph != null) {
            decorator.shutdownGraph(graph);
        }
    }

    private Graph loadGraph() {
        LOG.debug("loading serialized graph for routerId {}", routerId);

        String graphFileName = path + GRAPH_FILENAME;
        String configFileName = path + CONFIG_FILENAME;

        LOG.debug("graph file for routerId '{}' is at {}", routerId, graphFileName);
        InputStream is = null;
        final String CLASSPATH_PREFIX = "classpath:/";
        if (graphFileName.startsWith(CLASSPATH_PREFIX)) {
            // look for graph on classpath
            String resourceName = graphFileName.substring(CLASSPATH_PREFIX.length());
            LOG.debug("loading graph on classpath at {}", resourceName);
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
        } else {
            // look for graph in filesystem
            try {
                File graphFile = new File(graphFileName);
                is = new FileInputStream(graphFile);
            } catch (IOException ex) {
                is = null;
                LOG.warn("Error creating graph input stream", ex);
            }
        }
        if (is == null) {
            LOG.warn("Graph file not found or not openable for routerId '{}' under {}", routerId,
                    graphFileName);
            return null;
        }
        LOG.info("Loading graph...");
        Graph graph = null;
        try {
            graph = Graph.load(new ObjectInputStream(is), loadLevel, streetVertexIndexFactory);
        } catch (Exception ex) {
            LOG.error("Exception while loading graph from {}.", graphFileName);
            ex.printStackTrace();
            return null;
        }

        graph.routerId = (routerId);

        // Decorate the graph. Even if a config file is not present
        // one could be bundled inside.
        try {
            is = null;
            if (configFileName.startsWith(CLASSPATH_PREFIX)) {
                // look for config on classpath
                String resourceName = configFileName.substring(CLASSPATH_PREFIX.length());
                LOG.debug("Trying to load config on classpath at {}", resourceName);
                is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream(resourceName);
            } else {
                // look for config in filesystem
                LOG.debug("Trying to load config on file at {}", configFileName);
                File configFile = new File(configFileName);
                if (configFile.canRead()) {
                    LOG.info("Loading config from file {}", configFileName);
                    is = new FileInputStream(configFile);
                }
            }
            Preferences config = is == null ? null : new PropertiesPreferences(is);
            decorator.setupGraph(graph, config);
        } catch (IOException e) {
            LOG.error("Can't read config file", e);
        }
        return graph;
    }
}
