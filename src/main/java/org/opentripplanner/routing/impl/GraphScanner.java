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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scan for graphs under the base directory and auto-register them.
 */
public class GraphScanner {

    private static final Logger LOG = LoggerFactory.getLogger(GraphScanner.class);

    /** Auto-scan for new graphs every n secs. */
    private static final int AUTOSCAN_PERIOD_SEC = 10;

    /** Where to look for graphs. Defaults to 'graphs' under the OTP server base path. */
    public File basePath = null;

    /** A list of routerIds to automatically register and load at startup */
    public List<String> autoRegister;

    /** The default router, none by default */
    public String defaultRouterId = null;

    /** Load level */
    public LoadLevel loadLevel = LoadLevel.FULL;

    /** The GraphService where register graphs to */
    private GraphService graphService;

    private ScheduledExecutorService scanExecutor;

    public GraphScanner(GraphService graphService, File basePath, boolean autoScan) {
        this.graphService = graphService;
        this.basePath = basePath;
        if (autoScan) {
            scanExecutor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    /**
     * Based on the autoRegister list, automatically register all routerIds for which we can find a
     * graph file in a subdirectory of the resourceBase path. Also register and load the graph for
     * the defaultRouterId and warn if no routerIds are registered.
     */
    public void startup() {
        Set<String> routerIds = new HashSet<String>();
        if (autoRegister != null)
            routerIds.addAll(autoRegister);
        if (defaultRouterId != null) {
            graphService.setDefaultRouterId(defaultRouterId);
            routerIds.add(defaultRouterId);
        }
        if (!routerIds.isEmpty()) {
            LOG.info("Attempting to automatically register routerIds {}", autoRegister);
            LOG.info("Graph files will be sought in paths relative to {}", basePath);
            for (String routerId : routerIds) {
                InputStreamGraphSource graphSource = InputStreamGraphSource.newFileGraphSource(
                        routerId, getBasePath(routerId), loadLevel);
                graphService.registerGraph(routerId, graphSource);
            }
        } else {
            LOG.info("No list of routerIds was provided for automatic registration.");
        }
        if (scanExecutor != null) {
            LOG.info("Auto-scan mode activated, looking in {}", basePath);
            autoScan();
            scanExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    autoScan();
                }
            }, AUTOSCAN_PERIOD_SEC, AUTOSCAN_PERIOD_SEC, TimeUnit.SECONDS);
        }
    }

    private void autoScan() {
        LOG.debug("Auto discovering graphs under {}", basePath);
        /*
         * There is no need to synchronize scan and registration here. If a graph file is removed
         * between scan and register, registering will fail but it's safe. It a graph file is
         * created, we'll wait for the next scan to register it.
         */
        Set<String> graphOnDisk = new HashSet<String>();
        /* First check for a root graph */
        File rootGraphFile = new File(basePath, InputStreamGraphSource.GRAPH_FILENAME);
        if (rootGraphFile.exists() && rootGraphFile.canRead()) {
            graphOnDisk.add("");
        }
        /* Then graph in sub-directories */
        for (String sub : basePath.list()) {
            File subPath = new File(basePath, sub);
            if (subPath.isDirectory()) {
                File graphFile = new File(subPath, InputStreamGraphSource.GRAPH_FILENAME);
                if (graphFile.exists() && graphFile.canRead()) {
                    graphOnDisk.add(sub);
                }
            }
        }
        Set<String> graphRegistered = new HashSet<>(graphService.getRouterIds());
        Set<String> graphToRegister = new HashSet<>(graphOnDisk);
        graphToRegister.removeAll(graphRegistered);

        if (!graphToRegister.isEmpty()) {
            LOG.info("Found new routers to register: {}",
                    Arrays.toString(graphToRegister.toArray()));
            for (String routerId : graphToRegister) {
                InputStreamGraphSource graphSource = InputStreamGraphSource.newFileGraphSource(
                        routerId, getBasePath(routerId), loadLevel);
                // Can be null here if the file has been removed in the meantime.
                graphService.registerGraph(routerId, graphSource);
            }
        }
        /*
         * Note: We do not automatically evict removed graph. They will be evicted only in
         * auto-reload mode, and that's the behavior we want.
         */
        Collection<String> routerIds = graphService.getRouterIds();
        if (routerIds.isEmpty()) {
            LOG.warn("No graphs have been loaded/registered. "
                    + "You must place one or more graphs before routing.");
        } else {
            try {
                // Check if we still have a default graph.
                graphService.getRouter();
            } catch (GraphNotFoundException e) {
                // Let's see which one we want to take by default
                if (routerIds.contains("")) {
                    // If we have a root graph, this should be a good default
                    LOG.info("Setting default routerId to root graph ''");
                    graphService.setDefaultRouterId("");
                } else {
                    // Otherwise take first one present
                    String defRouterId = routerIds.iterator().next();
                    if (routerIds.size() > 1)
                        LOG.warn("Setting default routerId to arbitrary one '{}'", defRouterId);
                    else
                        LOG.info("Setting default routerId to '{}'", defRouterId);
                    graphService.setDefaultRouterId(defRouterId);
                }
            }
        }
    }

    private File getBasePath(String routerId) {
        return new File(basePath, routerId);
    }
}
