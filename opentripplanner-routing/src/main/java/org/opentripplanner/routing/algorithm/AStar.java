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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.MultiShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.util.NullExtraEdges;


/**
 * Find the shortest path between graph vertices using A*. 
 */
public class AStar {

    /**
     * Plots a path on graph from origin to target, departing at the time 
     * given in state and with the options options.
     * 
     * @param graph
     * @param origin
     * @param target
     * @param init
     * @param options
     * @return the shortest path, or null if none is found
     */
    public static ShortestPathTree getShortestPathTree(Graph gg, String from_label,
            String to_label, State init, TraverseOptions options) {
        // Goal Variables
        String origin_label = from_label;
        String target_label = to_label;

        // Get origin vertex to make sure it exists
        Vertex origin = gg.getVertex(origin_label);
        Vertex target = gg.getVertex(target_label);

        return getShortestPathTree(gg, origin, target, init, options);
    }

    public static ShortestPathTree getShortestPathTreeBack(Graph gg, String from_label,
            String to_label, State init, TraverseOptions options) {
        // Goal Variables
        String origin_label = from_label;
        String target_label = to_label;

        // Get origin vertex to make sure it exists
        Vertex origin = gg.getVertex(origin_label);
        Vertex target = gg.getVertex(target_label);

        return getShortestPathTreeBack(gg, origin, target, init, options);
    }

    /**
     * Plots a path on graph from origin to target, ARRIVING at the time 
     * given in state and with the options options.  
     * 
     * @param graph
     * @param origin
     * @param target
     * @param init
     * @param options
     * @return the shortest path, or null if none is found
     */
    public static ShortestPathTree getShortestPathTreeBack(Graph graph, Vertex origin, Vertex target,
            State init, TraverseOptions options) {
        if (!options.isArriveBy()) {
            throw new RuntimeException("Reverse paths must call options.setArriveBy(true)");
        }
        if (origin == null || target == null) {
            return null;
        }
        
        // Return Tree
        ShortestPathTree spt;
        if (options.modes.getTransit()) { 
            spt = new MultiShortestPathTree();
        } else {
            spt = new BasicShortestPathTree();
        }
        
        /* Run backwards from the target to the origin */
        Vertex tmp = origin;
        origin = target;
        target = tmp;

        /* generate extra edges for StreetLocations */
        Map<Vertex, ArrayList<Edge>> extraEdges;
        if (origin instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, ArrayList<Edge>>();
            Iterable<Edge> extra = ((StreetLocation)origin).getExtra();
            for (Edge edge : extra) {
                Vertex tov = edge.getToVertex();
                ArrayList<Edge> edges = extraEdges.get(tov);
                if (edges == null) {
                    edges = new ArrayList<Edge>(); 
                    extraEdges.put(tov, edges);
                }
                edges.add(edge);
            }
        } else {
            extraEdges = new NullExtraEdges();
        }
        if (target instanceof StreetLocation) {
            if (extraEdges instanceof NullExtraEdges) {
                extraEdges = new HashMap<Vertex, ArrayList<Edge>>();
            }
            Iterable<Edge> extra = ((StreetLocation)target).getExtra();
            for (Edge edge : extra) {
                Vertex tov = edge.getToVertex();
                ArrayList<Edge> edges = extraEdges.get(tov);
                if (edges == null) {
                    edges = new ArrayList<Edge>(); 
                    extraEdges.put(tov, edges);
                }
                edges.add(edge);
            }
        }
        final double max_speed = getMaxSpeed(options);
        
        double distance = origin.fastDistance(target) / max_speed;
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        FibHeap<SPTVertex> pq = new FibHeap<SPTVertex>(graph.getVertices().size() + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + distance);
        
        boolean useTransit = options.modes.getTransit();
        HashSet<Vertex> closed = new HashSet<Vertex>(100000);
        
        // Iteration Variables
        SPTVertex spt_u, spt_v;
        while (!pq.empty()) { // Until the priority queue is empty:
            spt_u = pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            Vertex tov = spt_u.mirror;
            if (tov == target)
                break;
            
            closed.add(tov);

            GraphVertex gv = graph.getGraphVertex(tov);
            
            Collection<Edge> incoming;
            if (gv == null) {
                incoming = new ArrayList<Edge>();
            } else {
                incoming = gv.getIncoming();
            }
            
            if (extraEdges.containsKey(tov)) {
                List<Edge> newIncoming = new ArrayList<Edge>();
                for (Edge edge : incoming)
                    newIncoming.add(edge);
                newIncoming.addAll(extraEdges.get(tov));
                incoming = newIncoming;
            }


            for (Edge edge : incoming) {
                State state = spt_u.state;
                if (edge.getFromVertex() == target) {
                    state = state.clone();
                    state.lastEdgeWasStreet = false;
                }
                    
                if (edge instanceof PatternAlight && state.numBoardings > options.maxTransfers) {
                    continue;
                }
                
                TraverseResult wr = edge.traverseBack(state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge " + edge);
                }

                Vertex fromv = edge.getFromVertex();
                double new_w = spt_u.weightSum + wr.weight;
                distance = tov.fastDistance(target) / max_speed;
                if (useTransit) {
                    distance = Math.min(distance + options.boardCost,
                        options.walkReluctance * tov.fastDistance(target) / options.speed);
                }
                
                double heuristic_distance = new_w + distance;
                if (heuristic_distance > options.maxWeight || wr.state.getTime() < options.worstTime) {
                    //too expensive to get here
                    continue;
                }
                
                spt_v = spt.addVertex(fromv, wr.state, new_w, options);
                if (spt_v != null) {
                    spt_v.setParent(spt_u, edge);
                    if (!closed.contains(fromv)) {
                        pq.insert_or_dec_key(spt_v, heuristic_distance);
                    }
                }
            }
        }
        return spt;
    }

