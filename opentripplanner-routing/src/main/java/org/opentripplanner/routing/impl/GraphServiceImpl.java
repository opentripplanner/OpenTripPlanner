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
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;

/**
 * The primary implementation of the GraphService interface. It can handle multiple graphs, each
 * with its own routerId. These graphs are loaded from serialized graph files in subdirectories
 * immediately under the specified base resource/filesystem path.
 * 
 * Delegate the file loading implementation details to the GraphServiceFileImpl.
 * 
 * @see GraphServiceFileImpl
 */
@Scope("singleton")
public class GraphServiceImpl implements GraphService {

    private static final Logger LOG = LoggerFactory.getLogger(GraphServiceImpl.class);

    private GraphServiceFileImpl decorated = new GraphServiceFileImpl();

    /** A list of routerIds to automatically register and load at startup */
    @Setter
    private List<String> autoRegister;

    /** If true, on startup register the graph in the location defaultRouterId. */
    @Setter
    private boolean attemptRegisterDefault = true;

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
        decorated.setBasePath(path);
    }

    /**
     * Based on the autoRegister list, automatically register all routerIds for which we can find a
     * graph file in a subdirectory of the resourceBase path. Also register and load the graph for
     * the defaultRouterId and warn if no routerIds are registered.
     */
    @PostConstruct
    // PostConstruct means run on startup after all injection has occurred
    private void startup() {
        if (autoRegister != null && !autoRegister.isEmpty()) {
            LOG.info("attempting to automatically register routerIds {}", autoRegister);
            LOG.info("graph files will be sought in paths relative to {}",
                    decorated.getBasePath());
            for (String routerId : autoRegister) {
                registerGraph(routerId, true);
            }
        } else {
            LOG.info("no list of routerIds was provided for automatic registration.");
        }
        if (attemptRegisterDefault
                && !decorated.getRouterIds().contains(decorated.getDefaultRouterId())) {
            LOG.info("Attempting to load graph for default routerId '{}'.",
                    decorated.getDefaultRouterId());
            registerGraph(decorated.getDefaultRouterId(), true);
        }
        if (this.getRouterIds().isEmpty()) {
            LOG.warn("No graphs have been loaded/registered. "
                    + "You must use the routers API to register one or more graphs before routing.");
        }
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
        return decorated.registerGraph(routerId, preEvict);
    }

    @Override
    public boolean registerGraph(String routerId, Graph graph) {
        return decorated.registerGraph(routerId, graph);
    }

    @Override
    public boolean evictGraph(String routerId) {
        return decorated.evictGraph(routerId);
    }

    @Override
    public int evictAll() {
        return decorated.evictAll();
    }

}
