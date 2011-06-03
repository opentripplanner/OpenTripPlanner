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
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.pqueue.OTPPriorityQueue;
import org.opentripplanner.routing.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.pqueue.PriorityQueueImpl;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.MultiShortestPathTree;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;

/**
 * Find the shortest path between graph vertices using A*.
 */
public class GenericAStar {

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
     * Convenience method that swaps the origin and target for the actual search
     * @param graph
     * @param origin
     * @param target
     * @param init
     * @param options
     * @return
     */
    public ShortestPathTree getShortestPathTreeBack(Graph graph, Vertex origin,
            Vertex target, State init, TraverseOptions options) {
        
        if (!options.isArriveBy()) {
            throw new RuntimeException("Reverse paths must call options.setArriveBy(true)");
        }
        return getShortestPathTree(graph, target, origin, init, options);
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
    public ShortestPathTree getShortestPathTree(Graph graph, Vertex origin, Vertex target,
            State init, TraverseOptions options) {

        if (origin == null || target == null) {
            return null;
        }

        ShortestPathTree spt = createShortestPathTree(init, options);

        options.setTransferTable(graph.getTransferTable());

        /**
         * Populate any extra edges
         */
        final ExtraEdgesStrategy extraEdgesStrategy = options.extraEdgesStrategy;
        Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        if (options.isArriveBy()) {
            extraEdgesStrategy.addIncomingEdgesForOrigin(extraEdges, origin);
            extraEdgesStrategy.addIncomingEdgesForTarget(extraEdges, target);
        } else {
            extraEdgesStrategy.addOutgoingEdgesForOrigin(extraEdges, origin);
            extraEdgesStrategy.addOutgoingEdgesForTarget(extraEdges, target);
        }

        if (extraEdges.isEmpty())
            extraEdges = Collections.emptyMap();

        final RemainingWeightHeuristic heuristic = options.remainingWeightHeuristic;

        double initialWeight = heuristic.computeInitialWeight(origin, target, options);
        SPTVertex spt_origin = spt.addVertex(origin, init, 0, options);

        // Priority Queue
        OTPPriorityQueueFactory factory = PriorityQueueImpl.FACTORY;
        OTPPriorityQueue<SPTVertex> pq = factory.create(graph.getVertices().size()
                + extraEdges.size());
        pq.insert(spt_origin, spt_origin.weightSum + initialWeight);

        options = options.clone();
        /** max walk distance cannot be less than distances to nearest transit stops */
        options.maxWalkDistance += origin.getDistanceToNearestTransitStop()
                + target.getDistanceToNearestTransitStop();

        long computationStartTime = System.currentTimeMillis();
        long maxComputationTime = options.maxComputationTime;

        boolean exit = false;

        /* the core of the A* algorithm */
        while (!pq.empty()) { // Until the priority queue is empty:

            if (exit)
                break;

            if (_verbose) {
                double w = pq.peek_min_key();
                System.out.println("min," + w);
            }

            /**
             * Terminate the search prematurely if we've hit our computation wall.
             */
            if (maxComputationTime > 0) {
                if ((System.currentTimeMillis() - computationStartTime) > maxComputationTime) {
                    break;
                }
            }

            SPTVertex spt_u = pq.extract_min(); // get the lowest-weightSum Vertex 'u',

            // hard limit on walk distance. to be replaced with something more subtle later.
            State state_u = spt_u.state;
            StateData data_u = state_u.getData();

            Vertex fromv = spt_u.mirror;

            if (_verbose)
                System.out.println(fromv);

            /**
             * Should we terminate the search?
             */
            if (_searchTerminationStrategy != null) {
                if (!_searchTerminationStrategy.shouldSearchContinue(origin, target, spt_u, spt,
                        options))
                    break;
            } else if (fromv == target) {
                break;
            }

            Collection<Edge> edges = getEdgesForVertex(graph, extraEdges, fromv, options);

            for (Edge edge : edges) {

                State state = spt_u.state;

                if (edge instanceof PatternBoard && data_u.getNumBoardings() > options.maxTransfers) {
                    continue;
                }

                // Iterate over traversal results. When an edge leads nowhere (as indicated by
                // returning NULL), the iteration is over.
                for (TraverseResult wr = traversEdge(edge, state, options); wr != null; wr = wr
                        .getNextResult()) {

                    if (wr.weight < 0) {
                        throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge "
                                + edge);
                    }
                    
                    if( _skipTraversalResultStrategy != null && _skipTraversalResultStrategy.shouldSkipTraversalResult(origin, target, spt_u, wr, spt, options))
                        continue;

                    EdgeNarrative er = wr.getEdgeNarrative();

                    Vertex tov = options.isArriveBy() ? er.getFromVertex() : er.getToVertex();

                    double new_w = spt_u.weightSum + wr.weight;
                    double remaining_w = computeRemainingWeight(heuristic, spt_u, edge, wr, target,
                            options);
                    double heuristic_distance = new_w + remaining_w;

                    if (_verbose) {
                        System.out.println("  w=" + spt_u.weightSum + "+" + wr.weight + "+"
                                + remaining_w + "=" + heuristic_distance + " " + tov);
                    }

                    if (heuristic_distance > options.maxWeight || isWorstTimeExceeded(wr, options)) {
                        // too expensive to get here
                    } else {
                        SPTVertex spt_v = spt.addVertex(tov, wr.state, new_w, options);
                        if (spt_v != null) {
                            spt_v.setParent(spt_u, edge, er);
                            pq.insert_or_dec_key(spt_v, heuristic_distance);
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

    private TraverseResult traversEdge(Edge edge, State state, TraverseOptions options) {
    	List<Patch> patches = edge.getPatches();
        if (options.isArriveBy()) {

            TraverseResult result = edge.traverseBack(state, options);
            if (patches == null) {
            	return result;
            }
            for (Patch patch : patches) {
            	if (!patch.activeDuring(state.getStartTime(), state.getTime())) {
            		continue;
            	}
                if (result != null) {
                	result = patch.filterTraverseResults(result);
                }
            	TraverseResult patchResult = patch.addTraverseResultBack(edge, state, options);
            	if (patchResult != null) {
            		if (result != null) {
            			patchResult.addToExistingResultChain(result);
            		} else {
            			result = patchResult;
            		}
            	}
            }
            return result;
        } else {
        	TraverseResult result = edge.traverse(state, options);
            if (patches == null) {
            	return result;
            }
        	for (Patch patch : patches) {
        		if (!patch.activeDuring(state.getStartTime(), state.getTime())) {
            		continue;
            	}
                if (result != null) {
                	result = patch.filterTraverseResults(result);
                }
            	TraverseResult patchResult = patch.addTraverseResult(edge, state, options);
            	if (patchResult != null) {
            		if (result != null) {
            			patchResult.addToExistingResultChain(result);
            		} else {
            			result = patchResult;
            		}
            	}
            }
        	return result;
        }
    }

    private double computeRemainingWeight(final RemainingWeightHeuristic heuristic,
            SPTVertex spt_u, Edge edge, TraverseResult wr, Vertex target, TraverseOptions options) {
        if (options.isArriveBy())
            return heuristic.computeReverseWeight(spt_u, edge, wr, target);
        else
            return heuristic.computeForwardWeight(spt_u, edge, wr, target);
    }

    private boolean isWorstTimeExceeded(TraverseResult wr, TraverseOptions options) {
        if (options.isArriveBy())
            return wr.state.getTime() < options.worstTime;
        else
            return wr.state.getTime() > options.worstTime;
    }

    private ShortestPathTree createShortestPathTree(State init, TraverseOptions options) {

        // Return Tree
        ShortestPathTree spt = null;

        if (_shortestPathTreeFactory != null)
            spt = _shortestPathTreeFactory.create();

        if (spt == null) {
            if (options.getModes().getTransit()) {
                spt = new MultiShortestPathTree();
                //if (options.useServiceDays)
                    options.setServiceDays(init.getTime());
            } else {
                spt = new BasicShortestPathTree();
            }
        }

        return spt;
    }
}
