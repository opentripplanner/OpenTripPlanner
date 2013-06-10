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
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * An implementation of the file-based GraphServiceFileImpl which auto-configure itself by scanning
 * the root resource directory.
 */
@Scope("singleton")
public class GraphServiceAutoDiscoverImpl implements GraphService, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceAutoDiscoverImpl.class);

    private GraphServiceFileImpl decorated = new GraphServiceFileImpl();

    /** Last timestamp upper bound when we auto-scanned resources. */
    private long lastAutoScan = 0L;

    /** The autoscan period in seconds */
    @Setter
    private int autoScanPeriodSec = 60;

    /**
     * The delay before loading a new graph, in seconds. We load a graph if it has been modified at
     * least this amount of time in the past. This in order to give some time for non-atomic graph
     * copy.
     */
    @Setter
    private int loadDelaySec = 10;

    /**
     * @param indexFactory
     */
    public void setIndexFactory(StreetVertexIndexFactory indexFactory) {
        decorated.setIndexFactory(indexFactory);
    }

    /**
     * @param defaultRouterId
     */
    public void setDefaultRouterId(String defaultRouterId) {
        decorated.setDefaultRouterId(defaultRouterId);
    }

    /**
     * Sets a base path for graph loading from the filesystem. Serialized graph files will be
     * retrieved from sub-directories immediately below this directory. The routerId of a graph is
     * the same as the name of its sub-directory. This does the same thing as setResource, except
     * the parameter is interpreted as a file path.
     */
    public void setPath(String path) {
        decorated.setResource("file:" + path);
    }

    /**
     * Sets a base path in the classpath or relative to the webapp root. This can be useful in cloud
     * computing environments where webapps must be entirely self-contained. When OTP is running as
     * a webapp, the ResourceLoader provided by Spring will be a ServletContextResourceLoader, so
     * paths will be interpreted relative to the webapp root and WARs should be handled
     * transparently. If you want to point to a location outside the webapp or you just want to be
     * clear about exactly where the graphs are to be found, this path should be prefixed with
     * 'classpath:','file:', or 'url:'.
     */
    public void setResource(String resourceBaseName) {
        decorated.setResource(resourceBaseName);
    }

    @Override
    public Graph getGraph() {
        return decorated.getGraph();
    }

    @Override
    public Graph getGraph(String routerId) {
        return decorated.getGraph(routerId);
    }

    @Override
    public void setLoadLevel(LoadLevel level) {
        decorated.setLoadLevel(level);
    }

    // TODO Should we extract this interface in GraphService?
    // See the (strange) cast to GraphServiceImpl in Routers.reloadGraphs()
    public boolean reloadGraphs(boolean preEvict) {
        return decorated.reloadGraphs(preEvict);
    }

    @Override
    public Collection<String> getRouterIds() {
        return decorated.getRouterIds();
    }

    @Override
    public boolean registerGraph(String routerId, boolean preEvict) {
        // Invalid in auto-discovery mode
        return false;
    }

    @Override
    public boolean registerGraph(String routerId, Graph graph) {
        // Invalid in auto-discovery mode
        return false;
    }

    @Override
    public boolean evictGraph(String routerId) {
        // Invalid in auto-discovery mode
        return false;
    }

    @Override
    public int evictAll() {
        // Invalid in auto-discovery mode
        return 0;
    }

    /** The resourceLoader setter is called by Spring via ResourceLoaderAware interface. */
    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        decorated.setResourceLoader(resourceLoader);
    }

    /**
     * Based on the autoRegister list, automatically register all routerIds for which we can find a
     * graph file in a subdirectory of the resourceBase path. Also register and load the graph for
     * the defaultRouterId and warn if no routerIds are registered.
     */
    @PostConstruct
    // PostConstruct means run on startup after all injection has occurred
    private void startup() {
        /* Run the first one syncronously as other initialization methods may need a default router. */
        autoDiscoverGraphs();
        /*
         * Starting with JDK7 we should use a directory change listener callback on baseResource
         * instead.
         */
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                autoDiscoverGraphs();
            }
        }, autoScanPeriodSec * 1000L, autoScanPeriodSec * 1000L);
    }

    private synchronized void autoDiscoverGraphs() {
        LOG.debug("Auto discovering graphs under {}", decorated.getResourceBase());
        Collection<String> graphOnDisk = new HashSet<String>();
        Collection<String> graphToLoad = new HashSet<String>();
        // Only reload graph modified more than 1 mn ago.
        long validEndTime = System.currentTimeMillis() - loadDelaySec * 1000;
        Resource base = decorated.getResource(decorated.getResourceBase());
        try {
            File baseFile = base.getFile();
            // First check for a root graph
            File rootGraphFile = new File(baseFile, GraphServiceFileImpl.GRAPH_FILENAME);
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
                    File graphFile = new File(subFile, GraphServiceFileImpl.GRAPH_FILENAME);
                    if (graphFile.exists() && graphFile.canRead()) {
                        graphOnDisk.add(sub);
                        long lastModified = graphFile.lastModified();
                        if (lastModified > lastAutoScan && lastModified <= validEndTime) {
                            LOG.debug("Graph to (re)load: {}, lastModified={}", graphFile,
                                    lastModified);
                            graphToLoad.add(sub);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Can happen if base is not a standard directory (resource)
            LOG.error(
                    "Graph auto-discovering has been set, but {} is not a file resource, so I'm bailing-out.",
                    decorated.getResourceBase());
            // Just warn the user, no need to throw exception
        } finally {
            lastAutoScan = validEndTime;
        }

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
            decorated.registerGraph(routerId, false);
        }
        for (String routerId : getRouterIds()) {
            // Evict graph removed from disk.
            if (!graphOnDisk.contains(routerId)) {
                LOG.warn("Auto-evicting routerId '{}', not present on disk anymore.", routerId);
                decorated.evictGraph(routerId);
            }
        }

        /*
         * If the defaultRouterId is not present, print a warning and set it to some default.
         */
        if (!getRouterIds().contains(decorated.getDefaultRouterId())) {
            LOG.warn("Default routerId '{}' not available!", decorated.getDefaultRouterId());
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
                decorated.setDefaultRouterId(defRouterId);
            }
        }
        if (this.getRouterIds().isEmpty()) {
            LOG.warn("No graphs have been loaded/registered. "
                    + "You must place one or more graphs before routing.");
        }
    }
}
