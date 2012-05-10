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
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Scope("singleton")
public class GraphServiceImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private String pathPattern;

    private Map<String, Graph> graphs = new HashMap<String, Graph>();

    private Map<String, Long> modTimes = new HashMap<String, Long>();

    private LoadLevel loadLevel;
    
    private String defaultRouterId = "";

    private boolean synchronousReload = true;

    public void setPath(String path) {
        this.pathPattern = path;
    }

    @Override
    public void refreshGraphs() {
        //////////////////
    }

    @Override
    @PostConstruct // This means it will run on startup
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
        File graphFile = getFileForRouterId(routerId);
        // key on filename, mapping all routerIds to the same graph when there is no {} in pattern
        graph = graphs.get(graphFile.getAbsolutePath());
        Long modTime = modTimes.get(graphFile.getAbsolutePath());
        LOG.debug("graph for routerId '{}' is at {}", routerId, graphFile.getAbsolutePath());
        if (graphFile != null && graphFile.exists()) {
            if (modTime == null || graphFile.lastModified() > modTime) {
                LOG.debug("this graph has changed or was not yet loaded");
                modTime = graphFile.lastModified();
                try {
                    graph = Graph.load(graphFile, loadLevel);
                    modTimes.put(graphFile.getAbsolutePath(), modTime);
                    graphs.put(graphFile.getAbsolutePath(), graph);
                } catch (Exception ex) {
                    LOG.error("Exception while loading graph from {}.", graphFile);
                    throw new RuntimeException("error loading graph from " + graphFile, ex);
                }
            } else {
                LOG.debug("returning cached graph {} for routerId '{}'", graph, routerId);
            }
        } else {
            LOG.warn("graph file not found: {}", graphFile);
        }
        return graph;
    }

    private File getFileForRouterId(String routerId) {
        if (routerId.indexOf("../") != -1) {
            LOG.warn("attempt to navigate up the directory hierarchy using a routerId");
            return null;
        } else {
            String fileName = pathPattern.replace("{}", routerId);
            return new File(fileName.concat("/Graph.obj"));
        }
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
}
