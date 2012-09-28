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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The primary implementation of the GraphService interface.
 * It can handle multiple graphs, each with its own graphId. These graphs are loaded from 
 * serialized graph files in subdirectories immediately under the specified base resource/file path.
 */
@Scope("singleton")
public class GraphServiceImpl implements GraphService, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private String resourceBase = "/var/otp/graphs";

    private Map<String, Graph> graphs = new HashMap<String, Graph>();

    private Map<String, LoadLevel> levels = new HashMap<String, LoadLevel>();

    private LoadLevel loadLevel = LoadLevel.FULL;

    @Setter private String defaultRouterId = "";

    /** The resourceLoader setter is called by Spring via ResourceLoaderAware interface. */
    @Setter private ResourceLoader resourceLoader = null; 

    /** 
     * If true, search through immediate subdirectories of the resourceBase on startup and 
     * register any graphs found under the router IDs implied by the directory names.
     */
    @Setter private boolean scanGraphIds = false;

    /** If true, on startup register the graph in the location defaultRouterId. */
    @Setter private boolean attemptLoadDefault = true;

    /** 
     * Router IDs may contain alphanumeric characters, underscores, and dashes only. 
     * This prevents any confusion caused by the presence of special characters that might have a 
     * meaning for the filesystem.
     */
    private static Pattern graphIdPattern = Pattern.compile("[\\p{Alnum}_-]*");
    
    /** 
     * Sets a base path for graph loading from the filesystem. Serialized graph files will be 
     * retrieved from sub-directories immediately below this directory. The routerId of a graph is
     * the same as the names of its sub-directory. This does the same thing as setResource, except 
     * the parameter is interpreted as a file path.
     */
    public void setPath(String path) {
        this.resourceBase = "file:" + path;
    }

    /**
     * Sets a base path in the classpath or relative to the webapp root. This can be useful in 
     * cloud computing environments where webapps must be entirely self-contained. When OTP is
     * running as a webapp, the ResourceLoader provided by Spring will be a 
     * ServletContextResourceLoader, so paths will be interpreted relative to the webapp root and 
     * WARs should be handled transparently. If you want to point to a location outside the webapp 
     * or you just want to be clear about exactly where the graphs are to be found, this path 
     * should be prefixed with 'classpath:','file:', or 'url:'.
     */
    public void setResource(String resourceBaseName) {
        this.resourceBase = resourceBaseName;
    }

    @PostConstruct // PostConstruct means run on startup after all injection has occurred 
    private void autoRegisterGraphs() {
        LOG.info("routerId scanning : {}", scanGraphIds);
        if (scanGraphIds) {
            scanRegister();
        }
        if ( ! graphs.containsKey(defaultRouterId)) {
            LOG.info("Attempting to load graph for default routerId '{}'.", defaultRouterId);
            registerGraph(defaultRouterId, true);
        }
        if (this.getRouterIds().isEmpty()) {
            LOG.warn("No graphs have been registered or loaded. " +
                    "You must use the API to register one or more graphs before routing.");
        }
    }

    /** 
     * Examine all immediate subdirectories of the base path and register anything that looks like 
     * a graphId. Also register the empty string if a graph is present in the base directory.
     */
    private void scanRegister() {
        if (resourceLoader instanceof ResourcePatternResolver) {
            ResourcePatternResolver resolver = (ResourcePatternResolver) resourceLoader;
            LOG.info("Scanning for graphs to register under {}", resourceBase);
            String resourcePattern = resourceBase.concat("/*/Graph.obj");
            Resource[] resources;
            try {
                resources = resolver.getResources(resourcePattern);
            } catch (IOException ioe) {
                LOG.debug("IO exception while searching for graphs, aborting.");
                ioe.printStackTrace();
                return;
            }
            for (Resource resource : resources) {
                try {
                    String resourceName = resource.getURI().toString();
                    LOG.info("found graph at {}", resourceName);
                    InputStream is;
                        is = resource.getInputStream();
                    Graph graph = Graph.load(is, LoadLevel.FULL);
                    graphs.put(resourceName, graph);
                    levels.put(resourceName, LoadLevel.FULL);
                } catch (Exception ex) {
                    LOG.debug("Exception while loading graph, skipping.");
                    ex.printStackTrace();
                }
            }
        }
    }
    
    @Override
    public Graph getGraph() {
        return getGraph(null);
    }

    @Override
    public Graph getGraph(String routerId) {
        if (routerId == null || routerId.isEmpty()) {
            routerId = defaultRouterId;
            LOG.debug("graphId not specified, set to default of '{}'", routerId);
        }
        synchronized (graphs) {
            if ( ! graphs.containsKey(routerId)) {
                LOG.error("no graph registered with the graphId '{}'", routerId);
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

    private boolean routerIdLegal(String graphId) {
        Matcher m = graphIdPattern.matcher(graphId);
        return m.matches();
    }

    private Graph loadGraph(String routerId) {
        if ( ! routerIdLegal(routerId)) {
            LOG.error("graphId '{}' contains characters other than alphanumeric, underscore, and dash.", routerId);
            return null;
        }
        LOG.debug("loading serialized graph for graphId {}", routerId);
        String resourceLocation = String.format("%s/%s/Graph.obj", resourceBase, routerId);
        LOG.debug("graph file for routerId '{}' is at {}", routerId, resourceLocation);
        Resource graphResource;
        InputStream is;
        try {
            graphResource = resourceLoader.getResource(resourceLocation);
            //graphResource = resourceBase.createRelative(graphId);
            is = graphResource.getInputStream();
        } catch (IOException ex) {
            LOG.warn("Graph file not found or not openable for graphId '{}' under {}", routerId, resourceBase);
            ex.printStackTrace();
            return null;
        }
        LOG.debug("graph input stream successfully opened. now loading.");
        try {
            return Graph.load(is, loadLevel);
        } catch (Exception ex) {
            LOG.error("Exception while loading graph from {}.", graphResource);
            ex.printStackTrace();
            return null;
        }
    }

    public boolean reloadGraphs(boolean preEvict) {
        boolean allSucceeded = true;
        synchronized (graphs) {
            for (String graphId : this.getRouterIds()) {
                boolean success = registerGraph(graphId, preEvict);
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
    public boolean registerGraph(String graphId, boolean preEvict) {
        if (preEvict)
            evictGraph(graphId);
        Graph graph = this.loadGraph(graphId);
        if (graph != null) {
            synchronized (graphs) {
                graphs.put(graphId, graph);
            }
            levels.put(graphId, loadLevel);
            return true;
        }
        return false;
    }

    @Override
    public boolean registerGraph(String graphId, Graph graph) {
        Graph existing = graphs.put(graphId, graph);
        return existing == null;
    }
    
    @Override
    public boolean evictGraph(String graphId) {
        LOG.debug("evicting graph {}", graphId);
        synchronized (graphs) {
            Graph existing = graphs.remove(graphId);
            return existing != null;
        }
    }

    @Override
    public int evictAll() {
        int n;
        synchronized(graphs) {
            n = graphs.size(); 
            graphs.clear();
        }
        return n;
    }
}
