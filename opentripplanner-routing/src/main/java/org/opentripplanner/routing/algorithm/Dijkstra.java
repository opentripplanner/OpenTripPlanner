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

package org.opentripplanner.routing.algorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opentripplanner.routing.contraction.Shortcut;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.VertexIngress;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.SPTVertex;

/**
 * Find the shortest path between graph vertices using Dijkstra's algorithm. 
 */
public class Dijkstra {

    Vertex taboo;
    private Vertex origin;
    private BasicShortestPathTree spt;
    private FibHeap<SPTVertex> queue;
    private TraverseOptions options;
    private HashSet<Vertex> closed;
    
    private int hopLimit;
    private Graph graph;
    
    public Dijkstra(Graph graph, Vertex origin, TraverseOptions options, Vertex taboo) {
        this(graph, origin, options, taboo, Integer.MAX_VALUE);
    }
    /**
     * 
     * @param taboo Do not consider any paths passing through this vertex
     */
    public Dijkstra(Graph graph, Vertex origin, TraverseOptions options, Vertex taboo, int hopLimit) {
        this.graph = graph;
        this.origin = origin;
        this.options = options;
        this.taboo = taboo;
        this.hopLimit = hopLimit;
        spt = new BasicShortestPathTree();
        
        queue = new FibHeap<SPTVertex>(graph.getVertices().size());
        State init = new State();
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);
        queue.insert(spt_origin, spt_origin.weightSum);
        closed = new HashSet<Vertex> ();
    }
    
    /**
     * Plots a path on graph from origin to target, departing at the time 
     * given in state and with the options options.  
     * 
     * @param graph
     * @param origin
     * @param target
     * @return the shortest path, or null if none is found
     */

    public BasicShortestPathTree getShortestPathTree(Vertex target, double weightLimit) {
        
        // Iteration Variables
        SPTVertex spt_u, spt_v;

        closed.add(taboo);
        
        while (!queue.empty()) { // Until the priority queue is empty:
            spt_u = queue.peek_min(); // get the lowest-weightSum Vertex 'u',
            if (spt_u.weightSum > weightLimit) {
                return spt;
            }

            Vertex fromv = spt_u.mirror;
            if (fromv == target)
                break;

            queue.extract_min();
            
            closed.add(fromv);

            Iterable<Edge> outgoing = graph.getOutgoing(spt_u.mirror);

            for (Edge edge : outgoing) {
                State state = spt_u.state;

                TraverseResult wr = edge.traverse(state, options);
                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }
                
                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge " + edge);
                }
                
                EdgeNarrative er = wr.getEdgeNarrative();
                Vertex toVertex = er.getToVertex();
                if (closed.contains(toVertex)) {
                    continue;
                }
                
                double new_w = spt_u.weightSum + wr.weight;
             
                spt_v = spt.addVertex(toVertex, wr.state, new_w, options, spt_u.hops + 1);

                if (spt_v != null) {
                    spt_v.setParent(spt_u, edge,er);

                    if (spt_u.hops < hopLimit) {
                        queue.insert_or_dec_key(spt_v, new_w);
                    }
                }
            }
        }
        return spt;
    }
    
    private HashMap<Vertex, List<VertexIngress>> neighbors;
    private HashSet<String> targets = null;
    
    public BasicShortestPathTree getShortestPathTree(double weightLimit, int nodeLimit) {
        
        // Iteration Variables
        SPTVertex spt_u, spt_v;

        if (targets != null) {
            targets.remove(origin);
        }
        
        closed.add(taboo);

        while (!queue.empty()) { // Until the priority queue is empty:
            
            spt_u = queue.peek_min(); // get the lowest-weightSum Vertex 'u',

            Vertex fromv = spt_u.mirror;

            if (nodeLimit-- <= 0) {
                return spt;
            }
            queue.extract_min();
            
            closed.add(fromv);
            if (targets != null) {
                targets.remove(fromv);
                if (targets.size() == 0) {
                    // all targets reached
                    return spt;
                }
            }
            
            Iterable<Edge> outgoing = graph.getOutgoing(fromv);
            State state = spt_u.state;

            for (Edge edge : outgoing) {
                if (!(edge instanceof TurnEdge || edge instanceof FreeEdge || edge instanceof Shortcut || edge instanceof PlainStreetEdge)) {
                    //only consider street edges when contracting
                    continue;
                }

                TraverseResult wr = edge.traverse(state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }
                
                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge " + edge);
                }
                
                EdgeNarrative er = wr.getEdgeNarrative();
                Vertex toVertex = er.getToVertex();

                if (closed.contains(toVertex)) {
                    continue;
                }
                
                double new_w = spt_u.weightSum + wr.weight;

                spt_v = spt.addVertex(toVertex, wr.state, new_w, options, spt_u.hops + 1);

                if (spt_v != null) {
                    spt_v.setParent(spt_u, edge,er);
                    
                    if (spt_u.hops < hopLimit && new_w < weightLimit) {
                        queue.insert_or_dec_key(spt_v, new_w);
                    }
                }

                if (neighbors != null && neighbors.containsKey(toVertex)) {
                    SPTVertex parent = spt.getVertex(toVertex);
                    for (VertexIngress w : neighbors.get(toVertex)) {
                        State newState = wr.state.incrementTimeInSeconds((int) w.time);
                        double neighborWeight = w.weight + new_w;
                        if (neighborWeight < weightLimit) {
                            SPTVertex spt_w = spt.addVertex(w.vertex, newState, neighborWeight, options, spt_u.hops + 2);
                            if (spt_w != null) {
                                spt_w.setParent(parent, w.edge,w.edge);
                                queue.insert_or_dec_key(spt_w, neighborWeight);
                            }
                        }
                    }
                }
                
            }
        }
        return spt;
    }

    public void setNeighbors(HashMap<Vertex, List<VertexIngress>> neighbors2) {
        this.neighbors = neighbors2;
    }
    
    public void setTargets(HashSet<String> targets) {
        this.targets = targets;
    }
}
