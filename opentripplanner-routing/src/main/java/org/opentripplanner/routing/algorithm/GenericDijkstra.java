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

import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;

/**
 * Find the shortest path between graph vertices using Dijkstra's algorithm.
 */
public class GenericDijkstra {

    private OverlayGraph replacementEdges;

    private RoutingRequest options;

    private ShortestPathTreeFactory _shortestPathTreeFactory;

    private OTPPriorityQueueFactory _priorityQueueFactory;

    private SearchTerminationStrategy _searchTerminationStrategy;

    private SkipEdgeStrategy _skipEdgeStrategy;

    private SkipTraverseResultStrategy _skipTraverseResultStrategy;

    private boolean _verbose = false;

    private RemainingWeightHeuristic heuristic = new TrivialRemainingWeightHeuristic();

    public GenericDijkstra(RoutingRequest options) {
        this.options = options;
    }

    public GenericDijkstra(RoutingRequest options, OverlayGraph replacementEdges) {
        this.options = options;
        this.replacementEdges = replacementEdges;
    }

    public void setShortestPathTreeFactory(ShortestPathTreeFactory shortestPathTreeFactory) {
        _shortestPathTreeFactory = shortestPathTreeFactory;
    }

    public void setPriorityQueueFactory(OTPPriorityQueueFactory priorityQueueFactory) {
        _priorityQueueFactory = priorityQueueFactory;
    }

    public void setSearchTerminationStrategy(SearchTerminationStrategy searchTerminationStrategy) {
        _searchTerminationStrategy = searchTerminationStrategy;
    }

    public void setSkipEdgeStrategy(SkipEdgeStrategy skipEdgeStrategy) {
        _skipEdgeStrategy = skipEdgeStrategy;
    }

    public void setSkipTraverseResultStrategy(SkipTraverseResultStrategy skipTraverseResultStrategy) {
        _skipTraverseResultStrategy = skipTraverseResultStrategy;
    }

    public ShortestPathTree getShortestPathTree(State initialState) {
        Vertex target = null;
        if (options.rctx != null) {
            target = initialState.getOptions().rctx.target;
        }
        ShortestPathTree spt = createShortestPathTree(options);
        OTPPriorityQueue<State> queue = createPriorityQueue();

        spt.add(initialState);
        queue.insert(initialState, initialState.getWeight());

        while (!queue.empty()) { // Until the priority queue is empty:
            State u = queue.extract_min();
            Vertex u_vertex = u.getVertex();

            if (!spt.getStates(u_vertex).contains(u)) {
                continue;
            }

            if (_verbose) {
                System.out.println("min," + u.getWeight());
                System.out.println(u_vertex);
            }

            if (_searchTerminationStrategy != null
                    && !_searchTerminationStrategy.shouldSearchContinue(initialState.getVertex(), 
                    null, u, spt, options))
                        break;

            for (Edge edge : options.isArriveBy() ? u_vertex.getIncoming() : u_vertex.getOutgoing()) {

                if (_skipEdgeStrategy != null
                        && _skipEdgeStrategy.shouldSkipEdge(initialState.getVertex(), null, u, edge, spt,
                                options))
                    continue;

                // Iterate over traversal results. When an edge leads nowhere (as indicated by
                // returning NULL), the iteration is over.
                for (State v = edge.traverse(u); v != null; v = v.getNextResult()) {

                    if (_skipTraverseResultStrategy != null
                            && _skipTraverseResultStrategy.shouldSkipTraversalResult(initialState.getVertex(),
                                    null, u, v, spt, options))
                        continue;

                    if (_verbose)
                        System.out.printf("  w = %f + %f = %f %s", u.getWeight(), v.getWeightDelta(), 
                        		v.getWeight(),  v.getVertex());
                    
                    if (v.exceedsWeightLimit(options.maxWeight))
                        continue;

                    if (spt.add(v)) {
                        double estimate = heuristic.computeForwardWeight(v, target);
                        queue.insert(v, v.getWeight() + estimate);
                    }

                }
            }
            spt.postVisit(u);
        }
        return spt;
    }

    protected OTPPriorityQueue<State> createPriorityQueue() {
        if (_priorityQueueFactory != null)
            return _priorityQueueFactory.create(10);
        return new BinHeap<State>();
    }

    protected ShortestPathTree createShortestPathTree(RoutingRequest options) {
        if (_shortestPathTreeFactory != null)
            return _shortestPathTreeFactory.create(options);
        return new BasicShortestPathTree(options);
    }

    public void setHeuristic(RemainingWeightHeuristic heuristic) {
        this.heuristic = heuristic;
    }
}
