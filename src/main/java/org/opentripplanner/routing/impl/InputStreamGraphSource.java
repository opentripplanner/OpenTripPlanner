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
import java.io.FileOutputStream;
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

import com.google.common.io.ByteStreams;

/**
 * The primary implementation of the GraphSource interface. The graph is loaded from a serialized
 * graph from a given source.
 * 
 */
public class InputStreamGraphSource implements GraphSource {

    public static final String GRAPH_FILENAME = "Graph.obj";

    public static final String CONFIG_FILENAME = "Graph.properties";

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamGraphSource.class);

    /**
     * Delay before starting to load a graph after the last modification time. In case of writing,
     * we expect graph last modification time to be updated at at least that frequency. If not, you
     * can either increase this value, or use an atomic move when copying the file.
     * */
    private static final long LOAD_DELAY_SEC = 10;

    private Graph graph;

    private String routerId;

    private long graphLastModified = 0L;

    private LoadLevel loadLevel;

    private Object preEvictMutex = new Boolean(false);

    /**
     * The current used input stream implementation for getting graph data source.
     */
    private GraphInputStream graphInputStream;

    // TODO Why do we need a factory? There is a single one implementation.
    private StreetVertexIndexFactory streetVertexIndexFactory = new DefaultStreetVertexIndexFactory();

    private GraphUpdaterConfigurator configurator = new GraphUpdaterConfigurator();

    /**
     * @param routerId
     * @param path
     * @param loadLevel
     * @return A GraphSource loading graph from the file system under a base path.
     */
    public static InputStreamGraphSource newFileGraphSource(String routerId, File path,
            LoadLevel loadLevel) {
        return new InputStreamGraphSource(routerId, loadLevel, new FileGraphInputStream(path));
    }

    /**
     * @param routerId
     * @param path
     * @param loadLevel
     * @return A GraphSource loading graph from an embedded classpath resources (a graph bundled
     *         inside a pre-packaged WAR for example).
     */
    public static InputStreamGraphSource newClasspathGraphSource(String routerId, File path,
            LoadLevel loadLevel) {
        return new InputStreamGraphSource(routerId, loadLevel, new ClasspathGraphInputStream(path));
    }

    private InputStreamGraphSource(String routerId, LoadLevel loadLevel,
            GraphInputStream graphInputStream) {
        this.routerId = routerId;
        this.loadLevel = loadLevel;
        this.graphInputStream = graphInputStream;
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
            long lastModified = graphInputStream.getLastModified();
            boolean doReload = force ? true : checkAutoReload(lastModified);
            if (!doReload)
                return true;
            if (preEvict) {
                synchronized (preEvictMutex) {
                    if (graph != null)
                        configurator.shutdownGraph(graph);
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
                        configurator.shutdownGraph(graph);
                    graph = newGraph; // Assignment in java is atomic
                } else {
                    // Load failed
                    if (force || graph == null) {
                        LOG.warn("Unable to load data for router '{}'.", routerId);
                        if (graph != null)
                            configurator.shutdownGraph(graph);
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

    /**
     * Check if a graph has been modified since the last time it has been loaded.
     * 
     * @param lastModified Time of last modification of current loaded data.
     * @return True if the input data has been modified and need to be reloaded.
     */
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
                configurator.shutdownGraph(graph);
            }
        }
    }

    /**
     * Do the actual operation of graph loading. Load configuration if present, and configure the
     * graph with dynamic updaters.
     * 
     * @return
     */
    private Graph loadGraph() {
        final Graph graph;
        try (InputStream is = graphInputStream.getGraphInputStream()) {
            LOG.info("Loading graph...");
            try {
                graph = Graph.load(new ObjectInputStream(is), loadLevel, streetVertexIndexFactory);
            } catch (Exception ex) {
                LOG.error("Exception while loading graph '{}'.", routerId);
                ex.printStackTrace();
                return null;
            }

            graph.routerId = (routerId);
        } catch (IOException e) {
            LOG.warn("Graph file not found or not openable for routerId '{}': {}", routerId, e);
            return null;
        }

        // Decorate the graph. Even if a config file is not present
        // one could be bundled inside.
        try (InputStream is = graphInputStream.getConfigInputStream()) {
            Preferences config = is == null ? null : new PropertiesPreferences(is);
            configurator.setupGraph(graph, config);
        } catch (IOException e) {
            LOG.error("Can't read config file", e);
        }
        return graph;
    }

    /**
     * InputStreamGraphSource delegates to some actual implementation the fact of getting the input
     * stream and checking the last modification timestamp for a given routerId.
     */
    private interface GraphInputStream {
        public abstract InputStream getGraphInputStream() throws IOException;

        public abstract InputStream getConfigInputStream() throws IOException;

        public abstract long getLastModified();
    }

    private static class FileGraphInputStream implements GraphInputStream {

        private File path;

        private FileGraphInputStream(File path) {
            this.path = path;
        }

        @Override
        public InputStream getGraphInputStream() throws IOException {
            File graphFile = new File(path, GRAPH_FILENAME);
            LOG.debug("Loading graph from file '{}'", graphFile.getPath());
            return new FileInputStream(graphFile);
        }

        @Override
        public InputStream getConfigInputStream() throws IOException {
            File configFile = new File(path, CONFIG_FILENAME);
            if (configFile.canRead()) {
                LOG.debug("Loading config from file '{}'", configFile.getPath());
                return new FileInputStream(configFile);
            } else {
                return null;
            }
        }

        @Override
        public long getLastModified() {
            // Note: this returns 0L if the file does not exists
            return new File(path, GRAPH_FILENAME).lastModified();
        }
    }

    private static class ClasspathGraphInputStream implements GraphInputStream {

        private File path;

        private ClasspathGraphInputStream(File path) {
            this.path = path;
        }

        @Override
        public InputStream getGraphInputStream() {
            File graphFile = new File(path, GRAPH_FILENAME);
            LOG.debug("Loading graph from classpath at '{}'", graphFile.getPath());
            return Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(graphFile.getPath());
        }

        @Override
        public InputStream getConfigInputStream() {
            File configFile = new File(path, CONFIG_FILENAME);
            LOG.debug("Trying to load config on classpath at '{}'", configFile.getPath());
            return Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(configFile.getPath());
        }

        /**
         * For a packaged classpath resources we assume the data won't change, so returning always
         * 0L basically disable auto-reload in that case.
         */
        @Override
        public long getLastModified() {
            return 0L;
        }
    }

    /**
     * A GraphSource factory creating InputStreamGraphSource from file.
     * 
     * @see FileGraphSource
     */
    public static class FileFactory implements GraphSource.Factory {

        private static final Logger LOG = LoggerFactory.getLogger(FileFactory.class);

        public File basePath = new File("/var/otp/graphs");

        public LoadLevel loadLevel = LoadLevel.FULL;

        @Override
        public GraphSource createGraphSource(String routerId) {
            return InputStreamGraphSource.newFileGraphSource(routerId, getBasePath(routerId),
                    loadLevel);
        }

        @Override
        public boolean save(String routerId, InputStream is) {

            File sourceFile = new File(getBasePath(routerId), InputStreamGraphSource.GRAPH_FILENAME);

            try {

                // Create directory if necessary
                File directory = new File(sourceFile.getParentFile().getPath());
                if (!directory.exists()) {
                    directory.mkdir();
                }

                // Store the stream to disk, to be sure no data will be lost make a temporary backup
                // file of the original file.

                // Make backup file
                File destFile = null;
                if (sourceFile.exists()) {
                    destFile = new File(sourceFile.getPath() + ".bak");
                    if (destFile.exists()) {
                        destFile.delete();
                    }
                    sourceFile.renameTo(destFile);
                }

                // Store the stream
                try (FileOutputStream os = new FileOutputStream(sourceFile)) {
                    ByteStreams.copy(is, os);
                }

                // And delete the backup file
                sourceFile = new File(sourceFile.getPath() + ".bak");
                if (sourceFile.exists()) {
                    sourceFile.delete();
                }

            } catch (Exception ex) {
                LOG.error("Exception while storing graph to {}.", sourceFile.getPath());
                ex.printStackTrace();
                return false;
            }

            return true;
        }

        private File getBasePath(String routerId) {
            return new File(basePath, routerId);
        }
    }
}