    /**
     * Plots a path on graph from origin to target, DEPARTING at the time 
     * given in state and with the options options.  
     * 
     * @param graph
     * @param origin
     * @param target
     * @param init
     * @param options
     * @return the shortest path, or null if none is found
     */
    public static ShortestPathTree getShortestPathTree(Graph graph, Vertex origin, Vertex target,
            State init, TraverseOptions options) {

        if (origin == null || target == null) {
            return null;
        }

        // Return Tree
        ShortestPathTree spt;
        if (options.modes.getTransit()) { 
            spt = new MultiShortestPathTree();
        } else {
            spt = new BasicShortestPathTree();
        }
        
        /* generate extra edges for StreetLocations */
        Map<Vertex, ArrayList<Edge>> extraEdges;
        if (origin instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, ArrayList<Edge>>();
            Iterable<Edge> extra = ((StreetLocation)origin).getExtra();
            for (Edge edge : extra) {
                Vertex fromv = edge.getFromVertex();
                ArrayList<Edge> edges = extraEdges.get(fromv);
                if (edges == null) {
                    edges = new ArrayList<Edge>(); 
                    extraEdges.put(fromv, edges);
                }
                edges.add(edge);
            }
        } else {
            extraEdges = new NullExtraEdges();
        }
        if (target instanceof StreetLocation) {
            if (extraEdges instanceof NullExtraEdges) {
                extraEdges = new HashMap<Vertex, ArrayList<Edge>>();
            }
            Iterable<Edge> extra = ((StreetLocation)target).getExtra();
            for (Edge edge : extra) {
                Vertex fromv = edge.getFromVertex();
                ArrayList<Edge> edges = extraEdges.get(fromv);
                if (edges == null) {
                    edges = new ArrayList<Edge>(); 
                    extraEdges.put(fromv, edges);
                }
                edges.add(edge);
            }
        }
        final double max_speed = getMaxSpeed(options);
        double distance = origin.fastDistance(target) / max_speed;
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        FibHeap<SPTVertex> pq = new FibHeap<SPTVertex>(graph.getVertices().size() + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + distance);

        boolean useTransit = options.modes.getTransit();
        
        /* the core of the A* algorithm */
        while (!pq.empty()) { // Until the priority queue is empty:
            SPTVertex spt_u = pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            Vertex fromv = spt_u.mirror;
            if (fromv == target) {
                break;
            }
            GraphVertex gv = graph.getGraphVertex(fromv);
 
            Collection<Edge> outgoing;
            if (gv == null) {
                outgoing = new ArrayList<Edge>(1);
            } else {
                outgoing = gv.getOutgoing();
            }
            
            if (extraEdges.containsKey(fromv)) {
                List<Edge> newOutgoing = new ArrayList<Edge>();
                for (Edge edge : outgoing)
                    newOutgoing.add(edge);
                newOutgoing.addAll(extraEdges.get(fromv));
                outgoing = newOutgoing;
            }
            if (fromv instanceof StreetLocation) {
                StreetLocation sl = (StreetLocation) fromv;
                List<Edge> extra = sl.getExtra();
                if (extra.size() > 0) {
                    List<Edge> newOutgoing = new ArrayList<Edge>(outgoing.size() + extra.size());
                    for (Edge edge : outgoing)
                        newOutgoing.add(edge);
                    newOutgoing.addAll(extra);
                    outgoing = newOutgoing;
                }
            }

            for (Edge edge : outgoing) {

                State state = spt_u.state;
                Vertex tov = edge.getToVertex();
                if (tov == target) {
                    state = state.clone();
                    state.lastEdgeWasStreet = false;
                }
                
                if (edge instanceof PatternBoard && state.numBoardings > options.maxTransfers) {
                    continue;
                }

                TraverseResult wr = edge.traverse(state, options);
                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }
                
                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight));
                }
                
                double new_w = spt_u.weightSum + wr.weight;

                distance = tov.fastDistance(target) / max_speed;
                if (useTransit) {
                    int boardCost;
                    if (edge instanceof PatternHop || edge instanceof PatternBoard || edge instanceof PatternDwell ||
                            edge instanceof PatternInterlineDwell || edge instanceof Board || edge instanceof Hop) {
                        boardCost = 0;
                    } else {
                        boardCost = options.boardCost;
                    }
                    distance = Math.min(distance + boardCost,
                        options.walkReluctance * tov.fastDistance(target) / options.speed);
                }
                
                double heuristic_distance = new_w + distance;
                if (heuristic_distance > options.maxWeight || wr.state.getTime() > options.worstTime) {
                    //too expensive to get here
                    continue;
                }

                SPTVertex spt_v = spt.addVertex(tov, wr.state, new_w, options);
                if (spt_v != null) {
                    spt_v.setParent(spt_u, edge);
                    pq.insert_or_dec_key(spt_v, heuristic_distance);
                }
            }
        }

        return spt;
    }

    public static double getMaxSpeed(TraverseOptions options) {
        if (options.modes.contains(TraverseMode.TRANSIT)) {
            //assume that the max average transit speed over a hop is 10 m/s, which is so far true for
            //New York and Portland
            return 10;
        } else {
            if (options.optimizeFor == OptimizeType.QUICK) {
                return options.speed;
            } else {
                //assume that the best route is no more than 10 times better than
                //the as-the-crow-flies flat base route.  
                return options.speed * 10; 
            }
        }
    }

}
