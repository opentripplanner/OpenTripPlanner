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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.MultiShortestPathTree;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Find the shortest path between graph vertices using A*.
 */
public class AStar {

    /**
     * Plots a path on graph from origin to target, departing at the time given in state and with
     * the options options.
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
     * Plots a path on graph from origin to target, ARRIVING at the time given in state and with the
     * options options.
     * 
     * @param graph
     * @param origin
     * @param target
     * @param init
     * @param options
     * @return the shortest path, or null if none is found
     */
    public static ShortestPathTree getShortestPathTreeBack(Graph graph, Vertex origin,
            Vertex target, State init, TraverseOptions options) {
        if (!options.isArriveBy()) {
            throw new RuntimeException("Reverse paths must call options.setArriveBy(true)");
        }
        if (origin == null || target == null) {
            return null;
        }

        options.setTransferTable(graph.getTransferTable());

        // Return Tree
        ShortestPathTree spt;
        if (options.getModes().getTransit()) {
            spt = new MultiShortestPathTree();
            options.setServiceDays(init.getTime());
        } else {
            spt = new BasicShortestPathTree();
        }

        /* Run backwards from the target to the origin */
        Vertex tmp = origin;
        origin = target;
        target = tmp;

        options = options.clone();
        /** max walk distance cannot be less than distances to nearest transit stops */
        options.maxWalkDistance += origin.getDistanceToNearestTransitStop()
                + target.getDistanceToNearestTransitStop();
        boolean limitWalkDistance = options.getModes().getTransit();

        /**
         * Populate any extra edges
         */
        final ExtraEdgesStrategy extraEdgesStrategy = options.extraEdgesStrategy;
        Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        extraEdgesStrategy.addIncomingEdgesForOrigin(extraEdges, origin);
        extraEdgesStrategy.addIncomingEdgesForTarget(extraEdges, target);
        if( extraEdges.isEmpty() )
            extraEdges = Collections.emptyMap();

        final RemainingWeightHeuristic heuristic = options.remainingWeightHeuristic;

        double initialWeight = heuristic.computeInitialWeight(origin, target, options);

        // double distance = origin.distance(target) / max_speed;
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        FibHeap<SPTVertex> pq = new FibHeap<SPTVertex>(graph.getVertices().size()
                + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + initialWeight);

        HashSet<Vertex> closed = new HashSet<Vertex>(100000);

        // Iteration Variables
        SPTVertex spt_u, spt_v;
        while (!pq.empty()) { // Until the priority queue is empty:
            spt_u = pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            // hard limit on walk distance. to be replaced with something more subtle later.
            State state_u = spt_u.state;
            StateData data_u = state_u.getData();
            if (limitWalkDistance &&
            	data_u.getWalkDistance() >= options.maxWalkDistance)
                continue;

            Vertex tov = spt_u.mirror;
            if (tov == target)
                break;

            closed.add(tov);

            Collection<Edge> incoming = GraphLibrary.getIncomingEdges(graph, tov, extraEdges);

            for (Edge edge : incoming) {

                if (edge instanceof PatternAlight && data_u.getNumBoardings() > options.maxTransfers) {
                    continue;
                }

                TraverseResult wr = edge.traverseBack(state_u, options);

                // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                // over.
                while (wr != null) {

                    if (wr.weight < 0) {
                        throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge "
                                + edge);
                    }

                    EdgeNarrative er = wr.getEdgeNarrative();
                    Vertex fromv = er.getFromVertex();

                    double new_w = spt_u.weightSum + wr.weight;
                    double remaining_w = heuristic.computeReverseWeight(spt_u, edge, wr, target);
                    double heuristic_distance = new_w + remaining_w;

                    if (heuristic_distance > options.maxWeight
                            || wr.state.getTime() < options.worstTime) {
                        // too expensive to get here
                    } else {
                        spt_v = spt.addVertex(fromv, wr.state, new_w, options);
                        if (spt_v != null) {
                            spt_v.setParent(spt_u, edge, er);
                            if (!closed.contains(fromv)) {
                                pq.insert_or_dec_key(spt_v, heuristic_distance);
                            }
                        }
                    }

                    // Iterate to next result
                    wr = wr.getNextResult();
                }
            }
        }
        return spt;
    }

    /**
     * Plots a path on graph from origin to target, DEPARTING at the time given in state and with
     * the options options.
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
        if (options.getModes().getTransit()) {
            spt = new MultiShortestPathTree();
            options.setServiceDays(init.getTime());
        } else {
            spt = new BasicShortestPathTree();
        }

        options.setTransferTable(graph.getTransferTable());

        /**
         * Populate any extra edges
         */
        final ExtraEdgesStrategy extraEdgesStrategy = options.extraEdgesStrategy;
        Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        extraEdgesStrategy.addOutgoingEdgesForOrigin(extraEdges, origin);
        extraEdgesStrategy.addOutgoingEdgesForTarget(extraEdges, target);
        if( extraEdges.isEmpty() )
            extraEdges = Collections.emptyMap();

        final RemainingWeightHeuristic heuristic = options.remainingWeightHeuristic;

        double initialWeight = heuristic.computeInitialWeight(origin, target, options);
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        FibHeap<SPTVertex> pq = new FibHeap<SPTVertex>(graph.getVertices().size()
                + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + initialWeight);

        options = options.clone();
        /** max walk distance cannot be less than distances to nearest transit stops */
        options.maxWalkDistance += origin.getDistanceToNearestTransitStop()
                + target.getDistanceToNearestTransitStop();

        boolean limitWalkDistance = options.getModes().getTransit();
        /* the core of the A* algorithm */
        while (!pq.empty()) { // Until the priority queue is empty:
            SPTVertex spt_u = pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            // hard limit on walk distance. to be replaced with something more subtle later.
            State state_u = spt_u.state;
            StateData data_u = state_u.getData();
            if (limitWalkDistance &&
               	data_u.getWalkDistance() >= options.maxWalkDistance)
                continue;

            Vertex fromv = spt_u.mirror;
            if (fromv == target) {
                break;
            }

            Collection<Edge> outgoing = GraphLibrary.getOutgoingEdges(graph, fromv, extraEdges);

            for (Edge edge : outgoing) {
                State state = spt_u.state;

                if (edge instanceof PatternBoard && data_u.getNumBoardings() > options.maxTransfers) {
                    continue;
                }

                TraverseResult wr = edge.traverse(state, options);

                // Iterate over traversal results. When an edge leads nowhere (as indicated by
                // returning NULL), the iteration is over.
                while (wr != null) {

                    if (wr.weight < 0) {
                        throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge " + edge);
                    }

                    EdgeNarrative er = wr.getEdgeNarrative();

                    Vertex tov = er.getToVertex();

                    double new_w = spt_u.weightSum + wr.weight;

                    double remaining_w = heuristic.computeForwardWeight(spt_u, edge, wr, target);

                    double heuristic_distance = new_w + remaining_w;

                    if (heuristic_distance > options.maxWeight
                            || wr.state.getTime() > options.worstTime) {
                        // too expensive to get here
                    } else {
                        SPTVertex spt_v = spt.addVertex(tov, wr.state, new_w, options);
                        if (spt_v != null) {
                            spt_v.setParent(spt_u, edge, er);
                            pq.insert_or_dec_key(spt_v, heuristic_distance);
                        }
                    }

                    // Iterate to next result
                    wr = wr.getNextResult();
                }
            }
        }

        return spt;
    }
}
