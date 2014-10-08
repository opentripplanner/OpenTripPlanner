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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.GraphSource;
import org.opentripplanner.routing.services.GraphSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The primary implementation of the GraphService interface. It can handle multiple graphs, each
 * with its own routerId.
 * 
 * Delegate the graph creation/loading details to the GraphFactory implementation.
 * 
 * @see GraphFactory
 */
public class GraphServiceImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private Map<String, GraphSource> graphSources = new HashMap<>();

    /**
     * Router IDs may contain alphanumeric characters, underscores, and dashes only. This prevents
     * any confusion caused by the presence of special characters that might have a meaning for the
     * filesystem.
     */
    public static final Pattern routerIdPattern = Pattern.compile("[\\p{Alnum}_-]*");

    private String defaultRouterId = "";

    public GraphSourceFactory graphSourceFactory;

    public GraphServiceImpl() {
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
        synchronized (graphSources) {
            if (!graphSources.containsKey(routerId)) {
                LOG.error("no graph registered with the routerId '{}'", routerId);
                throw new GraphNotFoundException();
            } else {
                return graphSources.get(routerId).getGraph();
            }
        }
    }

    @Override
    public boolean reloadGraphs(boolean preEvict) {
        boolean allSucceeded = true;
        synchronized (graphSources) {
            for (GraphSource graphSource : graphSources.values()) {
                boolean success = graphSource.reload(preEvict);
                allSucceeded &= success;
            }
        }
        return allSucceeded;
    }

    @Override
    public Collection<String> getRouterIds() {
        return Collections.unmodifiableCollection(graphSources.keySet());
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
        synchronized (graphSources) {
            GraphSource oldSource = graphSources.get(routerId);
            if (oldSource != null) {
                LOG.info("Graph '{}' already registered. Nothing to do.");
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
    public GraphSourceFactory getGraphSourceFactory() {
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
}
