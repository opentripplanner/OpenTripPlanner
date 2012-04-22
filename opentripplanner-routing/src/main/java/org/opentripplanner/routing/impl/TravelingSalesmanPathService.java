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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.contraction.ContractionHierarchy;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.LegSwitchingEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

public class TravelingSalesmanPathService implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(TravelingSalesmanPathService.class);
    private static final int MAX_INTERMEDIATES = 4;

    @Autowired private GraphService graphService;
    // @Resource("name") or @Qualifier
    @Autowired private SPTService tspSptService;
    @Autowired private PathService chainedPathService;
    
    @Override
    public List<GraphPath> getPaths(TraverseOptions options) {
        if (options.getIntermediatePlaces() == null || options.getIntermediatePlaces().size() == 0) {
            // no intermediate places specified, chain to main path service
            return chainedPathService.getPaths(options);
        }
        
        /* intermediate places present, intercept request */
        Graph graph = graphService.getGraph(options.getRouterId());
        long time = options.dateTime;
        options.setRoutingContext(graph);
        List<Vertex> vertices = options.rctx.intermediateVertices;
        vertices.add(options.rctx.toVertex);
        options.intermediatePlaces.clear();

        if (options.intermediatePlacesOrdered) {
            // simple case: intermediate places are in order.
            List<GraphPath> paths = new ArrayList<GraphPath>();
            Vertex previousVertex = options.rctx.fromVertex;
            for (Vertex v : vertices) {
                options.dateTime = time;
                options.setRoutingContext(graph, previousVertex, v);
                List<GraphPath> partialPaths = chainedPathService.getPaths(options);
                if (partialPaths == null || partialPaths.size() == 0)
                    return null;
                GraphPath path = partialPaths.get(0);
                paths.add(path);
                previousVertex = v;
                time = path.getEndTime();
            }
            return Arrays.asList(joinPaths(paths));
        } 

        // Difficult case: intermediate places can occur in any order (Traveling Salesman Problem)
        Graph graph = graphService.getGraph(options.routerId);
        options.setRoutingContext(graph);

        Map<Vertex, HashMap<Vertex, GraphPath>> paths = new HashMap<Vertex, HashMap<Vertex, GraphPath>>();

        HashMap<Vertex, GraphPath> firstLegPaths = new HashMap<Vertex, GraphPath>();
        paths.put(fromVertex, firstLegPaths);

        // compute shortest paths between each pair of vertices
        for (Vertex v : intermediates) {

            /* Find initial paths from the source vertex to the intermediate vertex */
            List<GraphPath> firstPaths = route(new State(time, fromVertex, options), v);
            if (!firstPaths.isEmpty()) {
                firstLegPaths.put(v, firstPaths.get(0));
            }

            /* Find paths from this intermediate vertex to each other intermediate */
            HashMap<Vertex, GraphPath> outPaths = new HashMap<Vertex, GraphPath>();
            paths.put(v, outPaths);

            State intermediateState = new State(time, v, options);

            for (Vertex tv : intermediates) {
                /* We don't need to compute paths where the source and target vertex are the same */
                if (v == tv)
                    continue;

                List<GraphPath> morePaths = route(intermediateState, tv);
                if (!morePaths.isEmpty()) {
                    outPaths.put(tv, morePaths.get(0));
                }
            }

            /* Find paths from the intermediate vertex to the target vertex */
            List<GraphPath> lastPaths = route(intermediateState, toVertex);
            if (!lastPaths.isEmpty())
                outPaths.put(toVertex, lastPaths.get(0));
        }

        // compute shortest path overall
        HashSet<Vertex> vertices = new HashSet<Vertex>();
        vertices.addAll(intermediates);
        return TSPPathFinder.findShortestPath(toVertex, fromVertex, paths, vertices, time,
                options);
    }

    private GraphPath joinPaths(List<GraphPath> paths) {
        State lastState = paths.get(0).states.getLast();
        GraphPath newPath = new GraphPath(lastState, false);
        Vertex lastVertex = lastState.getVertex();
        for (GraphPath path : paths.subList(1, paths.size())) {
            lastState = newPath.states.getLast();
            // add a leg-switching state
            LegSwitchingEdge legSwitchingEdge = new LegSwitchingEdge(lastVertex, lastVertex);
            lastState = legSwitchingEdge.traverse(lastState);
            newPath.edges.add(legSwitchingEdge);
            newPath.states.add(lastState);
            // add the next subpath
            for (Edge e : path.edges) {
                lastState = e.traverse(lastState);
                newPath.edges.add(e);
                newPath.states.add(lastState);
            }
            lastVertex = path.getEndVertex();
        }
        return newPath;
    }

}
