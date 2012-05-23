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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Scope("singleton")
public class GraphServiceImpl implements GraphService, ResourceLoaderAware {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private String resourcePattern;

    private Map<String, Graph> graphs = new HashMap<String, Graph>();

    private LoadLevel loadLevel;
    
    private String defaultRouterId = "";
    
    private ResourceLoader resourceLoader;

    private boolean preloadGraphs = true;

    public void setPath(String path) {
        this.resourcePattern = "file:".concat(path);
    }

	/**
	 * This property allows loading a graph from the classpath or a path
	 * relative to the webapp root, which can be useful in cloud computing
	 * environments where webapps must be entirely self-contained. If it is
	 * present, the placeholder {} will be replaced with the router ID. In
	 * normal client-server operation, the ResourceLoader provided by Spring
	 * will be a ServletContextResourceLoader, so the path will be interpreted
	 * relative to the webapp root, and WARs should be handled transparently. If
	 * you want to point to a path outside the webapp, or you want to be clear
	 * about exactly where the resource is to be found, it should be prefixed
	 * with 'classpath:','file:', or 'url:'.
	 */
	public void setResource(String resource) {
		this.resourcePattern = resource;
	}

    @Override
    public void refreshGraphs() {
        //////////////////
    }

    @PostConstruct // This means it will run on startup
    public void preloadGraphs() {
    	if (preloadGraphs) {
    		getGraph(null); // pre-load the default graph
    	}
    }

    @Override
    public Graph getGraph() {
        return getGraph(null);
    }

    @Override
    public synchronized Graph getGraph(String routerId) {
    	if (routerId == null || routerId.isEmpty()) {
    		routerId = defaultRouterId;    		
    		LOG.debug("routerId not specified, set to default of '{}'", routerId);
    	}
        Graph graph;
        String resourceName;
        if (routerId.indexOf("../") != -1) {
            LOG.warn("attempt to navigate up the directory hierarchy using a routerId");
            return null;
        } else {
            resourceName = resourcePattern.replace("{}", routerId);
        }
        LOG.debug("graph for routerId '{}' is at {}", routerId, resourceName);
        graph = graphs.get(resourceName);
        if (graph == null) {
            LOG.debug("this graph was not yet loaded");
            InputStream is;
            try {
                Resource resource = resourceLoader.getResource(resourceName.concat("/Graph.obj"));
                is = resource.getInputStream();
            } catch (Exception e) {
                LOG.warn("graph file not found at {}", resourceName);
                if (routerId.equals(defaultRouterId)) {
                    LOG.warn("graph for default routerId {} does not exist at {}", routerId,
                            resourceName);
                    return null;
                }
                return getGraph(null); // fall back on default if graph does not exist
            }
            try {
            	graph = Graph.load(is, LoadLevel.FULL);
            	// key on resource name instead of routerId so fallbacks to defaultRouterId will all yield the same Graph
                graphs.put(resourceName, graph);
            } catch (Exception ex) {
                LOG.error("Exception while loading graph from {}.", resourceName);
                throw new RuntimeException("error loading graph from " + resourceName, ex);
            }
        } else {
            LOG.debug("returning cached graph {} for routerId '{}'", graph, routerId);
        }
        return graph;
    }

    @Override
    public void setLoadLevel(LoadLevel level) {
        if (level != loadLevel) {
            loadLevel = level;
            refreshGraphs();
        }
    }

    public void setDefaultRouterId(String routerId) {
    	this.defaultRouterId = routerId;
    }

    @Override
    public Collection<String> getGraphIds() {
        return graphs.keySet();
    }

	@Override
	public void setResourceLoader(ResourceLoader rl) {
		this.resourceLoader = rl;
	}

}
