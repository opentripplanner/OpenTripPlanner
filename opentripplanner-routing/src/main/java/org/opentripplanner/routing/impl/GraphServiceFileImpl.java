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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * A GraphService implementation implementing loading graph from files or resources, but which does
 * not load anything by itself. It rely on decorators instances to help it initialize/reload itself.
 * 
 * Note: Instead of a decorator pattern we should have used a strategy pattern (letting the
 * GraphServiceImpl delegate the file mapping to various strategy/factory instances), but this would
 * have broke down the spring API widely used. And also this could have been a bit more complex as
 * auto-discover mode would have need callbacks.
 * 
 * @see GraphServiceImpl
 * @see GraphServiceAutoDiscoverImpl
 */
public class GraphServiceFileImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceFileImpl.class);

    public static final String GRAPH_FILENAME = "Graph.obj";

    @Getter
    private String resourceBase = "file:/var/otp/graphs";

    private Map<String, Graph> graphs = new HashMap<String, Graph>();

    private Map<String, LoadLevel> levels = new HashMap<String, LoadLevel>();

    private LoadLevel loadLevel = LoadLevel.FULL;

    @Setter
    private StreetVertexIndexFactory indexFactory = new DefaultStreetVertexIndexFactory();

    @Setter
    @Getter
    private String defaultRouterId = "";

    /** The resourceLoader setter is called by decorators. */
    @Setter
    private ResourceLoader resourceLoader = null;

    /**
     * Router IDs may contain alphanumeric characters, underscores, and dashes only. This prevents
     * any confusion caused by the presence of special characters that might have a meaning for the
     * filesystem.
     */
    private static Pattern routerIdPattern = Pattern.compile("[\\p{Alnum}_-]*");

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
        this.resourceBase = resourceBaseName;
    }

    public Graph getGraph() {
        return getGraph(null);
    }

    public Graph getGraph(String routerId) {
        if (routerId == null || routerId.isEmpty()) {
            routerId = defaultRouterId;
            LOG.debug("routerId not specified, set to default of '{}'", routerId);
        }
        synchronized (graphs) {
            if (!graphs.containsKey(routerId)) {
                LOG.error("no graph registered with the routerId '{}'", routerId);
                return null;
            } else {
                return graphs.get(routerId);
            }
        }
    }

    @Override
    public void setLoadLevel(LoadLevel level) {
        if (level != loadLevel) {
            loadLevel = level;
            reloadGraphs(true);
        }
    }

    private boolean routerIdLegal(String routerId) {
        Matcher m = routerIdPattern.matcher(routerId);
        return m.matches();
    }

    protected Graph loadGraph(String routerId) {
        if (!routerIdLegal(routerId)) {
            LOG.error(
                "routerId '{}' contains characters other than alphanumeric, underscore, and dash.",
                routerId);
            return null;
        }
        LOG.debug("loading serialized graph for routerId {}", routerId);
        StringBuilder sb = new StringBuilder(resourceBase);
        // S3 is intolerant of extra slashes in the URL, so only add them as needed
        if (!(resourceBase.endsWith("/") || resourceBase.endsWith(File.pathSeparator))) {
            sb.append("/");
        }
        if (routerId.length() > 0) {
            sb.append(routerId);
            sb.append("/");
        }
        sb.append("Graph.obj");
        String resourceLocation = sb.toString();
        LOG.debug("graph file for routerId '{}' is at {}", routerId, resourceLocation);
        Resource graphResource;
        InputStream is;
        try {
            graphResource = resourceLoader.getResource(resourceLocation);
            // graphResource = resourceBase.createRelative(graphId);
            is = graphResource.getInputStream();
        } catch (IOException ex) {
            LOG.warn("Graph file not found or not openable for routerId '{}' under {}", routerId,
                    resourceBase);
            ex.printStackTrace();
            return null;
        }
        LOG.debug("graph input stream successfully opened.");
        LOG.info("Loading graph...");
        try {
            return Graph.load(new ObjectInputStream(is), loadLevel, indexFactory);
        } catch (Exception ex) {
            LOG.error("Exception while loading graph from {}.", graphResource);
            ex.printStackTrace();
            return null;
        }
    }

    public boolean reloadGraphs(boolean preEvict) {
        boolean allSucceeded = true;
        synchronized (graphs) {
            for (String routerId : this.getRouterIds()) {
                boolean success = registerGraph(routerId, preEvict);
                allSucceeded &= success;
            }
        }
        return allSucceeded;
    }

    @Override
    public Collection<String> getRouterIds() {
        return new ArrayList<String>(graphs.keySet());
    }

    @Override
    public boolean registerGraph(String routerId, boolean preEvict) {
        if (preEvict)
            evictGraph(routerId);
        LOG.info("registering routerId '{}'", routerId);
        Graph graph = this.loadGraph(routerId);
        if (graph != null) {
            synchronized (graphs) {
                graphs.put(routerId, graph);
            }
            levels.put(routerId, loadLevel);
            return true;
        }
        LOG.info("routerId '{}' was not registered (graph was null).", routerId);
        return false;
    }

    @Override
    public boolean registerGraph(String routerId, Graph graph) {
        Graph existing = graphs.put(routerId, graph);
        return existing == null;
    }

    @Override
    public boolean evictGraph(String routerId) {
        LOG.debug("evicting graph {}", routerId);
        synchronized (graphs) {
            Graph existing = graphs.remove(routerId);
            return existing != null;
        }
    }

    @Override
    public int evictAll() {
        int n;
        synchronized (graphs) {
            n = graphs.size();
            graphs.clear();
        }
        return n;
    }

    public Resource getResource(String location) {
        return resourceLoader.getResource(location);
    }

}
