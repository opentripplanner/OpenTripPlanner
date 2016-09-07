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

package org.opentripplanner.routing.services;

import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A GraphService maps RouterIds to Graphs.
 * 
 * This graph service allows us to decouple the deserialization, loading, and management of the
 * underlying graph objects from the classes that need access to the objects. This indirection
 * allows us to provide multiple graphs distringuished by routerIds or to dynamically swap in new
 * graphs if underlying data changes.
 * 
 * Delegate the graph creation/loading details to the GraphSource implementations.
 * 
 * @see GraphSource
 */
public class GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphService.class);

    /** Poll period for auto-reload scan. */
    private static final int AUTORELOAD_PERIOD_SEC = 10;

    /**
     * Should we pre-evict in auto-reload mode? False is more memory consuming but safer in case of
     * problems.
     */
    private static final boolean AUTORELOAD_PREEVICT = false;

    private Map<String, GraphSource> graphSources = new HashMap<>();

    private static final Pattern routerIdPattern = Pattern.compile("[\\p{Alnum}_-]*");

    private String defaultRouterId = "";

    public GraphSource.Factory graphSourceFactory;

    private ScheduledExecutorService scanExecutor;

    public GraphService() {
        this(false);
    }

    public GraphService(boolean autoReload) {
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

    /** @param defaultRouterId The ID of the default router to return when no one is specified */
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

    /**
     * @return the current default router object
     */
    public Router getRouter() throws GraphNotFoundException {
        return getRouter(null);
    }

    /**
     * @return the graph object for the given router ID
     */
    public Router getRouter(String routerId) throws GraphNotFoundException {
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
        Router router = graphSource.getRouter();
        if (router == null) {
            evictRouter(routerId);
            throw new GraphNotFoundException();
        }
        return router;
    }

    /**
     * Reload all registered graphs from wherever they came from. See reloadGraph().
     * @return whether the operation completed successfully (all reloads are successful).
     */
    public boolean reloadGraphs(boolean preEvict, boolean force) {
        boolean allSucceeded = true;
        synchronized (graphSources) {
            Collection<String> routerIds = getRouterIds();
            for (String routerId : routerIds) {
                allSucceeded &= reloadGraph(routerId, preEvict, force);
            }
        }
        return allSucceeded;
    }

    /**
     * Reload a registered graph. If the reload fails, evict (remove) the graph.
     * 
     * @param routerId ID of the router
     * @param preEvict When true, release the existing graph (if any) before loading. This will
     *        halve the amount of memory needed for the operation, but routing will be unavailable
     *        for that graph during the load process
     * @param force When true, force a reload. If false, only check if the source has been modified,
     *        and reload if so.
     * @return True if the reload is successful, false otherwise.
     */
    public boolean reloadGraph(String routerId, boolean preEvict, boolean force) {
        synchronized (graphSources) {
            GraphSource graphSource = graphSources.get(routerId);
            if (graphSource == null) {
                return false;
            }
            boolean success = graphSource.reload(force, preEvict);
            if (!success) {
                evictRouter(routerId);
            }
            return success;
        }
    }

    /** @return a collection of all valid router IDs for this server */
    public Collection<String> getRouterIds() {
        return new ArrayList<String>(graphSources.keySet());
    }

    /**
     * Blocking method to associate the specified router ID with the corresponding graph source,
     * load it from appropriate source (serialized file, memory...), and enable its use in routing.
     * The relationship between router IDs and paths in the filesystem is determined by the
     * GraphSource implementation.
     * 
     * @return whether the operation completed successfully
     */
    public boolean registerGraph(String routerId, GraphSource graphSource) {
        LOG.info("Registering new router '{}'", routerId);
        if (!routerIdLegal(routerId)) {
            LOG.error(
                    "routerId '{}' contains characters other than alphanumeric, underscore, and dash.",
                    routerId);
            return false;
        }
        graphSource.reload(true, false);
        if (graphSource.getRouter() == null) {
            LOG.warn("Can't register router ID '{}', no graph.", routerId);
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

    /**
     * Dissociate a router ID from the corresponding graph/services object, and disable that router ID for
     * use in routing.
     * 
     * @return whether a router was associated with this router ID and was evicted.
     */
    public boolean evictRouter(String routerId) {
        LOG.info("Evicting router '{}'", routerId);
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

    /**
     * Dissocate all graphs from their router IDs and release references to the graphs to allow
     * garbage collection. Routing will not be possible until new graphs are registered.
     * 
     * This is equivalent to calling evictGraph on every registered router ID.
     */
    public int evictAll() {
        LOG.info("Evincting all graphs.");
        synchronized (graphSources) {
            int n = 0;
            Collection<String> routerIds = new ArrayList<String>(getRouterIds());
            for (String routerId : routerIds) {
                if (evictRouter(routerId)) {
                    n++;
                }
            }
            return n;
        }
    }

    /**
     * @return The default GraphSource factory. Needed in case someone want to register or save a
     *         new router with a router ID only (namely, via the web-service API).
     */
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
     * 
     * Router IDs may contain alphanumeric characters, underscores, and dashes only. This prevents
     * any confusion caused by the presence of special characters that might have a meaning for the
     * filesystem.
     */
    public static boolean routerIdLegal(String routerId) {
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
                    evictRouter(routerId);
                }
            }
        }
    }
}
