/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.algorithm.strategies.TableRemainingWeightHeuristic;
import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ContractionRoutingServiceImpl implements RoutingService {

    private GraphService _graphService;

    @Autowired
    public void setGraphService(GraphService graphService) {
        _graphService = graphService;
    }

    @Override
    public List<GraphPath> route(Vertex fromVertex, Vertex toVertex, State state,
            TraverseOptions options) {

        ContractionHierarchySet hierarchies = _graphService.getContractionHierarchySet();

        ContractionHierarchy hierarchy = null;
        hierarchy = hierarchies.getHierarchy(options);

        if (hierarchy == null
        	|| (options.remainingWeightHeuristic instanceof TableRemainingWeightHeuristic 
        	    && options.getModes().getTransit())) {
            Graph _graph = hierarchies.getGraph();
            if (options.isArriveBy()) {

                ShortestPathTree spt = AStar.getShortestPathTreeBack(_graph, fromVertex, toVertex,
                        state, options);
                if (spt == null) {
                    return null;
                }
                List<GraphPath> paths = spt.getPaths(fromVertex, true);
                for (GraphPath path : paths)
                    path.reverse();
                return paths;
            } else {
                ShortestPathTree spt = AStar.getShortestPathTree(_graph, fromVertex, toVertex,
                        state, options);
                if (spt == null) {
                    return null;
                }
                return spt.getPaths(toVertex, true);
            }
        }

        GraphPath path = hierarchy.getShortestPath(fromVertex, toVertex, state, options);
        if (path == null)
            return Collections.emptyList();
        return Arrays.asList(path);
    }

    @Override
    public GraphPath route(Vertex fromVertex, Vertex toVertex, List<Vertex> intermediates,
            State state, TraverseOptions options) {

        Map<Vertex, HashMap<Vertex, GraphPath>> paths = new HashMap<Vertex, HashMap<Vertex, GraphPath>>();

        HashMap<Vertex, GraphPath> firstLegPaths = new HashMap<Vertex, GraphPath>();
        paths.put(fromVertex, firstLegPaths);

        // compute shortest paths between each pair of vertices
        for (Vertex v : intermediates) {

            /**
             * Find initial paths from the source vertex to the intermediate vertex
             */
            List<GraphPath> firstPaths = route(fromVertex, v, state, options);
            if (!firstPaths.isEmpty()) {
                firstLegPaths.put(v, firstPaths.get(0));
            }

            HashMap<Vertex, GraphPath> outPaths = new HashMap<Vertex, GraphPath>();
            paths.put(v, outPaths);

            for (Vertex tv : intermediates) {
                /**
                 * We probably don't need to compute paths where the source and target vertex are
                 * the same
                 */
                if (v == tv)
                    continue;

                List<GraphPath> morePaths = route(v, tv, state, options);
                if (!morePaths.isEmpty()) {
                    outPaths.put(tv, morePaths.get(0));
                }
            }

            /**
             * Find paths from the intermediate vertex to the target vertex
             */
            List<GraphPath> lastPaths = route(v, toVertex, state, options);
            if (!lastPaths.isEmpty())
                outPaths.put(toVertex, lastPaths.get(0));
        }

        // compute shortest path overall
        HashSet<Vertex> vertices = new HashSet<Vertex>();
        vertices.addAll(intermediates);
        GraphPath shortestPath = TSPPathFinder.findShortestPath(toVertex, fromVertex, paths,
                vertices, state, options);
        return shortestPath;
    }

}
