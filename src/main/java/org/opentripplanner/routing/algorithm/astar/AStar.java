package org.opentripplanner.routing.algorithm.astar;

import com.beust.jcommander.internal.Lists;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.astar.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.astar.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Find the shortest path between graph vertices using A*.
 * A basic Dijkstra search is a special case of AStar where the heuristic is always zero.
 *
 * NOTE this is now per-request scoped, which has caused some threading problems in the past.
 * Always make one new instance of this class per request, it contains a lot of state fields.
 */
public class AStar {

    private static final Logger LOG = LoggerFactory.getLogger(AStar.class);

    private boolean verbose = false;

    private TraverseVisitor traverseVisitor;

    enum RunStatus {
        RUNNING, STOPPED
    }

    private SkipEdgeStrategy skipEdgeStrategy;

    /* TODO instead of having a separate class for search state, we should just make one GenericAStar per request. */
    class RunState {

        public State u;
        public ShortestPathTree spt;
        BinHeap<State> pq;
        RemainingWeightHeuristic heuristic;
        public RoutingContext rctx;
        public int nVisited;
        public List<State> targetAcceptedStates;
        public RunStatus status;
        private RoutingRequest options;
        private SearchTerminationStrategy terminationStrategy;
        public Vertex u_vertex;

        public RunState(RoutingRequest options, SearchTerminationStrategy terminationStrategy) {
            this.options = options;
            this.terminationStrategy = terminationStrategy;
        }

    }
    
    private RunState runState;
    
    /**
     * Compute SPT using default timeout and termination strategy.
     */
    public ShortestPathTree getShortestPathTree(RoutingRequest req) {
        return getShortestPathTree(req, -1, null); // negative timeout means no timeout
    }
    
    /**
     * Compute SPT using default termination strategy.
     */
    public ShortestPathTree getShortestPathTree(RoutingRequest req, double relTimeoutSeconds) {
        return this.getShortestPathTree(req, relTimeoutSeconds, null);
    }
    
    /** set up a single-origin search */
    public void startSearch(RoutingRequest options,
            SearchTerminationStrategy terminationStrategy, long abortTime) {
        startSearch(options, terminationStrategy, abortTime, true);
    }
    
