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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.GraphSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The primary implementation of the GraphService interface. It can handle multiple graphs, each
 * with its own routerId.
 * 
 * Delegate the graph creation/loading details to the GraphSource implementations.
 * 
 * @see GraphSource
 */
public class GraphServiceImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    /** Poll period for auto-reload scan. */
    private static final int AUTORELOAD_PERIOD_SEC = 10;

    /**
     * Should we pre-evict in auto-reload mode? False is more memory consuming but safer in case of
     * problems.
     */
    private static final boolean AUTORELOAD_PREEVICT = false;

    private Map<String, GraphSource> graphSources = new HashMap<>();

    /**
     * Router IDs may contain alphanumeric characters, underscores, and dashes only. This prevents
     * any confusion caused by the presence of special characters that might have a meaning for the
     * filesystem.
     */
    public static final Pattern routerIdPattern = Pattern.compile("[\\p{Alnum}_-]*");

    private String defaultRouterId = "";

    public GraphSource.Factory graphSourceFactory;

    private ScheduledExecutorService scanExecutor;

    public GraphServiceImpl() {
        this(false);
    }

    public GraphServiceImpl(boolean autoReload) {
        if (autoReload) {
            scanExecutor = Executors.newSingleThreadScheduledExecutor();
            scanExecutor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    autoReloadScan();
                }
            }, AUTORELOAD_PERIOD_SEC, AUTORELOAD_PERIOD_SEC, TimeUnit.SECONDS);
        }
    }

    /**
     * @param defaultRouterId
     */
    @Override
    public void setDefaultRouterId(String defaultRouterId) {
        this.defaultRouterId = defaultRouterId;
    }

    /**
     * This is called when the bean gets deleted, that is mainly in case of webapp container
     * application stop or reload. We teardown all loaded graph to stop their background real-time
     * data updater thread.
     */
    @PreDestroy
    private void teardown() {
        LOG.info("Cleaning-up graphs...");
        evictAll();
        cleanupWebapp();
    }

    @Override
    public Graph getGraph() throws GraphNotFoundException {
        return getGraph(null);
    }

    @Override
    public Graph getGraph(String routerId) throws GraphNotFoundException {
        if (routerId == null || routerId.isEmpty() || routerId.equalsIgnoreCase("default")) {
            routerId = defaultRouterId;
            LOG.debug("routerId not specified, set to default of '{}'", routerId);
        }
        /*
         * Here we should not synchronize on graphSource as it may block for a while (during
         * reload/autoreload). For normal operations a simple get do not need to be synchronized so
         * we should be safe.
         */
        GraphSource graphSource = graphSources.get(routerId);
        if (graphSource == null) {
            LOG.error("no graph registered with the routerId '{}'", routerId);
            throw new GraphNotFoundException();
        }
        Graph graph = graphSource.getGraph();
        if (graph == null) {
            evictGraph(routerId);
            throw new GraphNotFoundException();
        }
        return graph;
    }

    @Override
    public boolean reloadGraphs(boolean preEvict) {
        boolean allSucceeded = true;
        synchronized (graphSources) {
            Collection<String> routerIds = getRouterIds();
            for (String routerId : routerIds) {
                GraphSource graphSource = graphSources.get(routerId);
                boolean success = graphSource.reload(true, preEvict);
                if (!success) {
                    evictGraph(routerId);
                }
                allSucceeded &= success;
            }
        }
        return allSucceeded;
    }

    @Override
    public Collection<String> getRouterIds() {
        return new ArrayList<String>(graphSources.keySet());
    }

    @Override
    public boolean registerGraph(String routerId, GraphSource graphSource) {
        LOG.info("Registering new graph '{}'", routerId);
        if (!routerIdLegal(routerId)) {
            LOG.error(
                    "routerId '{}' contains characters other than alphanumeric, underscore, and dash.",
                    routerId);
            return false;
        }
        if (graphSource.getGraph() == null) {
            LOG.warn("Can't register router ID '{}', null graph.", routerId);
            return false;
        }
        synchronized (graphSources) {
            GraphSource oldSource = graphSources.get(routerId);
            if (oldSource != null) {
                LOG.info("Graph '{}' already registered. Nothing to do.", routerId);
                return false;
            }
            graphSources.put(routerId, graphSource);
            return true;
        }
    }

    @Override
    public boolean evictGraph(String routerId) {
        LOG.info("Evicting graph '{}'", routerId);
        synchronized (graphSources) {
            GraphSource graphSource = graphSources.get(routerId);
            graphSources.remove(routerId);
            if (graphSource != null) {
                graphSource.evict();
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public int evictAll() {
        LOG.info("Evincting all graphs.");
        synchronized (graphSources) {
            int n = 0;
            Collection<String> routerIds = new ArrayList<String>(getRouterIds());
            for (String routerId : routerIds) {
                if (evictGraph(routerId)) {
                    n++;
                }
            }
            return n;
        }
    }

    @Override
    public GraphSource.Factory getGraphSourceFactory() {
        return graphSourceFactory;
    }

    /**
     * Hook to cleanup various stuff of some used libraries (org.geotools), which depend on the
     * external client to call them for cleaning-up.
     */
    private void cleanupWebapp() {
        LOG.info("Web application shutdown: cleaning various stuff");
        WeakCollectionCleaner.DEFAULT.exit();
        DeferredAuthorityFactory.exit();
    }

    /**
     * Check whether a router ID is legal or not.
     */
    private boolean routerIdLegal(String routerId) {
        Matcher m = routerIdPattern.matcher(routerId);
        return m.matches();
    }

    private void autoReloadScan() {
        synchronized (graphSources) {
            Collection<String> routerIds = getRouterIds();
            for (String routerId : routerIds) {
                GraphSource graphSource = graphSources.get(routerId);
                boolean success = graphSource.reload(false, AUTORELOAD_PREEVICT);
                if (!success) {
                    evictGraph(routerId);
                }
            }
        }
    }
}
