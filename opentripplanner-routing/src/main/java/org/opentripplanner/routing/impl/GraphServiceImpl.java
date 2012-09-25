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

import java.util.Collection;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The primary implementation of the GraphService interface.
 * It can handle multiple graphs, each with its own graphId. These graphs are loaded from 
 * serialized graph files in subdirectories of the specified base resource/file path.
 */
@Scope("singleton")
public class GraphServiceImpl implements GraphService, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private String resourceBase = "/var/otp/graphs";

    private Map<String, Graph> graphs = new HashMap<String, Graph>();

    private Map<String, LoadLevel> levels = new HashMap<String, LoadLevel>();

    private LoadLevel loadLevel = LoadLevel.FULL;

    @Setter private String defaultRouterId = "";

    // this is set by Spring via ResourceLoaderAware interface
    @Setter private ResourceLoader resourceLoader = null; 

    @Setter private boolean scanGraphIds = false;

    @Setter private boolean attemptLoadDefault = true;

    /* graphIds may contain alphanumeric characters, underscores, and dashes only. */
    private static Pattern graphIdPattern = Pattern.compile("[\\p{Alnum}_-]*");
    
    /** 
     * Sets the base path to serialized graph files in the filesystem.
     * This does the same thing as setResource, except the parameter is interpreted as a file path.
     */
    public void setPath(String path) {
        this.resourceBase = "file:" + path;
    }

    /**
     * This property allows loading graphs from the classpath or a path relative to the webapp root, 
     * which can be useful in cloud computing environments where webapps must be entirely 
     * self-contained. In normal client-server operation, the ResourceLoader provided by Spring 
     * will be a ServletContextResourceLoader, so the path will be interpreted relative to the 
     * webapp root, and WARs should be handled transparently. If you want to point to a path 
     * outside the webapp, or you want to be clear about exactly where the resource is to be found, 
     * it should be prefixed with 'classpath:','file:', or 'url:'.
     */
    public void setResource(String resourceBaseName) {
        this.resourceBase = resourceBaseName;
    }

    @PostConstruct // PostConstruct means it will run on startup after all injection has occurred
    public void autoRegisterGraphs() {
        LOG.info("routerId scanning : {}", scanGraphIds);
        if (scanGraphIds) {
            scanRegister();
        }
        if ( ! graphs.containsKey(defaultRouterId)) {
            LOG.info("Attempting to load graph for default routerId '{}'.", defaultRouterId);
            registerGraph(defaultRouterId, true);
        }
        if (this.getGraphIds().isEmpty()) {
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

    private Graph loadGraph(String graphId) {
        if ( ! graphIdLegal(graphId)) {
            LOG.error("graphId '{}' contains characters other than alphanumeric, underscore, and dash.", graphId);
            return null;
        }
        LOG.debug("loading serialized graph for graphId {}", graphId);
        String resourceLocation = String.format("%s/%s/Graph.obj", resourceBase, graphId);
        LOG.debug("graph file for routerId '{}' is at {}", graphId, resourceLocation);
        Resource graphResource;
        InputStream is;
        try {
            graphResource = resourceLoader.getResource(resourceLocation);
            //graphResource = resourceBase.createRelative(graphId);
            is = graphResource.getInputStream();
        } catch (IOException ex) {
            LOG.warn("Graph file not found or not openable for graphId '{}' under {}", graphId, resourceBase);
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
            for (String graphId : this.getGraphIds()) {
                boolean success = registerGraph(graphId, preEvict);
                allSucceeded &= success;
            }
        }
        return allSucceeded;
    }
    
    @Override
    public Collection<String> getGraphIds() {
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
    public boolean evictGraph(String graphId) {
        LOG.debug("evicting graph {}", graphId);
        synchronized (graphs) {
            Graph existing = graphs.remove(graphId);
            return existing != null;
        }
    }

    private boolean graphIdLegal(String graphId) {
        Matcher m = graphIdPattern.matcher(graphId);
        return m.matches();
        
    }

    @Override
    public boolean registerGraph(String graphId, Graph graph) {
        Graph existing = graphs.put(graphId, graph);
        return existing == null;
    }

    @Override
    public void evictAll() {
        graphs.clear();
    }
}
