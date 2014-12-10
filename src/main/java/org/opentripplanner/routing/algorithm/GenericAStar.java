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
import java.util.List;

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.*;
import org.opentripplanner.util.DateUtils;
import org.opentripplanner.util.monitoring.MonitoringStore;
import org.opentripplanner.util.monitoring.MonitoringStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Lists;

/**
 * Find the shortest path between graph vertices using A*.
 */
public class GenericAStar implements SPTService { // maybe this should be wrapped in a component SPT service 

    private static final Logger LOG = LoggerFactory.getLogger(GenericAStar.class);
    // FIXME this is not really a factory, it's a way to fake a global variable. This should be stored at the OTPServer level.
    private static final MonitoringStore store = MonitoringStoreFactory.getStore();
    private static final double OVERSEARCH_MULTIPLIER = 4.0;

    private boolean verbose = false;

    private TraverseVisitor traverseVisitor;

    enum RunStatus {
        RUNNING, STOPPED
    }

    /* TODO instead of having a separate class for search state, we should just make one GenericAStar per request. */
    class RunState {

        public State u;
        public ShortestPathTree spt;
        BinHeap<State> pq;
        RemainingWeightHeuristic heuristic;
        public RoutingContext rctx;
        public int nVisited;
        public List<Object> targetAcceptedStates;
        public RunStatus status;
        private RoutingRequest options;
        private SearchTerminationStrategy terminationStrategy;
        public Vertex u_vertex;
        Double foundPathWeight = null;

        public RunState(RoutingRequest options, SearchTerminationStrategy terminationStrategy) {
            this.options = options;
            this.terminationStrategy = terminationStrategy;
        }

    }
    
    private RunState runState;
    
    /**
     * Compute SPT using default timeout and termination strategy.
     */
    @Override
    public ShortestPathTree getShortestPathTree(RoutingRequest req) {
        return getShortestPathTree(req, -1, null); // negative timeout means no timeout
    }
    
    /**
     * Compute SPT using default termination strategy.
     */
    @Override
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double relTimeoutSeconds) {
        return this.getShortestPathTree(req, relTimeoutSeconds, null);
    }
    
    public void startSearch(RoutingRequest options,
            SearchTerminationStrategy terminationStrategy, long abortTime) {

        runState = new RunState( options, terminationStrategy );
        runState.rctx = options.getRoutingContext();
        // TODO this is a hackish way of communicating which mode we are in (since search mode is currently server-wide)
        runState.spt = options.longDistance ?
                new WeightOnlyShortestPathTree(runState.options) : new MultiShortestPathTree(runState.options);
        runState.heuristic = options.batch ?
                new TrivialRemainingWeightHeuristic() : runState.rctx.remainingWeightHeuristic;

        // Since initial states can be multiple, heuristic cannot depend on the initial state.
        runState.heuristic.initialize(runState.options, runState.rctx.origin, runState.rctx.target, abortTime);
        if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
            LOG.warn("Timeout during initialization of goal direction heuristic.");
            options.rctx.debugOutput.timedOut = true;
            runState = null; // Search timed out
            return;
        }
        State initialState = new State(options);
        runState.spt.add(initialState);

        // Priority Queue.
        // NOTE(flamholz): the queue is self-resizing, so we initialize it to have 
        // size = O(sqrt(|V|)) << |V|. For reference, a random, undirected search
        // on a uniform 2d grid will examine roughly sqrt(|V|) vertices before
        // reaching its target. 
        int initialSize = runState.rctx.graph.getVertices().size();
        initialSize = (int) Math.ceil(2 * (Math.sqrt((double) initialSize + 1)));
        runState.pq = new BinHeap<State>(initialSize);
        runState.pq.insert(initialState, 0);

