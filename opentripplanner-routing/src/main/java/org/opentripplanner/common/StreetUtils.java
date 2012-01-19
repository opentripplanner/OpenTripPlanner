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

import org.opentripplanner.routing.edgetype.DirectEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreetUtils {

    private static Logger _log = LoggerFactory.getLogger(StreetUtils.class);

    /**
     * Make an ordinary graph into an edge-based graph.
     * @param endpoints 
     * @param coordinateToStreetNames 
     */
    public static void makeEdgeBased(Graph graph, Collection<IntersectionVertex> endpoints, 
            Map<Edge, TurnRestriction> restrictions) {
        
        /* generate turns */

        _log.debug("converting to edge-based graph");
        for (IntersectionVertex v : endpoints) {
            for (Edge e_in : v.getIncoming()) {
                PlainStreetEdge pse_in = (PlainStreetEdge) e_in;
                TurnVertex tv_in = getTurnVertexForEdge(graph, pse_in);
                TurnRestriction restriction = null;
                if (restrictions != null) {
                    restriction = restrictions.get(pse_in);
                }
                for (Edge e_out : v.getOutgoing()) {
                	// do not make turns for going around loops (several hundred in Portland)
                	if (e_in == e_out)
                		continue;
                    // only make turn edges for U turns when they are dead ends
                    if (e_in.getFromVertex() == ((DirectEdge)e_out).getToVertex() &&
                    	v.getOutgoing().size() > 1)
                    		continue;
                    TurnVertex tv_out = getTurnVertexForEdge(graph, (PlainStreetEdge) e_out);
                    TurnEdge turn = new TurnEdge(tv_in, tv_out);
                    if (restriction != null) {
                    	if (restriction.type == TurnRestrictionType.NO_TURN && restriction.to == e_out) {
                    	    turn.setRestrictedModes(restriction.modes);
                    	} else if (restriction.type == TurnRestrictionType.ONLY_TURN && restriction.to != e_in) {
                            turn.setRestrictedModes(restriction.modes);
                    	}
                    }
                }
            }
        }
        /* remove standard graph */
        for (Vertex v : endpoints) {
            graph.removeVertex(v);
        }
    }

    private static TurnVertex getTurnVertexForEdge(Graph graph, PlainStreetEdge e) {
        boolean back = e.back;
        
        String id = e.getId();
        Vertex v = graph.getVertex(id + (back ? " back" : ""));
        if (v != null) {
            return (TurnVertex) v;
        }

        TurnVertex newv = new TurnVertex(graph, id, e.getGeometry(), e.getName(), e.getLength(), back, e.getNotes());
        newv.setWheelchairAccessible(e.isWheelchairAccessible());
        newv.setBicycleSafetyEffectiveLength(e.getBicycleSafetyEffectiveLength());
        newv.setCrossable(e.isCrossable());
        newv.setPermission(e.getPermission());
        newv.setSlopeOverride(e.getSlopeOverride());
        newv.setElevationProfile(e.getElevationProfile());
        newv.setRoundabout(e.isRoundabout());
        newv.setBogusName(e.hasBogusName());
        newv.setNoThruTraffic(e.isNoThruTraffic());
        newv.setStairs(e.isStairs());
        return newv;
    }


    public static void pruneFloatingIslands(Graph graph) {
    	_log.debug("pruning");
    	Map<Vertex, HashSet<Vertex>> subgraphs = new HashMap<Vertex, HashSet<Vertex>>();
    	Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<Vertex, ArrayList<Vertex>>();
    	
    	TraverseOptions options = new TraverseOptions(new TraverseModeSet(TraverseMode.WALK));
    	
    	for (Vertex gv : graph.getVertices()) {
    		if (!(gv instanceof IntersectionVertex)) {
    			continue;
    		}
        	State s0 = new State(gv, options);
    		for (Edge e: gv.getOutgoing()) {
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
/* removed 10/27/11, since it looks like PDX is fixed.
    	for (HashSet<Vertex> island : islands) {
    		if (island.size() < 20) {
    			_log.warn("Depedestrianizing or deleting floating island at " + island.iterator().next());
    			for (Vertex vertex : island) {
    				depedestrianizeOrRemove(graph, vertex);
    			}
    		} 
    	}
*/
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
