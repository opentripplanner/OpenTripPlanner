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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipEdgeStrategy;
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
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;

/**
 * Find the shortest path between graph vertices using Dijkstra's algorithm.
 */
public class GenericDijkstra {

    private Graph graph;

    private TraverseOptions options;

    private ShortestPathTreeFactory _shortestPathTreeFactory;

    private OTPPriorityQueueFactory _priorityQueueFactory;

    private SearchTerminationStrategy _searchTerminationStrategy;

    private SkipEdgeStrategy _skipEdgeStrategy;

    private SkipTraverseResultStrategy _skipTraverseResultStrategy;

    private ExtraEdgesStrategy _extraEdgesStrategy;

    private boolean _verbose = false;

    /**
     */
    public GenericDijkstra(Graph graph, TraverseOptions options) {
        this.graph = graph;
        this.options = options;
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

    public void setExtraEdgesStrategy(ExtraEdgesStrategy extraEdgesStrategy) {
        _extraEdgesStrategy = extraEdgesStrategy;
    }

    public ShortestPathTree getShortestPathTree(State initialState) {

        ShortestPathTree spt = createShortestPathTree();
        OTPPriorityQueue<State> queue = createPriorityQueue();

        spt.add(initialState);
        queue.insert(initialState, initialState.getWeight());

        Map<Vertex, List<Edge>> extraEdges = null;
        if (_extraEdgesStrategy != null) {
            extraEdges = new HashMap<Vertex, List<Edge>>();
            _extraEdgesStrategy.addOutgoingEdgesForOrigin(extraEdges, initialState.getVertex());
        }

        while (!queue.empty()) { // Until the priority queue is empty:

            State u = queue.extract_min();
            Vertex fromv = u.getVertex();

            if (_verbose) {
                System.out.println("min," + u.getWeight());
                System.out.println(fromv);
            }

            if (_searchTerminationStrategy != null
                    && !_searchTerminationStrategy.shouldSearchContinue(initialState.getVertex(), 
                    null, u, spt, options))
                        break;

            Collection<Edge> outgoing = GraphLibrary.getOutgoingEdges(graph, fromv, extraEdges);

            for (Edge edge : outgoing) {

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
                        System.out.printf("  w = %d + %d = %d %s", u.getWeight(), v.getWeightDelta(), 
                        		v.getWeight(),  v.getVertex());
                    
                    if (v.exceedsWeightLimit(options.maxWeight))
                        continue;

                    if (spt.add(v))
                        queue.insert(v, v.getWeight());

                }
            }
        }
        return spt;
    }

    protected OTPPriorityQueue<State> createPriorityQueue() {
        if (_priorityQueueFactory != null)
            return _priorityQueueFactory.create(graph.getVertices().size());
        return new BinHeap<State>(graph.getVertices().size() / 2);
    }

    protected ShortestPathTree createShortestPathTree() {
        if (_shortestPathTreeFactory != null)
            return _shortestPathTreeFactory.create();
        return new BasicShortestPathTree();
    }
}
