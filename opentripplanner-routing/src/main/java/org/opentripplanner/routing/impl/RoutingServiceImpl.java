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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

class TSPPathFinder {
    static class TSPPath {
        List<Vertex> vertices;
        double cost;
        TSPPath(Vertex v, double cost) {
            vertices = new ArrayList<Vertex>();
            vertices.add(v);
            this.cost = cost;
        }
    }
    public TSPPathFinder() {}
    
    private static TSPPath findShortestPathInternal(Vertex toVertex, Vertex fromVertex,
            Map<Vertex, HashMap<Vertex, GraphPath>> paths, HashSet<Vertex> vertices) {
        
        if (vertices.size() == 0) {
            TSPPath path = new TSPPath(toVertex, getCost(paths.get(fromVertex).get(toVertex)));
            return path;
        }
        
        List<Vertex> vertexCopy = new ArrayList<Vertex> ();
        vertexCopy.addAll(vertices);
        TSPPath shortest = null;
        for (Vertex vertex : vertexCopy) {
            vertices.remove(vertex);
            TSPPath path = findShortestPathInternal(toVertex, vertex, paths, vertices);
            path.cost += getCost(paths.get(fromVertex).get(vertex));
            if (shortest == null || shortest.cost > path.cost) {
                shortest = path;
                path.vertices.add(0, vertex);
            }
            vertices.add(vertex);
        }
        return shortest;
    }

    public static GraphPath findShortestPath(Vertex toVertex, Vertex fromVertex,
            Map<Vertex, HashMap<Vertex, GraphPath>> paths, HashSet<Vertex> vertices, State state, TraverseOptions options) {
        TSPPath shortestPath = findShortestPathInternal(toVertex, fromVertex, paths, vertices);
        
        ShortestPathTree spt = new BasicShortestPathTree();
        GraphPath newPath = new GraphPath();
        Vertex lastVertex = fromVertex;
        GraphPath subPath;
        SPTVertex fromv = spt.addVertex(fromVertex, state, 0, options);
        
        for (Vertex v : shortestPath.vertices) {
            subPath = paths.get(lastVertex).get(v);
            lastVertex = v;
            for (int i = 0; i < subPath.edges.size(); ++i) {
                SPTEdge edge = subPath.edges.get(i);
                TraverseResult result = edge.traverse(state, options);
                state = result.state;
                SPTVertex tov = spt.addVertex(edge.payload.getToVertex(), state, result.weight, options);
                SPTEdge newEdge = tov.setParent(fromv, edge.payload);
                newPath.vertices.add(fromv);
                newPath.edges.add(newEdge);
                fromv = tov;
            }   
        }
        newPath.vertices.add(fromv);
        
        return newPath;
    }
    private static double getCost(GraphPath graphPath) {
        return graphPath.vertices.lastElement().weightSum;
    }

}

@Component
public class RoutingServiceImpl implements RoutingService {

    private Graph _graph;

    @Autowired
    public void setGraph(Graph graph) {
        _graph = graph;
    }

    @Override
    public GraphPath route(Vertex fromVertex, Vertex toVertex, State state, TraverseOptions options) {
        
        if (options.back) {
            ShortestPathTree spt = AStar.getShortestPathTreeBack(_graph, fromVertex, toVertex, state,
                    options);
            if (spt == null) {
                return null;
            }
            GraphPath path = spt.getPath(fromVertex);
            path.reverse();
            return path;
        } else {
            ShortestPathTree spt = AStar.getShortestPathTree(_graph, fromVertex, toVertex, state,
                    options);
            if (spt == null) {
                return null;
            }
            return spt.getPath(toVertex);
        }
    }

    @Override
    public GraphPath route(Vertex fromVertex, Vertex toVertex, List<Vertex> intermediates, State state, TraverseOptions options) {

        Map<Vertex, HashMap<Vertex, GraphPath>> paths = new HashMap<Vertex, HashMap<Vertex,GraphPath>>();
        
        HashMap<Vertex, GraphPath> firstLegPaths = new HashMap<Vertex, GraphPath>();
        paths.put(fromVertex, firstLegPaths);
        
        //compute shortest paths between each pair of vertices
        for (Vertex v: intermediates) {
            HashMap<Vertex, GraphPath> outPaths = new HashMap<Vertex, GraphPath>();
            paths.put(v, outPaths);
            GraphPath path = route(fromVertex, v, state, options);
            firstLegPaths.put (v, path);
            for (Vertex tv: intermediates) {
                path = route(v, tv, state, options);
                outPaths.put (tv, path);
            }
            path = route(v, toVertex, state, options);
            outPaths.put(toVertex, path);
        }
    
        //compute shortest path overall
        HashSet<Vertex> vertices = new HashSet<Vertex>();
        vertices.addAll(intermediates);
        GraphPath shortestPath = TSPPathFinder.findShortestPath(toVertex, fromVertex, paths, vertices, state, options);
        return shortestPath;
    }
    
}
