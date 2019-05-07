package org.opentripplanner.routing.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.io.ByteStreams;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.standalone.config.GraphConfig;
import org.opentripplanner.standalone.config.OTPConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The primary implementation of the GraphSource interface. The graph is loaded from a serialized
 * graph from a given source.
 * 
 */
public class InputStreamGraphSource implements GraphSource {

    static final String GRAPH_FILENAME = "Graph.obj";

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

    private final Object preEvictMutex = Boolean.FALSE;

    private final GraphConfig config;


    /**
     * Create GraphSource loading graph from the file system using the graph config with graph
     * base path.
     */
    InputStreamGraphSource(String routerId, GraphConfig config) {
        this.routerId = routerId;
        this.config = config;
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
            long lastModified = getLastModified();
            boolean doReload = force || checkAutoReload(lastModified);
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
        try (InputStream is = getGraphInputStream()) {
            LOG.info("Loading graph...");
            try {
                newGraph = Graph.load(is);
            } catch (Exception ex) {
                LOG.error("Exception while loading graph '{}'.", routerId, ex);
                return null;
            }

            newGraph.routerId = (routerId);
        } catch (IOException e) {
            LOG.warn("Graph file not found or not openable for routerId '{}': {}", routerId, e);
            return null;
        }

        // Load configuration from disk or use the embedded configuration as fallback.
        JsonNode jsonConfig = config.routerConfig(newGraph.routerConfig);

        Router newRouter = new Router(routerId, newGraph);
        newRouter.startup(jsonConfig);
        return newRouter;
    }

    private InputStream getGraphInputStream() throws IOException {
        File graphFile = new File(config.getPath(), GRAPH_FILENAME);
        LOG.debug("Loading graph from file '{}'", graphFile.getPath());
        return new FileInputStream(graphFile);
    }

    private long getLastModified() {
        // Note: this returns 0L if the file does not exists
        return new File(config.getPath(), GRAPH_FILENAME).lastModified();
    }

    /**
     * A GraphSource factory creating InputStreamGraphSource from file.
     */
    public static class FileFactory implements GraphSource.Factory {

        private static final Logger LOG = LoggerFactory.getLogger(FileFactory.class);


        private final OTPConfiguration otpConfiguration;
        private final File basePath;

        public FileFactory(OTPConfiguration otpConfiguration, File basePath) {
            this.otpConfiguration = otpConfiguration;
            this.basePath = basePath;
        }

        @Override
        public GraphSource createGraphSource(String routerId) {
            return new InputStreamGraphSource(
                    routerId, otpConfiguration.getGraphConfig(getBasePath(routerId))
            );
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
