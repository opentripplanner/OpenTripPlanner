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

package org.opentripplanner.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.opentripplanner.gbannotation.GraphConnectivity;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreetUtils {

    private static Logger _log = LoggerFactory.getLogger(StreetUtils.class);

    public static void pruneFloatingIslands(Graph graph, int maxIslandSize) {
        _log.debug("pruning");
        Map<Vertex, HashSet<Vertex>> subgraphs = new HashMap<Vertex, HashSet<Vertex>>();
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<Vertex, ArrayList<Vertex>>();

        RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK, TraverseMode.TRANSIT));

        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            State s0 = new State(gv, options);
            for (Edge e : gv.getOutgoing()) {
                Vertex in = gv;
                if (!(e instanceof StreetEdge)) {
                    continue;
                }
                State s1 = e.traverse(s0);
                if (s1 == null) {
                    continue;
                }
                Vertex out = s1.getVertex();

                ArrayList<Vertex> vertexList = neighborsForVertex.get(in);
                if (vertexList == null) {
                    vertexList = new ArrayList<Vertex>();
                    neighborsForVertex.put(in, vertexList);
                }
                vertexList.add(out);

                vertexList = neighborsForVertex.get(out);
                if (vertexList == null) {
                    vertexList = new ArrayList<Vertex>();
                    neighborsForVertex.put(out, vertexList);
                }
                vertexList.add(in);
            }
        }

        ArrayList<HashSet<Vertex>> islands = new ArrayList<HashSet<Vertex>>();
        /* associate each node with a subgraph */
        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            Vertex vertex = gv;
            if (subgraphs.containsKey(vertex)) {
                continue;
            }
            if (!neighborsForVertex.containsKey(vertex)) {
                continue;
            }
            HashSet<Vertex> subgraph = computeConnectedSubgraph(neighborsForVertex, vertex);
            for (Vertex subnode : subgraph) {
                subgraphs.put(subnode, subgraph);
            }
            islands.add(subgraph);
        }
        _log.debug(islands.size() + " sub graphs found");
        /* remove all tiny subgraphs */
        for (HashSet<Vertex> island : islands) {
            if (island.size() < maxIslandSize) {
                _log.warn(graph.addBuilderAnnotation(new GraphConnectivity(island.iterator().next(), island.size())));
                depedestrianizeOrRemove(graph, island);
            }
        }
        if (graph.removeEdgelessVertices() > 0) {
            _log.warn("Removed edgeless vertices after pruning islands.");
        }
    }

    private static void depedestrianizeOrRemove(Graph graph, Collection<Vertex> vertices) {
        for (Vertex v : vertices) {
            Collection<Edge> outgoing = new ArrayList<Edge>(v.getOutgoing());
            for (Edge e : outgoing) {
                if (e instanceof PlainStreetEdge) {
                    PlainStreetEdge pse = (PlainStreetEdge) e;
                    StreetTraversalPermission permission = pse.getPermission();
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                    if (permission == StreetTraversalPermission.NONE) {
                        pse.detach();
                    } else {
                        pse.setPermission(permission);
                    }
                }
            }
        }
        for (Vertex v : vertices) {
            if (v.getDegreeOut() + v.getDegreeIn() == 0) {
                graph.remove(v);
            }
        }
    }

    private static HashSet<Vertex> computeConnectedSubgraph(
            Map<Vertex, ArrayList<Vertex>> neighborsForVertex, Vertex startVertex) {
        HashSet<Vertex> subgraph = new HashSet<Vertex>();
        Queue<Vertex> q = new LinkedList<Vertex>();
        q.add(startVertex);
        while (!q.isEmpty()) {
            Vertex vertex = q.poll();
            for (Vertex neighbor : neighborsForVertex.get(vertex)) {
                if (!subgraph.contains(neighbor)) {
                    subgraph.add(neighbor);
                    q.add(neighbor);
                }
            }
        }
        return subgraph;
    }
}
