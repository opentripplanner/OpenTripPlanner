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
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.pqueue.OTPPriorityQueue;
import org.opentripplanner.routing.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.MultiShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find the shortest path between graph vertices using A*.
 */
public class GenericAStar {

    private static final Logger LOG = LoggerFactory.getLogger(GenericAStar.class);

    private boolean _verbose = false;

    private ShortestPathTreeFactory _shortestPathTreeFactory;

    private SkipTraverseResultStrategy _skipTraversalResultStrategy;

    private SearchTerminationStrategy _searchTerminationStrategy;

    public void setShortestPathTreeFactory(ShortestPathTreeFactory shortestPathTreeFactory) {
        _shortestPathTreeFactory = shortestPathTreeFactory;
    }

    public void setSkipTraverseResultStrategy(SkipTraverseResultStrategy skipTraversalResultStrategy) {
        _skipTraversalResultStrategy = skipTraversalResultStrategy;
    }

    public void setSearchTerminationStrategy(SearchTerminationStrategy searchTerminationStrategy) {
        _searchTerminationStrategy = searchTerminationStrategy;
    }

    /**
     * Plots a path on graph from origin to target. In the case of "arrive-by" routing, the origin
     * state is actually the user's end location and the target vertex is the user's start location.
     * 
     * @param graph
     * @param origin
     * @param target
     * @return the shortest path, or null if none is found
     */
    public ShortestPathTree getShortestPathTree(Graph graph, State origin, Vertex target) {

        if (origin == null || target == null) {
            return null;
        }

        TraverseOptions options = origin.getOptions();

        // from now on, target means "where this search will terminate"
        // not "the end of the trip from the user's perspective".

        ShortestPathTree spt = createShortestPathTree(origin, options);

        options.setTransferTable(graph.getTransferTable());

        /**
         * Populate any extra edges
         */
        final ExtraEdgesStrategy extraEdgesStrategy = options.extraEdgesStrategy;
        Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        // conditional could be eliminated by placing this before the o/d swap above
        if (options.isArriveBy()) {
            extraEdgesStrategy.addIncomingEdgesForOrigin(extraEdges, origin.getVertex());
            extraEdgesStrategy.addIncomingEdgesForTarget(extraEdges, target);
        } else {
            extraEdgesStrategy.addOutgoingEdgesForOrigin(extraEdges, origin.getVertex());
            extraEdgesStrategy.addOutgoingEdgesForTarget(extraEdges, target);
        }

        if (extraEdges.isEmpty())
            extraEdges = Collections.emptyMap();

        final RemainingWeightHeuristic heuristic = options.remainingWeightHeuristic;

        double initialWeight = heuristic.computeInitialWeight(origin, target);
        spt.add(origin);

        // Priority Queue
        OTPPriorityQueueFactory factory = BinHeap.FACTORY;
        OTPPriorityQueue<State> pq = factory.create(graph.getVertices().size() + extraEdges.size());
        // this would allow continuing a search from an existing state
        pq.insert(origin, origin.getWeight() + initialWeight);

        options = options.clone();
        /** max walk distance cannot be less than distances to nearest transit stops */
        double minWalkDistance = origin.getVertex().getDistanceToNearestTransitStop()
                + target.getDistanceToNearestTransitStop();
        options.setMaxWalkDistance(Math.max(options.getMaxWalkDistance(), minWalkDistance));

        long computationStartTime = System.currentTimeMillis();
        long maxComputationTime = options.maxComputationTime;

        boolean exit = false; // Unused?

        int nVisited = 0;

        /* the core of the A* algorithm */
        while (!pq.empty()) { // Until the priority queue is empty:

            if (exit)
                break; // unused?

            if (_verbose) {
                double w = pq.peek_min_key();
                System.out.println("pq min key = " + w);
            }

            /**
             * Terminate the search prematurely if we've hit our computation wall.
             */
            if (maxComputationTime > 0) {
                if ((System.currentTimeMillis() - computationStartTime) > maxComputationTime) {
                    break;
                }
            }

            // get the lowest-weight state in the queue
            State u = pq.extract_min();
            // check that this state has not been dominated
            // and mark vertex as visited
            if (!spt.visit(u))
                continue;
            Vertex u_vertex = u.getVertex();
            // Uncomment the following statement
            // to print out a CSV (actually semicolon-separated)
            // list of visited nodes for display in a GIS
            // System.out.println(u_vertex + ";" + u_vertex.getX() + ";" + u_vertex.getY() + ";" +
            // u.getWeight());

            if (_verbose)
                System.out.println("   vertex " + u_vertex);

            /**
             * Should we terminate the search?
             */
            if (_searchTerminationStrategy != null) {
                if (!_searchTerminationStrategy.shouldSearchContinue(origin.getVertex(), target, u,
                        spt, options))
                    break;
            } else if (u_vertex == target) {
                LOG.debug("total vertices visited {}", nVisited);
                return spt;
            }

            Collection<Edge> edges = getEdgesForVertex(graph, extraEdges, u_vertex, options);

            nVisited += 1;

            for (Edge edge : edges) {

                // Iterate over traversal results. When an edge leads nowhere (as indicated by
                // returning NULL), the iteration is over.
                for (State v = edge.traverse(u); v != null; v = v.getNextResult()) {
                    // Could be: for (State v : traverseEdge...)

                    // TEST: uncomment to verify that all optimisticTraverse functions are actually
                    // admissible
                    // State lbs = edge.optimisticTraverse(u);
                    // if ( ! (lbs.getWeight() <= v.getWeight())) {
                    // System.out.printf("inadmissible lower bound %f vs %f on edge %s\n",
                    // lbs.getWeightDelta(), v.getWeightDelta(), edge);
                    // }

                    if (_skipTraversalResultStrategy != null
                            && _skipTraversalResultStrategy.shouldSkipTraversalResult(
                                    origin.getVertex(), target, u, v, spt, options))
                        continue;

                    double remaining_w = computeRemainingWeight(heuristic, v, target, options);
                    double estimate = v.getWeight() + remaining_w;

                    if (_verbose) {
                        System.out.println("      edge " + edge);
                        System.out.println("      " + u.getWeight() + " -> " + v.getWeight()
                                + "(w) + " + remaining_w + "(heur) = " + estimate + " vert = "
                                + v.getVertex());
                    }

                    if (estimate > options.maxWeight) {
                        // too expensive to get here
                        if (_verbose)
                            System.out.println("         too expensive to reach, not enqueued. estimated weight = " + estimate);
                    } else if (isWorstTimeExceeded(v, options)) {
                        // too much time to get here
                    	if (_verbose)
                            System.out.println("         too much time to reach, not enqueued. time = " + v.getTime());
                    } else {
                        if (spt.add(v)) {
                            pq.insert(v, estimate);
                        } 
                    }
                }
            }
        }
        return spt;
    }

