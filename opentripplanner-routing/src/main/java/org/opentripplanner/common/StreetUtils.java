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

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreetUtils {

    private static Logger _log = LoggerFactory.getLogger(StreetUtils.class);

    /**
     * Make an ordinary graph into an edge-based graph.
     * 
     * @param endpoints
     * @param coordinateToStreetNames
     */
    public static void makeEdgeBased(Graph graph, Collection<IntersectionVertex> endpoints,
            Map<Edge, TurnRestriction> restrictions) {

        Map<PlainStreetEdge, TurnVertex> turnVertices = new HashMap<PlainStreetEdge, TurnVertex>();
        /* generate turns */

        _log.debug("converting to edge-based graph");
        for (IntersectionVertex v : endpoints) {
            for (Edge e_in : v.getIncoming()) {
                for (Edge e_out : v.getOutgoing()) {
                    if (e_in.getFromVertex() == e_out.getToVertex() && v.getOutgoing().size() > 1)
                        // only make turn edges for U turns when they are dead ends
                        continue;
                    if (e_in instanceof PlainStreetEdge && e_out instanceof PlainStreetEdge) {
                        PlainStreetEdge pse_in = (PlainStreetEdge) e_in;
                        TurnVertex tv_in = getTurnVertexForEdge(graph, turnVertices, pse_in);
                        PlainStreetEdge pse_out = (PlainStreetEdge) e_out;
                        TurnVertex tv_out = getTurnVertexForEdge(graph, turnVertices, pse_out);
                        TurnEdge turn = new TurnEdge(tv_in, tv_out);
                        TurnRestriction restriction = null;
                        if (restrictions != null) {
                            restriction = restrictions.get(pse_in);
                        }
                        if (restriction != null) {
                            if (restriction.type == TurnRestrictionType.NO_TURN
                                    && restriction.to == e_out) {
                                turn.setRestrictedModes(restriction.modes);
                            } else if (restriction.type == TurnRestrictionType.ONLY_TURN
                                    && restriction.to != e_in) {
                                turn.setRestrictedModes(restriction.modes);
                            }
                        }
                    } else { // turn involving a plainstreetedge and a freeedge
                        Vertex fromv = null;
                        Vertex tov = null;
                        if (e_in instanceof PlainStreetEdge) {
                            fromv = getTurnVertexForEdge(graph, turnVertices,
                                    (PlainStreetEdge) e_in);
                        } else if (e_in instanceof FreeEdge) {
                            fromv = e_in.getFromVertex(); // fromv for incoming
                        }
                        if (e_out instanceof PlainStreetEdge) {
                            tov = getTurnVertexForEdge(graph, turnVertices, (PlainStreetEdge) e_out);
                        } else if (e_out instanceof FreeEdge) {
                            tov = e_out.getToVertex(); // tov for outgoing
                        }
                        if (fromv instanceof TurnVertex) {
                            new TurnEdge((TurnVertex) fromv, (StreetVertex) tov);
                        } else {
                            new FreeEdge(fromv, tov);
                        }
                    }
                }
            }
        }

        /* remove standard graph */
        for (IntersectionVertex iv : endpoints) {
            graph.removeVertex(iv);
        }
    }

    private static TurnVertex getTurnVertexForEdge(Graph graph,
            Map<PlainStreetEdge, TurnVertex> turnVertices, PlainStreetEdge pse) {

        TurnVertex tv = turnVertices.get(pse);
        if (tv != null) {
            return tv;
        }

        boolean back = pse.back;
        String id = pse.getId();
        tv = new TurnVertex(graph, id, pse.getGeometry(), pse.getName(), pse.getLength(), back,
                pse.getNotes());
        tv.setWheelchairNotes(pse.getWheelchairNotes());
        tv.setWheelchairAccessible(pse.isWheelchairAccessible());
        tv.setBicycleSafetyEffectiveLength(pse.getBicycleSafetyEffectiveLength());
        tv.setCrossable(pse.isCrossable());
        tv.setPermission(pse.getPermission());
        tv.setSlopeOverride(pse.getSlopeOverride());
        // the only cases where there will already be an elevation profile are those where it came
        // from
        // the street network (osm ele tags, for instance), so it's OK to force it here.
        tv.setElevationProfile(pse.getElevationProfile(), true);
        tv.setRoundabout(pse.isRoundabout());
        tv.setBogusName(pse.hasBogusName());
        tv.setNoThruTraffic(pse.isNoThruTraffic());
        tv.setStairs(pse.isStairs());
        turnVertices.put(pse, tv);
        return tv;
    }

    public static void pruneFloatingIslands(Graph graph) {
        _log.debug("pruning");
        Map<Vertex, HashSet<Vertex>> subgraphs = new HashMap<Vertex, HashSet<Vertex>>();
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<Vertex, ArrayList<Vertex>>();

        TraverseOptions options = new TraverseOptions(new TraverseModeSet(TraverseMode.WALK, TraverseMode.TRANSIT));

        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof IntersectionVertex)) {
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
            if (!(gv instanceof IntersectionVertex)) {
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
    	
    	/* remove all tiny subgraphs */
        for (HashSet<Vertex> island : islands) {
            if (island.size() < 20) {
                _log.warn("Depedestrianizing or deleting floating island at "
                        + island.iterator().next());
                for (Vertex vertex : island) {
                    depedestrianizeOrRemove(graph, vertex);
                }
            }
        }
    }

    private static void depedestrianizeOrRemove(Graph graph, Vertex v) {
        Collection<Edge> outgoing = new ArrayList<Edge>(v.getOutgoing());
        for (Edge e : outgoing) {
            if (e instanceof PlainStreetEdge) {
                PlainStreetEdge pse = (PlainStreetEdge) e;
                StreetTraversalPermission permission = pse.getPermission();
                permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                if (permission == StreetTraversalPermission.NONE) {
                    pse.detach();
                } else {
                    pse.setPermission(permission);
                }
            }
        }
        if (v.getOutgoing().size() == 0) {
            graph.removeVertexAndEdges(v);
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
