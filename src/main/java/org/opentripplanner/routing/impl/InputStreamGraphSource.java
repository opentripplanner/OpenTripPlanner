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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.io.ByteStreams;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * The primary implementation of the GraphSource interface. The graph is loaded from a serialized
 * graph from a given source.
 * 
 */
public class InputStreamGraphSource implements GraphSource {

    public static final String GRAPH_FILENAME = "Graph.obj";

    private static final Logger LOG = LoggerFactory.getLogger(InputStreamGraphSource.class);

    /**
     * Delay before starting to load a graph after the last modification time. In case of writing,
     * we expect graph last modification time to be updated at at least that frequency. If not, you
     * can either increase this value, or use an atomic move when copying the file.
     * */
    private static final long LOAD_DELAY_SEC = 10;

    private Router router;

    private String routerId;

    private long graphLastModified = 0L;

    private LoadLevel loadLevel;

    private Object preEvictMutex = new Boolean(false);

    /**
     * The current used input stream implementation for getting graph data source.
     */
    private Streams streams;

    // TODO Why do we need a factory? There is a single one implementation.
    private StreetVertexIndexFactory streetVertexIndexFactory = new DefaultStreetVertexIndexFactory();

    /**
     * @param routerId
     * @param path
     * @param loadLevel
     * @return A GraphSource loading graph from the file system under a base path.
     */
    public static InputStreamGraphSource newFileGraphSource(String routerId, File path,
            LoadLevel loadLevel) {
        return new InputStreamGraphSource(routerId, loadLevel, new FileStreams(path));
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
        return new InputStreamGraphSource(routerId, loadLevel, new ClasspathStreams(path));
    }

    private InputStreamGraphSource(String routerId, LoadLevel loadLevel,
            Streams streams) {
        this.routerId = routerId;
        this.loadLevel = loadLevel;
        this.streams = streams;
    }

    @Override
    public Router getRouter() {
        /*
         * We synchronize on pre-evict mutex in case we are in the middle of reloading in pre-evict
         * mode. In that case we must make the client wait until the new graph is loaded, because
         * the old one is gone to the GC. Performance hit should be low as getGraph() is not called
         * often.
         */
        synchronized (preEvictMutex) {
            return router;
        }
    }

    @Override
    public boolean reload(boolean force, boolean preEvict) {
        /* We synchronize on 'this' to prevent multiple reloads from being called at the same time */
        synchronized (this) {
            long lastModified = streams.getLastModified();
            boolean doReload = force ? true : checkAutoReload(lastModified);
            if (!doReload)
                return true;
            if (preEvict) {
                synchronized (preEvictMutex) {
                    if (router != null) {
                        LOG.info("Reloading '{}': pre-evicting router", routerId);
                        router.shutdown();
                    }
                    /*
                     * Forcing router to null here should remove any references to the graph once
                     * all current requests are done. So the next reload is supposed to have more
                     * memory.
                     */
                    router = null;
                    router = loadGraph();
                }
            } else {
                Router newRouter = loadGraph();
                if (newRouter != null) {
                    // Load OK
                    if (router != null) {
                        LOG.info("Reloading '{}': post-evicting router", routerId);
                        router.shutdown();
                    }
                    router = newRouter; // Assignment in java is atomic
                } else {
                    // Load failed
                    if (force || router == null) {
                        LOG.warn("Unable to load data for router '{}'.", routerId);
                        if (router != null) {
                            router.shutdown();
                        }
                        router = null;
                    } else {
                        // No shutdown, since we keep current one.
                        LOG.warn("Unable to load data for router '{}', keeping old data.", routerId);
                    }
                }
            }
            if (router == null) {
                graphLastModified = 0L;
            } else {
                /*
                 * Note: we flag even if loading failed, because we want to wait for fresh new data
                 * before loading again.
                 */
                graphLastModified = lastModified;
            }
            // If a router is null, it will be evicted.
            return (router != null);
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
        final Graph newGraph;
        try (InputStream is = streams.getGraphInputStream()) {
            LOG.info("Loading graph...");
            try {
                newGraph = Graph.load(new ObjectInputStream(is), loadLevel,
                        streetVertexIndexFactory);
            } catch (Exception ex) {
                LOG.error("Exception while loading graph '{}'.", routerId, ex);
                return null;
            }

            newGraph.routerId = (routerId);
        } catch (IOException e) {
            LOG.warn("Graph file not found or not openable for routerId '{}': {}", routerId, e);
            return null;
        }

        // Decorate the graph TODO how are we "decorating" it? This appears to refer to loading its configuration.
        // Even if a config file is not present on disk one could be bundled inside.
        try (InputStream is = streams.getConfigInputStream()) {
            JsonNode config = MissingNode.getInstance();
            if (is != null) {
                // TODO reuse the exact same JSON loader from OTPConfigurator
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
                mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
                config = mapper.readTree(is);
            }
            Router newRouter = new Router(routerId, newGraph);
            newRouter.startup(config);
            return newRouter;
        } catch (IOException e) {
            LOG.error("Can't read config file.");
            LOG.error(e.getMessage());
            return null;
        }
    }

    /**
     * InputStreamGraphSource delegates to some actual implementation the fact of getting the input
     * stream and checking the last modification timestamp for a given routerId.
     * FIXME this seems like a lot of boilerplate just to switch between FileInputStream and getResourceAsStream
     * a couple of conditional blocks and a boolean field "onClasspath" might do the trick.
     */
    private interface Streams {
        public abstract InputStream getGraphInputStream() throws IOException;

        public abstract InputStream getConfigInputStream() throws IOException;

        public abstract long getLastModified();
    }

    private static class FileStreams implements Streams {

        private File path;

        private FileStreams(File path) {
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
            File configFile = new File(path, Router.ROUTER_CONFIG_FILENAME);
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

    private static class ClasspathStreams implements Streams {

        private File path;

        private ClasspathStreams(File path) {
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
            File configFile = new File(path, Router.ROUTER_CONFIG_FILENAME);
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
     */
    public static class FileFactory implements GraphSource.Factory {

        private static final Logger LOG = LoggerFactory.getLogger(FileFactory.class);

        public File basePath;

        public LoadLevel loadLevel = LoadLevel.FULL;

        public FileFactory(File basePath) {
            this.basePath = basePath;
        }

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
                LOG.error("Exception while storing graph to {}.", sourceFile.getPath(), ex);
                return false;
            }

            return true;
        }

        private File getBasePath(String routerId) {
            return new File(basePath, routerId);
        }
    }
}
