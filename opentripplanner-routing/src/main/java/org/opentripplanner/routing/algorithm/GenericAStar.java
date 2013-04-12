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

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.DefaultShortestPathTreeFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;
import org.opentripplanner.util.DateUtils;
import org.opentripplanner.util.monitoring.MonitoringStore;
import org.opentripplanner.util.monitoring.MonitoringStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Find the shortest path between graph vertices using A*.
 */
public class GenericAStar implements SPTService { // maybe this should be wrapped in a component SPT service 

    private static final Logger LOG = LoggerFactory.getLogger(GenericAStar.class);
    private static final MonitoringStore store = MonitoringStoreFactory.getStore();

    private boolean _verbose = false;

    private ShortestPathTreeFactory _shortestPathTreeFactory = new DefaultShortestPathTreeFactory();

    private SkipTraverseResultStrategy _skipTraversalResultStrategy;

    private SearchTerminationStrategy _searchTerminationStrategy;

    private TraverseVisitor traverseVisitor;

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
     * Compute SPT using default timeout and termination strategy.
     */
    @Override
    public ShortestPathTree getShortestPathTree(RoutingRequest req) {
        return getShortestPathTree(req, -1, _searchTerminationStrategy); // negative timeout means no timeout
    }
    
    /**
     * Compute SPT using default termination strategy.
     */
    @Override
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double timeoutSeconds) {
        return this.getShortestPathTree(req, timeoutSeconds, _searchTerminationStrategy);
    }

    /** @return the shortest path, or null if none is found */
    public ShortestPathTree getShortestPathTree(RoutingRequest options, double relTimeout,
            SearchTerminationStrategy terminationStrategy) {

        RoutingContext rctx = options.getRoutingContext();
        long abortTime = DateUtils.absoluteTimeout(relTimeout);

        // null checks on origin and destination vertices are already performed in setRoutingContext
        // options.rctx.check();
        
        ShortestPathTree spt = createShortestPathTree(options);

        final RemainingWeightHeuristic heuristic = options.batch ? 
                new TrivialRemainingWeightHeuristic() : rctx.remainingWeightHeuristic; 

        // heuristic calc could actually be done when states are constructed, inside state
        State initialState = new State(options);
        double initialWeight = heuristic.computeInitialWeight(initialState, rctx.target);
        spt.add(initialState);

        // Priority Queue.
        // NOTE(flamholz): the queue is self-resizing, so we initialize it to have 
        // size = O(sqrt(|V|)) << |V|. For reference, a random, undirected search
        // on a uniform 2d grid will examine roughly sqrt(|V|) vertices before
        // reaching its target. 
        OTPPriorityQueueFactory qFactory = BinHeap.FACTORY;
        int initialSize = rctx.graph.getVertices().size();
        initialSize = (int) Math.ceil(2 * (Math.sqrt((double) initialSize + 1)));
        OTPPriorityQueue<State> pq = qFactory.create(initialSize);
        // this would allow continuing a search from an existing state
        pq.insert(initialState, initialWeight);

//        options = options.clone();
//        /** max walk distance cannot be less than distances to nearest transit stops */
//        double minWalkDistance = origin.getVertex().getDistanceToNearestTransitStop()
//                + target.getDistanceToNearestTransitStop();
//        options.setMaxWalkDistance(Math.max(options.getMaxWalkDistance(), rctx.getMinWalkDistance()));

        int nVisited = 0;

        /* the core of the A* algorithm */
        while (!pq.empty()) { // Until the priority queue is empty:
            if (_verbose) {
                double w = pq.peek_min_key();
                System.out.println("pq min key = " + w);
            }

            /**
             * Terminate the search prematurely if we've hit our computation wall.
             */
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                LOG.warn("Search timeout. origin={} target={}", rctx.origin, rctx.target);
                // Returning null indicates something went wrong and search should be aborted.
                // This is distinct from the empty list of paths which implies that a result may still
                // be found by retrying with altered options (e.g. max walk distance)
                storeMemory();
                return null; // throw timeout exception
            }

            // get the lowest-weight state in the queue
            State u = pq.extract_min();
            
            // check that this state has not been dominated
            // and mark vertex as visited
            if (!spt.visit(u)) {
                // state has been dominated since it was added to the priority queue, so it is
                // not in any optimal path. drop it on the floor and try the next one.
                continue;  
            }

            if (traverseVisitor != null) {
                traverseVisitor.visitVertex(u);
            }

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
            if (terminationStrategy != null) {
                if (!terminationStrategy.shouldSearchContinue(
                    rctx.origin, rctx.target, u, spt, options))
                    break;
            // TODO AMB: Replace isFinal with bicycle conditions in BasicPathParser
            } else if (!options.batch && u_vertex == rctx.target && u.isFinal() && u.allPathParsersAccept()) {
                LOG.debug("total vertices visited {}", nVisited);
                storeMemory();
                return spt;
            }

            Collection<Edge> edges = options.isArriveBy() ? u_vertex.getIncoming() : u_vertex.getOutgoing();

            nVisited += 1;

            for (Edge edge : edges) {

                // Iterate over traversal results. When an edge leads nowhere (as indicated by
                // returning NULL), the iteration is over.
                for (State v = edge.traverse(u); v != null; v = v.getNextResult()) {
                    // Could be: for (State v : traverseEdge...)

                    if (traverseVisitor != null) {
                        traverseVisitor.visitEdge(edge, v);
                    }
                    // TEST: uncomment to verify that all optimisticTraverse functions are actually
                    // admissible
                    // State lbs = edge.optimisticTraverse(u);
                    // if ( ! (lbs.getWeight() <= v.getWeight())) {
                    // System.out.printf("inadmissible lower bound %f vs %f on edge %s\n",
                    // lbs.getWeightDelta(), v.getWeightDelta(), edge);
                    // }

                    if (_skipTraversalResultStrategy != null
                            && _skipTraversalResultStrategy.shouldSkipTraversalResult(
                                    rctx.origin, rctx.target, u, v, spt, options)) {
                        continue;
                    }

                    double remaining_w = computeRemainingWeight(heuristic, v, rctx.target, options);
                    if (remaining_w < 0 || Double.isInfinite(remaining_w) ) {
                        continue;
                    }
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
                            if (traverseVisitor != null)
                                traverseVisitor.visitEnqueue(v);
                            pq.insert(v, estimate);
                        } 
                    }
                }
            }
        }
        storeMemory();
        return spt;
    }

    private void storeMemory() {
        if (store.isMonitoring("memoryUsed")) {
            System.gc();
            long memoryUsed = Runtime.getRuntime().totalMemory() -
                    Runtime.getRuntime().freeMemory();
            store.setLongMax("memoryUsed", memoryUsed);
        }
    }

    private double computeRemainingWeight(final RemainingWeightHeuristic heuristic, State v,
            Vertex target, RoutingRequest options) {
        // actually, the heuristic could figure this out from the TraverseOptions.
        // set private member back=options.isArriveBy() on initial weight computation.
        if (options.isArriveBy()) {
            return heuristic.computeReverseWeight(v, target);
        } else {
            return heuristic.computeForwardWeight(v, target);
        }
    }

    private boolean isWorstTimeExceeded(State v, RoutingRequest opt) {
        if (opt.isArriveBy())
            return v.getTime() < opt.worstTime;
        else
            return v.getTime() > opt.worstTime;
    }

    private ShortestPathTree createShortestPathTree(RoutingRequest opts) {
        return _shortestPathTreeFactory.create(opts);
    }

    public void setTraverseVisitor(TraverseVisitor traverseVisitor) {
        this.traverseVisitor = traverseVisitor;
    }
}
