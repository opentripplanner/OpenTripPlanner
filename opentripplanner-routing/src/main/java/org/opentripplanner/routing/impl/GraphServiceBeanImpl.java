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

import java.util.Arrays;
import java.util.List;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Graph.LoadLevel;
import org.opentripplanner.routing.services.GraphService;

/**
 * This simple implementation of {@link GraphService} is mostly useful for testing
 * 
 * @see GraphServiceImpl
 * @see GraphService
 */
public class GraphServiceBeanImpl implements GraphService {

    private Graph graph;

    // 0-arg bean constructor
    public GraphServiceBeanImpl() {
    }
    
    public GraphServiceBeanImpl(Graph graph) {
        this.graph = graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    @Override
    public void setLoadLevel(LoadLevel level) {

    }

    @Override
    public Graph getGraph(String routerId) {
        return graph;
    }

    @Override
    public List<String> getRouterIds() {
        return Arrays.asList("default");
    }

    @Override
    public boolean registerGraph(String graphId, boolean preEvict) {
        return false;
    }

    @Override
    public boolean registerGraph(String graphId, Graph graph) {
        return false;
    }

    @Override
    public boolean evictGraph(String graphId) {
        return false;
    }

    @Override
    public int evictAll() {
        return 0;
    }

    @Override
    public boolean reloadGraphs(boolean preEvict) {
        throw new UnsupportedOperationException();
    }

}
