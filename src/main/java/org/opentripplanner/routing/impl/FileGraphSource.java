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
 */
public class FileGraphSource implements GraphSource {

    private static final Logger LOG = LoggerFactory.getLogger(FileGraphSource.class);

    public static final String GRAPH_FILENAME = "Graph.obj";

    public static final String CONFIG_FILENAME = "Graph.properties";

    private static final long LOAD_DELAY_SEC = 10;

    private Graph graph;

    private String routerId;

    protected File path;

    private long graphLastModified = 0L;

    private LoadLevel loadLevel;

    private Object preEvictMutex = new Boolean(false);

    // TODO Why do we need a factory? There is a single one implementation.
    private StreetVertexIndexFactory streetVertexIndexFactory = new DefaultStreetVertexIndexFactory();

    private GraphUpdaterConfigurator decorator = new GraphUpdaterConfigurator();

    public FileGraphSource(String routerId, File path, LoadLevel loadLevel) {
        this.routerId = routerId;
        this.path = path;
        this.loadLevel = loadLevel;
        this.reload(true, false);
    }

    @Override
    public Graph getGraph() {
        /*
         * We synchronize on pre-evict mutex in case we are in the middle of reloading in pre-evict
         * mode. In that case we must make the client wait until the new graph is loaded, because
         * the old one is gone to the GC. Performance hit should be low as getGraph() is not called
         * often.
         */
        synchronized (preEvictMutex) {
            return graph;
        }
    }

    @Override
    public boolean reload(boolean force, boolean preEvict) {
        /* We synchronize on 'this' to prevent multiple reloads from being called at the same time */
        synchronized (this) {
            long lastModified = getLastModified();
            boolean doReload = force ? true : checkAutoReload(lastModified);
            if (!doReload)
                return true;
            if (preEvict) {
                synchronized (preEvictMutex) {
                    if (graph != null)
                        decorator.shutdownGraph(graph);
                    /*
                     * Forcing graph to null here should remove any references to the graph once all
                     * current requests are done. So the next reload is supposed to have more
                     * memory.
                     */
                    graph = null;
                    graph = loadGraph();
                }
            } else {
                Graph newGraph = loadGraph();
                if (newGraph != null) {
                    // Load OK
                    if (graph != null)
                        decorator.shutdownGraph(graph);
                    graph = newGraph; // Assignment in java is atomic
                } else {
                    // Load failed
                    if (force || graph == null) {
                        LOG.warn("Unable to load data for router '{}'.", routerId);
                        if (graph != null)
                            decorator.shutdownGraph(graph);
                        graph = null;
                    } else {
                        // No shutdown, since we keep current one.
                        LOG.warn("Unable to load data for router '{}', keeping old data.", routerId);
                    }
                }
            }
            if (graph == null) {
                graphLastModified = 0L;
            } else {
                /*
                 * Note: we flag even if loading failed, because we want to wait for fresh new data
                 * before loading again.
                 */
                graphLastModified = lastModified;
            }
            // If a graph is null, it will be evicted.
            return (graph != null);
        }
    }

    private boolean checkAutoReload(long lastModified) {
        // We check only for graph file modification, not config
        long validEndTime = System.currentTimeMillis() - LOAD_DELAY_SEC * 1000;
        LOG.debug(
                "checkAutoReload router '{}' validEndTime={} lastModified={} graphLastModified={}",
                routerId, validEndTime, lastModified, graphLastModified);
        if (lastModified != graphLastModified && lastModified <= validEndTime) {
            // Only reload graph modified more than 1 mn ago.
            LOG.info("Router ID '{}' graph input modification detected, force reload.", routerId);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void evict() {
        synchronized (this) {
            if (graph != null) {
                decorator.shutdownGraph(graph);
            }
        }
    }

    private Graph loadGraph() {

        InputStream is = getGraphInputStream();
        if (is == null) {
            LOG.warn("Graph file not found or not openable for routerId '{}'", routerId);
            return null;
        }
        LOG.info("Loading graph...");
        Graph graph = null;
        try {
            graph = Graph.load(new ObjectInputStream(is), loadLevel, streetVertexIndexFactory);
        } catch (Exception ex) {
            LOG.error("Exception while loading graph '{}'.", routerId);
            ex.printStackTrace();
            return null;
        }

        graph.routerId = (routerId);

        // Decorate the graph. Even if a config file is not present
        // one could be bundled inside.
        try {
            is = getConfigInputStream();
            Preferences config = is == null ? null : new PropertiesPreferences(is);
            decorator.setupGraph(graph, config);
        } catch (IOException e) {
            LOG.error("Can't read config file", e);
        }
        return graph;
    }

    protected InputStream getGraphInputStream() {
        try {
            File graphFile = new File(path, GRAPH_FILENAME);
            LOG.info("Loading graph from file '{}'", graphFile.getPath());
            return new FileInputStream(graphFile);
        } catch (IOException ex) {
            LOG.warn("Error creating graph input stream", ex);
            return null;
        }
    }

    protected InputStream getConfigInputStream() throws IOException {
        File configFile = new File(path, CONFIG_FILENAME);
        if (configFile.canRead()) {
            LOG.info("Loading config from file '{}'", configFile.getPath());
            return new FileInputStream(configFile);
        } else {
            return null;
        }
    }

    protected long getLastModified() {
        // Note: this returns 0L if the file does not exists
        return new File(path, GRAPH_FILENAME).lastModified();
    }
}