    private Collection<Edge> getEdgesForVertex(Graph graph, Map<Vertex, List<Edge>> extraEdges,
            Vertex vertex, TraverseOptions options) {

        if (options.isArriveBy())
            return GraphLibrary.getIncomingEdges(graph, vertex, extraEdges);
        else
            return GraphLibrary.getOutgoingEdges(graph, vertex, extraEdges);
    }

    private double computeRemainingWeight(final RemainingWeightHeuristic heuristic, State v,
            Vertex target, TraverseOptions options) {
        // actually, the heuristic could figure this out from the TraverseOptions.
        // set private member back=options.isArriveBy() on initial weight computation.
        if (options.isArriveBy())
            return heuristic.computeReverseWeight(v, target);
        else
            return heuristic.computeForwardWeight(v, target);
    }

    private boolean isWorstTimeExceeded(State v, TraverseOptions options) {
        if (options.isArriveBy())
            return v.getTime() < options.worstTime;
        else
            return v.getTime() > options.worstTime;
    }

    private ShortestPathTree createShortestPathTree(State init, TraverseOptions options) {

        // Return Tree
        ShortestPathTree spt = null;

        if (_shortestPathTreeFactory != null)
            spt = _shortestPathTreeFactory.create();

        if (spt == null) {
            if (options.getModes().getTransit()) {
                spt = new MultiShortestPathTree();
                // if (options.useServiceDays)
                options.setServiceDays(init.getTime());
            } else {
                spt = new BasicShortestPathTree();
            }
        }

        return spt;
    }
}
