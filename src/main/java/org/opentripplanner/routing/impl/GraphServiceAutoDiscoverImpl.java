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
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.GraphSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of the file-based GraphServiceFileImpl which auto-configure itself by scanning
 * the root resource directory.
 * 
 * TODO Remove, and split existing functionalities in two:
 * 1) Graph auto-reload to FileGraphFactory,
 * 2) Graph auto-discovery to a "GraphScanner".
 */
public class GraphServiceAutoDiscoverImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceAutoDiscoverImpl.class);

    /** Last timestamp upper bound when we auto-scanned resources. */
    private long lastAutoScan = 0L;

    /** The autoscan period in seconds */
    private int autoScanPeriodSec = 60;

    private String basePath, defaultRouterId;

    private ScheduledExecutorService scanExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * The delay before loading a new graph, in seconds. We load a graph if it has been modified at
     * least this amount of time in the past. This in order to give some time for non-atomic graph
     * copy.
     */
    private int loadDelaySec = 10;

    @Override
    public Graph getGraph() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Graph getGraph(String routerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<String> getRouterIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean evictGraph(String routerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int evictAll() {
        throw new UnsupportedOperationException();
    }

    /**
     * Based on the autoRegister list, automatically register all routerIds for which we can find a
     * graph file in a subdirectory of the resourceBase path. Also register and load the graph for
     * the defaultRouterId and warn if no routerIds are registered.
     */
    public void startup() {
        /* Run the first one syncronously as other initialization methods may need a default router. */
        autoDiscoverGraphs();
        /*
         * Starting with JDK7 we should use a directory change listener callback on baseResource
         * instead.
         */
        scanExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                autoDiscoverGraphs();
            }
        }, autoScanPeriodSec, autoScanPeriodSec, TimeUnit.SECONDS);
    }

    /**
     * This is called when the bean gets deleted, that is mainly in case of webapp container
     * application stop or reload. We teardown all loaded graph to stop their background real-time
     * data updater thread, and also the background auto-discover scanner thread.
     */
    @PreDestroy
    private void teardown() {
        LOG.info("Cleaning-up auto-discover thread and graphs");
        //decorated.evictAll();
        scanExecutor.shutdown();
        try {
            boolean noTimeout = scanExecutor.awaitTermination(10, TimeUnit.SECONDS);
            if (!noTimeout)
                LOG.warn("Timeout while waiting for scanner thread to finish");
        } catch (InterruptedException e) {
            // This is not really important
            LOG.warn("Interrupted while waiting for scanner thread to finish", e);
        }
        //decorated.cleanupWebapp();
    }

    private synchronized void autoDiscoverGraphs() {
        LOG.debug("Auto discovering graphs under {}", basePath);
        Collection<String> graphOnDisk = new HashSet<String>();
        Collection<String> graphToLoad = new HashSet<String>();
        // Only reload graph modified more than 1 mn ago.
        long validEndTime = System.currentTimeMillis() - loadDelaySec * 1000;
        File baseFile = new File(basePath);
        // First check for a root graph
        File rootGraphFile = new File(baseFile, FileGraphSource.GRAPH_FILENAME);
        if (rootGraphFile.exists() && rootGraphFile.canRead()) {
            graphOnDisk.add("");
            // lastModified can change, so test must be atomic here.
            long lastModified = rootGraphFile.lastModified();
            if (lastModified > lastAutoScan && lastModified <= validEndTime) {
                LOG.debug("Graph to (re)load: {}, lastModified={}", rootGraphFile, lastModified);
                graphToLoad.add("");
            }
        }
        // Then graph in sub-directories
        for (String sub : baseFile.list()) {
            File subFile = new File(baseFile, sub);
            if (subFile.isDirectory()) {
                File graphFile = new File(subFile, FileGraphSource.GRAPH_FILENAME);
                if (graphFile.exists() && graphFile.canRead()) {
                    graphOnDisk.add(sub);
                    long lastModified = graphFile.lastModified();
                    if (lastModified > lastAutoScan && lastModified <= validEndTime) {
                        LOG.debug("Graph to (re)load: {}, lastModified={}", graphFile, lastModified);
                        graphToLoad.add(sub);
                    }
                }
            }
        }
        lastAutoScan = validEndTime;

        StringBuffer onDiskSb = new StringBuffer();
        for (String routerId : graphOnDisk)
            onDiskSb.append("[").append(routerId).append("]");
        StringBuffer toLoadSb = new StringBuffer();
        for (String routerId : graphToLoad)
            toLoadSb.append("[").append(routerId).append("]");
        LOG.debug("Found routers: {} - Must reload: {}", onDiskSb.toString(), toLoadSb.toString());
        for (String routerId : graphToLoad) {
            /*
             * Do not set preEvict, because: 1) during loading of a new graph we want to keep one
             * available; and 2) if the loading of a new graph fails we also want to keep the old
             * one.
             */
            this.registerGraph(routerId, new FileGraphSource(routerId, basePath, LoadLevel.FULL));
        }
        for (String routerId : getRouterIds()) {
            // Evict graph removed from disk.
            if (!graphOnDisk.contains(routerId)) {
                LOG.warn("Auto-evicting routerId '{}', not present on disk anymore.", routerId);
                this.evictGraph(routerId);
            }
        }

        /*
         * If the defaultRouterId is not present, print a warning and set it to some default.
         */
        if (!getRouterIds().contains(defaultRouterId)) {
            LOG.warn("Default routerId '{}' not available!", defaultRouterId);
            if (!getRouterIds().isEmpty()) {
                // Let's see which one we want to take by default
                String defRouterId = null;
                if (getRouterIds().contains("")) {
                    // If we have a root graph, this should be a good default
                    defRouterId = "";
                    LOG.info("Setting default routerId to root graph ''");
                } else {
                    // Otherwise take first one present
                    defRouterId = getRouterIds().iterator().next();
                    if (getRouterIds().size() > 1)
                        LOG.warn("Setting default routerId to arbitrary one '{}'", defRouterId);
                    else
                        LOG.info("Setting default routerId to '{}'", defRouterId);
                }
                defaultRouterId = (defRouterId);
            }
        }
        if (this.getRouterIds().isEmpty()) {
            LOG.warn("No graphs have been loaded/registered. "
                    + "You must place one or more graphs before routing.");
        }
    }

    @Override
    public boolean registerGraph(String routerId, GraphSource graphSource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDefaultRouterId(String defaultRouterId) {
        this.defaultRouterId = defaultRouterId;
    }

    @Override
    public boolean reloadGraphs(boolean preEvict) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GraphSourceFactory getGraphSourceFactory() {
        throw new UnsupportedOperationException();
    }
}
