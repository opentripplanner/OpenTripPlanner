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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/** 
 * 
 * NullExtraEdges is used to speed up checks for extra edges in the (common) case 
 * where there are none. Extra edges come from StreetLocationFinder, where 
 * they represent the edges between a location on a street segment and the 
 * corners at the ends of that segment. 
 */
class NullExtraEdges implements Map<Vertex, Edge> {

    @Override
    public void clear() {
    }

    @Override
    public boolean containsKey(Object arg0) {
        return false;
    }

    @Override
    public boolean containsValue(Object arg0) {
        return false;
    }

    @Override
    public Set<java.util.Map.Entry<Vertex, Edge>> entrySet() {
        return null;
    }

    @Override
    public Edge get(Object arg0) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<Vertex> keySet() {
        return null;
    }

    @Override
    public Edge put(Vertex arg0, Edge arg1) {
        return null;
    }

    @Override
    public void putAll(Map<? extends Vertex, ? extends Edge> arg0) {
    }

    @Override
    public Edge remove(Object arg0) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public Collection<Edge> values() {
        return null;
    }
}

/**
 * Find the shortest path between graph vertices using A*. 
 */
public class AStar {

    static final double MAX_SPEED = 10.0;

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
     * Plots a path on graph from origin to target, arriving at the time 
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

        if (origin == null || target == null) {
            return null;
        }

        /* Run backwards from the target to the origin */
        Vertex tmp = origin;
        origin = target;
        target = tmp;
        
        /* generate extra edges for StreetLocations */ 
        Map<Vertex, Edge> extraEdges;
        if (target instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, Edge>();
            Iterable<Edge> outgoing = target.getOutgoing();
            for (Edge edge : outgoing) {
                extraEdges.put(edge.getToVertex(), edge);
            }
        } else {
            extraEdges = new NullExtraEdges();
        }
        // Return Tree
        ShortestPathTree spt = new ShortestPathTree();

        double distance = origin.distance(target) / MAX_SPEED;
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        FibHeap pq = new FibHeap(graph.getVertices().size() + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + distance);

        // Iteration Variables
        SPTVertex spt_u, spt_v;
        while (!pq.empty()) { // Until the priority queue is empty:
            spt_u = (SPTVertex) pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            if (spt_u.mirror == target)
                break;

            Iterable<Edge> incoming = spt_u.mirror.getIncoming();

            if (extraEdges.containsKey(spt_u.mirror)) {
                List<Edge> newIncoming = new ArrayList<Edge>();
                for (Edge edge : spt_u.mirror.getOutgoing()) {
                    newIncoming.add(edge);
                }
                newIncoming.add(extraEdges.get(spt_u.mirror));
                incoming = newIncoming;
            }

            for (Edge edge : incoming) {

                TraverseResult wr = edge.traverseBack(spt_u.state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge " + edge);
                }

                Vertex fromv = edge.getFromVertex();
                distance = fromv.distance(target) / MAX_SPEED;
                double new_w = spt_u.weightSum + wr.weight;
                double old_w;

                spt_v = spt.getVertex(fromv);
                // if this is the first time edge.tov has been visited
                if (spt_v == null) {
                    old_w = Integer.MAX_VALUE;
                    spt_v = spt.addVertex(fromv, wr.state, new_w, options);
                } else {
                    old_w = spt_v.weightSum + distance;
                }

                // If the new way of getting there is better,
                if (new_w + distance < old_w) {
                    // Set the State of v in the SPT to the current winner
                    spt_v.state = wr.state;
                    spt_v.weightSum = new_w;
                    if (old_w == Integer.MAX_VALUE) {
                        pq.insert(spt_v, new_w + distance);
                    } else {
                        pq.insert_or_dec_key(spt_v, new_w + distance);
                    }

                    spt_v.setParent(spt_u, edge);
                }
            }
        }
        return spt;
    }

    public static ShortestPathTree getShortestPathTree(Graph graph, Vertex origin, Vertex target,
            State init, TraverseOptions options) {

        if (origin == null || target == null) {
            return null;
        }

        /* generate extra edges for StreetLocations */
        Map<Vertex, Edge> extraEdges;
        if (target instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, Edge>();
            Iterable<Edge> incoming = target.getIncoming();
            for (Edge edge : incoming) {
                extraEdges.put(edge.getFromVertex(), edge);
            }
        } else {
            extraEdges = new NullExtraEdges();
        }

        // Return Tree
        ShortestPathTree spt = new ShortestPathTree();

        double distance = origin.distance(target) / MAX_SPEED;
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        FibHeap pq = new FibHeap(graph.getVertices().size() + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + distance);

        // Iteration Variables
        SPTVertex spt_u, spt_v;
        while (!pq.empty()) { // Until the priority queue is empty:
            spt_u = (SPTVertex) pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            if (spt_u.mirror == target)
                break;

            Iterable<Edge> outgoing = spt_u.mirror.getOutgoing();

            if (extraEdges.containsKey(spt_u.mirror)) {
                List<Edge> newOutgoing = new ArrayList<Edge>();
                for (Edge edge : spt_u.mirror.getOutgoing())
                    newOutgoing.add(edge);
                newOutgoing.add(extraEdges.get(spt_u.mirror));
                outgoing = newOutgoing;
            }

            for (Edge edge : outgoing) {

                TraverseResult wr = edge.traverse(spt_u.state, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                if (wr == null) {
                    continue;
                }

                if (wr.weight < 0) {
                    throw new NegativeWeightException(String.valueOf(wr.weight));
                }

                Vertex tov = edge.getToVertex();
                distance = tov.distance(target) / MAX_SPEED;
                double new_w = spt_u.weightSum + wr.weight;
                double old_w;

                spt_v = spt.getVertex(tov);
                // if this is the first time edge.tov has been visited
                if (spt_v == null) {
                    old_w = Double.MAX_VALUE;
                    spt_v = spt.addVertex(tov, wr.state, new_w, options);
                } else {
                    old_w = spt_v.weightSum + distance;
                }

                // If the new way of getting there is better,
                if (new_w + distance < old_w) {
                    // Set the State of v in the SPT to the current winner
                    spt_v.state = wr.state;
                    spt_v.weightSum = new_w;
                    if (old_w == Integer.MAX_VALUE) {
                        pq.insert(spt_v, new_w + distance);
                    } else {
                        pq.insert_or_dec_key(spt_v, new_w + distance);
                    }

                    spt_v.setParent(spt_u, edge);
                }
            }
        }
        return spt;
    }

}