    /** set up the search, optionally not adding the initial state to the queue (for multi-state Dijkstra) */
    private void startSearch(RoutingRequest options,
            SearchTerminationStrategy terminationStrategy, long abortTime, boolean addToQueue) {

        runState = new RunState( options, terminationStrategy );
        runState.rctx = options.getRoutingContext();
        runState.spt = options.getNewShortestPathTree();

        // We want to reuse the heuristic instance in a series of requests for the same target to avoid repeated work.
        runState.heuristic = runState.rctx.remainingWeightHeuristic;

        // Since initial states can be multiple, heuristic cannot depend on the initial state.
        // Initializing the bidirectional heuristic is a pretty complicated operation that involves searching through
        // the streets around the origin and destination.
        runState.heuristic.initialize(runState.options, abortTime);
        if (abortTime < Long.MAX_VALUE  && System.currentTimeMillis() > abortTime) {
            LOG.warn("Timeout during initialization of goal direction heuristic.");
            runState = null; // Search timed out
            return;
        }

        // Priority Queue.
        // The queue is self-resizing, so we initialize it to have size = O(sqrt(|V|)) << |V|.
        // For reference, a random, undirected search on a uniform 2d grid will examine roughly sqrt(|V|) vertices
        // before reaching its target.
        int initialSize = runState.rctx.graph.getVertices().size();
        initialSize = (int) Math.ceil(2 * (Math.sqrt((double) initialSize + 1)));
        runState.pq = new BinHeap<>(initialSize);
        runState.nVisited = 0;
        runState.targetAcceptedStates = Lists.newArrayList();
        
        if (addToQueue) {
            for (State initialState : State.getInitialStates(options)) {
                runState.spt.add(initialState);
                runState.pq.insert(initialState, 0);
            }
        }
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

            if (skipEdgeStrategy != null &&
                    skipEdgeStrategy.shouldSkipEdge(
                        runState.rctx.fromVertices,
                        runState.rctx.toVertices,
                        runState.u,
                        edge,runState.spt,
                        runState.options
                    )
            ) {
                continue;
            }

            // Iterate over traversal results. When an edge leads nowhere (as indicated by
            // returning NULL), the iteration is over.
            for (State v = edge.traverse(runState.u); v != null; v = v.getNextResult()) {
                // Could be: for (State v : traverseEdge...)

                if (traverseVisitor != null) {
                    traverseVisitor.visitEdge(edge, v);
                }

                double remaining_w = runState.heuristic.estimateRemainingWeight(v);

//                LOG.info("{} {}", v, remaining_w);

                if (remaining_w < 0 || Double.isInfinite(remaining_w) ) {
                    continue;
                }
                double estimate = v.getWeight() + remaining_w;

                if (verbose) {
                    System.out.println("      edge " + edge);
                    System.out.println("      " + runState.u.getWeight() + " -> " + v.getWeight()
                            + "(w) + " + remaining_w + "(heur) = " + estimate + " vert = "
                            + v.getVertex());
                }
                
                // spt.add returns true if the state is hopeful; enqueue state if it's hopeful
                if (runState.spt.add(v)) {
                    // report to the visitor if there is one
                    if (traverseVisitor != null)
                        traverseVisitor.visitEnqueue(v);
                    //LOG.info("u.w={} v.w={} h={}", runState.u.weight, v.weight, remaining_w);
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
                LOG.warn("Search timeout. origin={} target={}", runState.rctx.fromVertices, runState.rctx.toVertices);
                // Rather than returning null to indicate that the search was aborted/timed out,
                // we instead set a flag in the routing context and return the SPT anyway. This
                // allows returning a partial list results even when a timeout occurs.
                runState.options.rctx.aborted = true; // signal search cancellation up to higher stack frames

                break;
            }
            
            /*
             * Get next best state and, if it hasn't already been dominated, add adjacent states to queue.
             * If it has been dominated, the iteration is over; don't bother checking for termination condition.
             * 
             * Note that termination is checked after adjacent states are added. This presents the negligible inefficiency
             * that adjacent states are generated for a state which could be the last one you need to check. The advantage
             * of this is that the algorithm is always left in a restartable state, which is useful for debugging or
             * potential future variations.
             */
            if(!iterate()){
                continue;
            }
            
            if (runState.terminationStrategy != null) {
                if (runState.terminationStrategy.shouldSearchTerminate(
                    runState.rctx.fromVertices, runState.rctx.toVertices, runState.u, runState.spt, runState.options)) {
                    break;
                }
            }
            if (runState.rctx.toVertices != null
                    && runState.rctx.toVertices.contains(runState.u_vertex)
                    && runState.u.isFinal()) {
                runState.targetAcceptedStates.add(runState.u);
                // new GraphPath(runState.u, false).dump();

                /* Break out of the search if we've found the requested number of paths. */
                // TODO Refactor. This check for getNumItineraries always returns 1
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
        
        return spt;
    }
    
    /** Get an SPT, starting from a collection of states */
    public ShortestPathTree getShortestPathTree(RoutingRequest options, double relTimeoutSeconds,
            SearchTerminationStrategy terminationStrategy, Collection<State> initialStates) {
        
        ShortestPathTree spt = null;
        long abortTime = DateUtils.absoluteTimeout(relTimeoutSeconds);

        startSearch (options, terminationStrategy, abortTime, false);
        
        if (runState != null) {
            for (State state : initialStates) {
                runState.spt.add(state);
                // TODO: hardwired for earliest arrival
                // TODO: weights are seconds, no?
                runState.pq.insert(state, state.getElapsedTimeSeconds());
            }
            
            runSearch(abortTime);
            spt = runState.spt;
        }
        
        return spt;
    }

    public void setTraverseVisitor(TraverseVisitor traverseVisitor) {
        this.traverseVisitor = traverseVisitor;
    }

    public List<GraphPath> getPathsToTarget() {
        if (runState == null) {
            return Collections.emptyList();
        }

        List<GraphPath> ret = new LinkedList<>();
        for (State s : runState.targetAcceptedStates) {
            if (s.isFinal()) {
                ret.add(new GraphPath(s));
            }
        }
        return ret;
    }

    public void setSkipEdgeStrategy(SkipEdgeStrategy skipEdgeStrategy) {
        this.skipEdgeStrategy = skipEdgeStrategy;
    }
}