//        options = options.clone();
//        /** max walk distance cannot be less than distances to nearest transit stops */
//        double minWalkDistance = origin.getVertex().getDistanceToNearestTransitStop()
//                + target.getDistanceToNearestTransitStop();
//        options.setMaxWalkDistance(Math.max(options.getMaxWalkDistance(), rctx.getMinWalkDistance()));

        runState.nVisited = 0;
        runState.targetAcceptedStates = Lists.newArrayList();

    }

    boolean iterate(){
        // print debug info
        if (verbose) {
            double w = runState.pq.peek_min_key();
            System.out.println("pq min key = " + w);
        }
        
        // interleave some heuristic-improving work (single threaded)
        runState.heuristic.doSomeWork();

        // get the lowest-weight state in the queue
        runState.u = runState.pq.extract_min();
        
        // check that this state has not been dominated
        // and mark vertex as visited
        if (!runState.spt.visit(runState.u)) {
            // state has been dominated since it was added to the priority queue, so it is
            // not in any optimal path. drop it on the floor and try the next one.
            return false;
        }
        
        if (traverseVisitor != null) {
            traverseVisitor.visitVertex(runState.u);
        }
        
        runState.u_vertex = runState.u.getVertex();

        if (verbose)
            System.out.println("   vertex " + runState.u_vertex);

        runState.nVisited += 1;
        
        Collection<Edge> edges = runState.options.arriveBy ? runState.u_vertex.getIncoming() : runState.u_vertex.getOutgoing();
        for (Edge edge : edges) {

            // Iterate over traversal results. When an edge leads nowhere (as indicated by
            // returning NULL), the iteration is over. TODO Use this to board multiple trips.
            for (State v = edge.traverse(runState.u); v != null; v = v.getNextResult()) {
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

                double remaining_w = computeRemainingWeight(runState.heuristic, v, runState.rctx.target, runState.options);
                if (remaining_w < 0 || Double.isInfinite(remaining_w) ) {
                    continue;
                }
                double estimate = v.getWeight() + remaining_w * runState.options.heuristicWeight;

                if (verbose) {
                    System.out.println("      edge " + edge);
                    System.out.println("      " + runState.u.getWeight() + " -> " + v.getWeight()
                            + "(w) + " + remaining_w + "(heur) = " + estimate + " vert = "
                            + v.getVertex());
                }

                // avoid enqueuing useless branches 
                if (estimate > runState.options.maxWeight) {
                    // too expensive to get here
                    if (verbose)
                        System.out.println("         too expensive to reach, not enqueued. estimated weight = " + estimate);
                    continue;
                }
                if (isWorstTimeExceeded(v, runState.options)) {
                    // too much time to get here
                    if (verbose)
                        System.out.println("         too much time to reach, not enqueued. time = " + v.getTimeSeconds());
                    continue;
                }
                
                // spt.add returns true if the state is hopeful; enqueue state if it's hopeful
                if (runState.spt.add(v)) {
                    // report to the visitor if there is one
                    if (traverseVisitor != null)
                        traverseVisitor.visitEnqueue(v);
                    
                    runState.pq.insert(v, estimate);
                } 
            }
        }
        
        return true;
    }
    
    void runSearch(long abortTime){
        /* the core of the A* algorithm */
        while (!runState.pq.empty()) { // Until the priority queue is empty:
            /*
             * Terminate based on timeout?
             */
            if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
                LOG.warn("Search timeout. origin={} target={}", runState.rctx.origin, runState.rctx.target);
                // Rather than returning null to indicate that the search was aborted/timed out,
                // we instead set a flag in the routing context and return the SPT anyway. This
                // allows returning a partial list results even when a timeout occurs.
                runState.options.rctx.aborted = true; // signal search cancellation up to higher stack frames
                runState.options.rctx.debugOutput.timedOut = true; // signal timeout in debug output object

                break;
            }
            
            /*
             * Get next best state and, if it hasn't already been dominated, add adjacent states to queue.
             * If it has been dominated, the iteration is over; don't bother checking for termination condition.
             * 
             * Note that termination is checked after adjacent states are added. This presents the small inefficiency
             * that adjacent states are generated for a state which could be the last one you need to check. The advantage
             * of this is that the algorithm is always left in a restartable state, which is useful for debugging or
             * potential future variations.
             */
            if(!iterate()){
                continue;
            }
            
            /*
             * Should we terminate the search?
             */
            // Don't search too far past the most recently found accepted path/state
            if (runState.foundPathWeight != null &&
                runState.u.getWeight() > runState.foundPathWeight * OVERSEARCH_MULTIPLIER ) {
                break;
            }
            if (runState.terminationStrategy != null) {
                if (runState.terminationStrategy.shouldSearchTerminate(
                    runState.rctx.origin, runState.rctx.target, runState.u, runState.spt, runState.options)) {
                    break;
                }
            // TODO AMB: Replace isFinal with bicycle conditions in BasicPathParser
            }  else if (!runState.options.batch && runState.u_vertex == runState.rctx.target && runState.u.isFinal() && runState.u.allPathParsersAccept()) {
                runState.targetAcceptedStates.add(runState.u);
                runState.foundPathWeight = runState.u.getWeight();
                runState.options.rctx.debugOutput.foundPath();
                //new GraphPath(runState.u, false).dump();
                /* Only find one path at a time in long distance mode. */
                if (runState.options.longDistance) break;
                /* Break out of the search if we've found the requested number of paths. */
                if (runState.targetAcceptedStates.size() >= runState.options.getNumItineraries()) {
                    LOG.debug("total vertices visited {}", runState.nVisited);
                    break;
                }
            }

        }
    }

    /** @return the shortest path, or null if none is found */
    public ShortestPathTree getShortestPathTree(RoutingRequest options, double relTimeoutSeconds,
            SearchTerminationStrategy terminationStrategy) {
        ShortestPathTree spt = null;
        long abortTime = DateUtils.absoluteTimeout(relTimeoutSeconds);

        startSearch (options, terminationStrategy, abortTime);

        if (runState != null) {
            runSearch(abortTime);
            spt = runState.spt;
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
        if (options.arriveBy) {
            return heuristic.computeReverseWeight(v, target);
        } else {
            return heuristic.computeForwardWeight(v, target);
        }
    }

    private boolean isWorstTimeExceeded(State v, RoutingRequest opt) {
        if (opt.arriveBy)
            return v.getTimeSeconds() < opt.worstTime;
        else
            return v.getTimeSeconds() > opt.worstTime;
    }

    public void setTraverseVisitor(TraverseVisitor traverseVisitor) {
        this.traverseVisitor = traverseVisitor;
    }
}
