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
package org.opentripplanner.routing.contraction;

import static org.opentripplanner.common.IterableLibrary.filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.edgetype.DirectEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.util.NullExtraEdges;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements roughly the algorithm described in
 * "Contraction Hierarchies: Faster and Simpler Hierarchical Routing in Road Networks"
 * http://algo2.iti.kit.edu/1087.php
 * 
 * Some code initially based on Brandon Martin-Anderson's implementation in Graphserver.
 * 
 */
public class ContractionHierarchy implements Serializable {

    /* contraction parameters */
    private static final int HOP_LIMIT_SIMULATE = 5; //10;
    private static final int HOP_LIMIT_CONTRACT = 5; //Integer.MAX_VALUE;
    private static final int NODE_LIMIT_SIMULATE = 500;
    private static final int NODE_LIMIT_CONTRACT = 500; //Integer.MAX_VALUE;

    private static final Logger _log = LoggerFactory.getLogger(ContractionHierarchy.class);

    private static final long serialVersionUID = 20111118L;

    public OverlayGraph core;

    public OverlayGraph updown;  // outgoing is up, incoming is down

    private double contractionFactor;

    private transient TraverseOptions fwdOptions, backOptions;

    private transient TraverseMode mode;

    private transient ThreadPoolExecutor threadPool;


    /**
     * Returns the set of shortcuts around a vertex, as well as the size of the space searched.
     * 
     * @param u - the node around which to find shortcuts
     * @param hopLimit - maximum length of witness search paths, in number of edges traversed
     * @param simulate - If true, use different hop and vertex visit limits, and do not remove any edges.
     *                   This is used for establishing the node order rather than actually contracting. 
     * 
     * @return - the necessary shortcuts and the search space of the witness search.
     */
    public WitnessSearchResult getShortcuts(Vertex u, boolean simulate) {

        /* Compute the cost from each vertex with an incoming edge to the target */
        State su = new State(u, backOptions); // search backward
        int searchSpace = 0;
        
        ArrayList<State> vs = new ArrayList<State>();
        for (Edge e : core.getIncoming(u)) {
            if (!isContractable(e)) {
                continue;
            }
            State sv = e.traverse(su);
            if (sv == null) {
                continue;
            }
            vs.add(sv);
        }

        /* Compute the cost to each vertex with an outgoing edge from the target */
        su = new State(u, fwdOptions); // search forward
        double maxWWeight = 0;

        ArrayList<State> ws = new ArrayList<State>();
        for (DirectEdge e : filter(core.getOutgoing(u), DirectEdge.class)) {
            if (!isContractable(e)) {
                continue;
            }
            State sw = e.traverse(su);
            if (sw == null) {
                continue;
            }
            ws.add(sw);
            if (sw.exceedsWeightLimit(maxWWeight)) {
                maxWWeight = sw.getWeight();
            }
        }

        /* figure out which shortcuts are needed */
        List<Shortcut> shortcuts = new ArrayList<Shortcut>();

        ArrayList<Callable<WitnessSearchResult>> tasks = 
                new ArrayList<Callable<WitnessSearchResult>>(vs.size());
        
        int nodeLimit = simulate ? NODE_LIMIT_SIMULATE : NODE_LIMIT_CONTRACT;
        int hopLimit = simulate ? HOP_LIMIT_SIMULATE : HOP_LIMIT_CONTRACT;

        // FOR EACH V
        for (State v : vs) {
            //allow about a second of inefficiency in routes in the name of planning
            //efficiency (+ 1)
            double weightLimit = v.getWeight() + maxWWeight + 1;
            WitnessSearch task = new WitnessSearch(u, hopLimit, nodeLimit, 
                    weightLimit, ws, v);
            tasks.add(task);
        }
        if (threadPool == null) {
            createThreadPool();
        }
        try {
            for (Future<WitnessSearchResult> future : threadPool.invokeAll(tasks)) {
                WitnessSearchResult wsresult = future.get();
                BasicShortestPathTree spt = wsresult.spt;
                if (!simulate && spt != null) {
                    /* while we're here, remove some non-optimal edges */
                    ArrayList<DirectEdge> toRemove = new ArrayList<DirectEdge>();
                    // starting from each v
                    State sv0 = new State(wsresult.vertex, fwdOptions);
                    for (DirectEdge e : filter(core.getOutgoing(wsresult.vertex), DirectEdge.class)) {
                        State sSpt = spt.getState(e.getToVertex());
                        if (sSpt == null) {
                            continue;
                        }
                        State sv1 = e.traverse(sv0);
                        if (sv1 == null) {
                            toRemove.add(e);
                            continue;
                        }
                        if (sSpt.getWeight() < sv1.getWeight()) {
                            // the path found by Dijkstra from u to e.tov is better
                            // than the path through e. Therefore e can be deleted.
                            toRemove.add(e);
                        }
                    }

                    Vertex uv = wsresult.vertex;
                    for (DirectEdge e : toRemove) {
                        uv.removeOutgoing(e);
                        e.getToVertex().removeIncoming(e);
                    }
                }
                searchSpace += wsresult.searchSpace;
                shortcuts.addAll(wsresult.shortcuts);
            }
        } catch (InterruptedException e1) {
            throw new RuntimeException(e1);
        } catch (ExecutionException e1) {
            throw new RuntimeException(e1);
        }
        return new WitnessSearchResult(shortcuts, null, null, searchSpace);
    }

    private class WitnessSearch implements Callable<WitnessSearchResult> {

    	private Vertex u;
        private double weightLimit;
        private int hopLimit;
        private List<State> ws;
        private State v;
        private int nodeLimit;

        public WitnessSearch(Vertex u, int hopLimit, int nodeLimit,
                double weightLimit, List<State> ws, State v) {
            this.u = u;
            this.hopLimit = hopLimit;
            this.nodeLimit = nodeLimit;
            this.weightLimit = weightLimit;
            this.ws = ws;
            this.v = v;
        }

        public WitnessSearchResult call() {
            return searchWitnesses(u, hopLimit, nodeLimit, weightLimit, ws, v);
        }
    }

    private WitnessSearchResult searchWitnesses(Vertex u, int hopLimit, 
    		int nodeLimit, double weightLimit, List<State> ws, State v) {

    	Dijkstra dijkstra = new Dijkstra(core, v.getVertex(), fwdOptions, u, hopLimit);
        dijkstra.setTargets(ws); 
        BasicShortestPathTree spt = dijkstra.getShortestPathTree(weightLimit, nodeLimit);

        ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();

        // FOR EACH W
        for (State w : ws) {

            if (v.getVertex() == w.getVertex()) {
                continue;
            }

            double weightThroughU = v.getWeight() + w.getWeight();
            State pathAroundU = spt.getState(w.getVertex());

            // if the path around the vertex is longer than the path through,
            // add the path through to the shortcuts
            if (pathAroundU == null || pathAroundU.exceedsWeightLimit(weightThroughU + .01)) {
                int timeThroughU = (int) (w.getAbsTimeDeltaSec() + v.getAbsTimeDeltaSec());
                double walkDistance = v.getWalkDistance() + w.getWalkDistance();
                Shortcut vuw = new Shortcut((DirectEdge)v.getBackEdge(), (DirectEdge)w.getBackEdge(), 
                        timeThroughU, weightThroughU, walkDistance, mode);
                shortcuts.add(vuw);
            }
        }
        
        return new WitnessSearchResult(shortcuts, spt, v.getVertex(), spt.getVertexCount());
    }

    /**
     * Determine whether an edge can be contracted. Presently, we only contract non-time-dependent
     * edges.
     */
    public static boolean isContractable(Edge edge) {
        return (edge instanceof TurnEdge || edge instanceof OutEdge  || 
        		edge instanceof FreeEdge || edge instanceof Shortcut || 
        		edge instanceof PlainStreetEdge);
    }

    /**
     * Returns the importance of a vertex from the edge difference, number of deleted neighbors, and
     * search space size ("EDS").
     */
    private int getImportance(Vertex v, WitnessSearchResult shortcutsAndSearchSpace,
            int deletedNeighbors) {
        int degree_in = core.getDegreeIn(v);
        int degree_out = core.getDegreeOut(v);
        int edge_difference = shortcutsAndSearchSpace.shortcuts.size() - (degree_in + degree_out);
        int searchSpace = shortcutsAndSearchSpace.searchSpace;

        return edge_difference * 190 + deletedNeighbors * 120 + searchSpace;
    }

    /**
     * Initialize the priority queue for contracting, by computing the initial importance of every
     * node
     * 
     * @return
     */
    BinHeap<Vertex> initPriorityQueue(OverlayGraph og) {
        Collection<Vertex> vertices = og.getVertices();
        BinHeap<Vertex> pq = new BinHeap<Vertex>(vertices.size());

        int i = 0;
        for (Vertex v : vertices) {
        	if (++i % 100000 == 0)
        		_log.debug("    vertex {}", i);
                //System.out.print(" vertex " + v);
            if (!isContractable(v)) {
                //System.out.println(" not contractable");
                continue;
            }
            if (og.getDegreeIn(v) > 7 || og.getDegreeOut(v) > 7) {
                //System.out.println(" degree too high");
                continue;
            }
            WitnessSearchResult shortcuts = getShortcuts(v, true);
            int imp = getImportance(v, shortcuts, 0);
            //System.out.println(" contractable " + imp);
            pq.insert(v, imp);
        }
        return pq;
    }

    /**
     * Transit nodes are not (yet) able to be contracted. So this returns true only for street
     * vertices which have no edges to non-street vertices
     * 
     * @return
     */
    private boolean isContractable(Vertex v) {
        if (v instanceof TurnVertex || v instanceof IntersectionVertex) {
            for (Edge e : core.getOutgoing(v)) {
                if( ! (e instanceof DirectEdge))
                    return false;
                DirectEdge de = (DirectEdge) e;
                Vertex tov = de.getToVertex();
                if (!(tov instanceof TurnVertex || tov instanceof IntersectionVertex)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Create a contraction hierarchy from a graph.
     * 
     * @param orig
     * @param options
     * @param contractionFactor
     *            A fraction from 0 to 1 of (the contractable portion of) the graph to contract
     */
    public ContractionHierarchy(Graph orig, TraverseOptions options, double contractionFactor) {
        
        core = new OverlayGraph(orig);

        this.fwdOptions = options;
        this.fwdOptions.setMaxWalkDistance(Double.MAX_VALUE);
        backOptions = this.fwdOptions.clone();
        backOptions.setArriveBy(true);
        this.contractionFactor = contractionFactor;

        this.mode = this.fwdOptions.getModes().getNonTransitMode();

        init();
        //useCoreVerticesFrosm(orig);
    }

// duplicates the graphvertices from the original graph rather than duplicating them
//    private void useCoreVerticesFrom(Graph orig) {
//        for (Vertex gv : graph.getVertices()) {
//            Vertex origgv = orig.getVertex(gv.vertex.getLabel());
//            if (origgv.equals(gv)) {
//                graph.addVertex(origgv);
//            }
//        }
//    }

    void init() {

        createThreadPool();

        updown = new OverlayGraph();

        long start = System.currentTimeMillis();

        HashMap<Vertex, Integer> deletedNeighbors = new HashMap<Vertex, Integer>();
        _log.debug("Preparing contraction hierarchy -- this may take quite a while");
        _log.debug("Initializing priority queue");

        BinHeap<Vertex> pq = initPriorityQueue(core);

        _log.debug("Contracting");
        long lastNotified = System.currentTimeMillis();
        int i = 0;
        int nContractableVertices, totalContractableVertices;
        totalContractableVertices = nContractableVertices = pq.size();
        int nContractableEdges = countContractableEdges(core);
        boolean edgesRemoved = false;

        while (!pq.empty()) {
            // stop contracting once a core is reached
            if (i > Math.ceil(totalContractableVertices * contractionFactor)) {
                break;
            }
            i += 1;

/*         "Keeping the cost of contraction up-to-date in the priority queue is quite costly. The contraction of a 
 			node in a search tree of the local search can aﬀect the cost of contraction. So after
            the contraction of a node w, it would be necessary to recalculate the priority of each node that
            has w in their local search spaces. Most local search spaces are small but there are exceptions.
            16If there is an edge with a large edge weight, e.g. a long-distance ferry connection, the search
            space can grow arbitrarily and with it the number of nodes that trigger a change in the cost of
            contraction. A simpliﬁed example can be found in Figure 6.
            In our implementation we will omit exakt updates for the sake of performance, update only
            the neighbors of the contracted node and eventually use lazy updates to cope with drastic
            increases of search space sizes." Geisberger (2008)
*/
            
//            if (potentialLazyUpdates > 30 && lazyUpdates > 28) {
//            	rebuildPriorityQueue(hopLimit, deletedNeighbors);
//                potentialLazyUpdates = 0;
//                lazyUpdates = 0;
//            }
            
            Vertex vertex = pq.extract_min();
            _log.trace("contracting vertex " + vertex);
            WitnessSearchResult shortcutsAndSearchSpace;
            // make sure priority of current vertex
            if (pq.empty()) {
                shortcutsAndSearchSpace = getShortcuts(vertex, false);
            } else {
                // resort the priority queue as necessary
                while (true) {
                    shortcutsAndSearchSpace = getShortcuts(vertex, true);
                    int deleted = 0;
                    if (deletedNeighbors.containsKey(vertex)) {
                        deleted = deletedNeighbors.get(vertex);
                    }
                    double new_prio = getImportance(vertex, shortcutsAndSearchSpace, deleted);
                    Double new_min = pq.peek_min_key();
                    if (new_prio <= new_min) {
                        break;
                    } else {
                        pq.insert(vertex, new_prio);
                        vertex = pq.extract_min();
                    }
                }

                shortcutsAndSearchSpace = getShortcuts(vertex, false);
            }

            long now = System.currentTimeMillis();
            if (now - lastNotified > 5000) {
                _log.debug("contracted: " + i + " / " + totalContractableVertices 
                        + " (" + i / (double)totalContractableVertices 
                        + ") total time "
                        + (now - start) / 1000.0 + "sec, average degree "
                        + nContractableEdges / (nContractableVertices + 0.00001));
                lastNotified = now;
            }

            // move edges from main graph to up and down graphs
            // vertices that are still in the graph are, by definition, of higher importance than
            // the one currently being plucked from the graph. Edges that go out are upward edges.
            // Edges that are coming in are downward edges.

            HashSet<Vertex> neighbors = new HashSet<Vertex>();

            // outgoing, therefore upward
            for (DirectEdge de : filter(core.getOutgoing(vertex), DirectEdge.class)) {
                // do not use removedirectedge 
                // to avoid erasing the edge list out from under iteration
                Vertex toVertex = de.getToVertex();
                core.removeIncoming(toVertex, de);
                updown.addOutgoing(vertex, de);
                neighbors.add(toVertex);
                nContractableEdges--;
            }

            // incoming, therefore downward
            for (DirectEdge de : filter(core.getIncoming(vertex), DirectEdge.class)) {
                Vertex fromVertex = de.getFromVertex();
                core.removeOutgoing(fromVertex, de);
                updown.addIncoming(vertex, de);
                neighbors.add(fromVertex);
                nContractableEdges--;
            }

            // remove vertex from working overlay graph,
            // along with its outgoing and incoming edge lists
            core.removeVertex(vertex);

            /* update neighbors' priority and deleted neighbors */
            for (Vertex n : neighbors) {
                int deleted = 0;
                if (deletedNeighbors.containsKey(n)) {
                    deleted = deletedNeighbors.get(n);
                }
                deleted += 1;
                deletedNeighbors.put(n, deleted);
                //update priority queue
                WitnessSearchResult nwsr = getShortcuts(n, true);
                double new_prio = getImportance(n, nwsr, deleted);
                pq.rekey(n, new_prio);
            }

            List<Shortcut> shortcuts = shortcutsAndSearchSpace.shortcuts;
            // Add shortcuts to graph
            for (Shortcut shortcut : shortcuts) {
                core.addDirectEdge(shortcut);
            }

            nContractableVertices--;
            nContractableEdges += shortcuts.size();
            if (nContractableVertices == 0) {
                continue;
            }

//            float averageDegree = nEdges / nVertices;
            
//            int oldHopLimit = hopLimit;
//            if (averageDegree > 10) {
//                if (edgesRemoved) {
//                    hopLimit = 5;
//                } else {
//                    hopLimit = 3;
//                    int removed = removeNonoptimalEdges(5);
//                    nEdges -= removed;
//                    edgesRemoved = true;
//                }
//            } else {
//                if (averageDegree > 3.3 && hopLimit == 1) {
//                    hopLimit = 2;
//                }
//            }
//
//            // after a hop limit upgrade, rebuild the priority queue
//            if (oldHopLimit != hopLimit) {
//                pq = rebuildPriorityQueue(hopLimit, deletedNeighbors);
//            }
        }
        threadPool.shutdownNow();
        threadPool = null;
    }

    private void createThreadPool() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        _log.debug("number of threads: " + nThreads);
        ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<Runnable>(1000);
        threadPool = new ThreadPoolExecutor(nThreads, nThreads, 10, TimeUnit.SECONDS, taskQueue);
    }

    private BinHeap<Vertex> rebuildPriorityQueue(int hopLimit,
            HashMap<Vertex, Integer> deletedNeighbors) {

    	_log.debug("    rebuild priority queue, hoplimit is now {}", hopLimit);
    	BinHeap<Vertex> newpq = new BinHeap<Vertex>(core.getVertices().size());
        int i = 0;
        for (Vertex v : core.getVertices()) {
        	if (++i % 100000 == 0)
        		_log.debug("    vertex {}", i);

            if (!isContractable(v)) {
                continue;
            }
            WitnessSearchResult wsresult = getShortcuts(v, true);
            Integer deleted = deletedNeighbors.get(v);
            if (deleted == null) {
                deleted = 0;
            }
            int imp = getImportance(v, wsresult, deleted);
            newpq.insert(v, imp);
        }
        return newpq;
    }

    private int removeNonoptimalEdges(int hopLimit) {

    	_log.debug("removing non-optimal edges, hopLimit is {}", hopLimit);
        int removed = 0;
        for (Vertex u : core.getVertices()) {
            State su = new State(u, fwdOptions);
            Dijkstra dijkstra = new Dijkstra(core, u, fwdOptions, null, hopLimit);
            BasicShortestPathTree spt = dijkstra.getShortestPathTree(Double.POSITIVE_INFINITY,
                    Integer.MAX_VALUE);
            ArrayList<DirectEdge> toRemove = new ArrayList<DirectEdge>();
            for (DirectEdge e : filter(core.getOutgoing(u),DirectEdge.class)) {
                if (!isContractable(e)) {
                    continue;
                }
                State svSpt = spt.getState(e.getToVertex());
                State sv = e.traverse(su);
                if (sv == null) {
                    //it is safe to remove edges that are not traversable anyway
                    toRemove.add(e);
                    continue;
                }
                if (svSpt != null && sv.getBackState().getVertex() != u
                        && svSpt.getWeight() <= sv.getWeight() + .01) {
                    toRemove.add(e);
                }
            }
            for (DirectEdge e : toRemove) {
                core.removeDirectEdge(e);
            }
            removed += toRemove.size();
        }
        return removed;
    }

    private int countContractableEdges(OverlayGraph og) {
        int total = 0;
        for (Vertex v : og.getVertices()) {
        	for (Edge e : og.getOutgoing(v))
        		if (isContractable(e))
        			total++;
                        // don't count all edges, just contractable ones
                        //            total += gv.getDegreeOut();
        }
        return total;
    }

    /**
     * Bidirectional Dijkstra's algorithm with some twists: For forward searches, The search from
     * the target stops when it hits the uncontracted core of the graph and the search from the
     * source continues across the (time-dependent) core.
     * 
     */
    public GraphPath getShortestPath(Vertex origin, Vertex target, long time,
            TraverseOptions opt) {

    	TraverseOptions upOptions = opt.clone();
    	TraverseOptions downOptions = opt.clone();

        /** max walk distance cannot be less than distances to nearest transit stops */
        double minWalkDistance = 
                origin.getDistanceToNearestTransitStop() + target.getDistanceToNearestTransitStop();
        upOptions.setMaxWalkDistance(Math.max(upOptions.getMaxWalkDistance(), minWalkDistance));
        downOptions.setMaxWalkDistance(Math.max(downOptions.getMaxWalkDistance(), minWalkDistance));

    	upOptions.setArriveBy(false);
    	downOptions.setArriveBy(true);
    	
    	final boolean VERBOSE = false;
    	
    	// DEBUG no walk limit
    	//options.maxWalkDistance = Double.MAX_VALUE;
    	
    	final int INITIAL_SIZE = 1000; 
    	
        if (origin == null || target == null) {
            return null;
        }
        
        if (VERBOSE)
        	_log.debug("origin {} target {}", origin, target);

        Map<Vertex, ArrayList<Edge>> extraEdges = getExtraEdges(origin, target);
        
        BasicShortestPathTree upspt = new BasicShortestPathTree();
        BasicShortestPathTree downspt = new BasicShortestPathTree();

        BinHeap<State> upqueue = new BinHeap<State>(INITIAL_SIZE);
        BinHeap<State> downqueue = new BinHeap<State>(INITIAL_SIZE);

        // These sets are used not only to avoid revisiting nodes, but to find meetings
        HashSet<Vertex> upclosed = new HashSet<Vertex>(INITIAL_SIZE);
        HashSet<Vertex> downclosed = new HashSet<Vertex>(INITIAL_SIZE);

        State upInit = new State(time, origin, upOptions);
        upspt.add(upInit);
        upqueue.insert(upInit, upInit.getWeight());

        /* FIXME: insert extra bike walking targets */

        State downInit = new State(time, target, downOptions);
        downspt.add(downInit);
        downqueue.insert(downInit, downInit.getWeight());
        
        Vertex meeting = null;
        double bestMeetingCost = Double.POSITIVE_INFINITY;

        boolean done_up = false;
        boolean done_down = false;
        
        while (!(done_up && done_down)) { 
            // up and down steps could be a single method with parameters changed
            // if ( ! done_up) treeStep(queue, options, thisClosed, thatClosed, thisSpt, thatSpt, bestMeeting);
            
            /* one step on the up tree */
            if (!done_up) {
                if (upqueue.empty()) {
                    done_up = true;
                    continue;
                }
                
                State up_su = upqueue.extract_min(); // get the lowest-weightSum
//                if ( ! upspt.visit(up_su))
//                	continue;
                
                if (up_su.exceedsWeightLimit(bestMeetingCost)) {
                    if (VERBOSE)
                    	_log.debug("pruning on up tree (best meeting cost)");
                    done_up = true;
                    continue;
                }

                Vertex u = up_su.getVertex();
                upclosed.add(u); // should basic spt maintain a closed list?
                if (VERBOSE)
                	_log.debug("    extract up {}", u);

                if (downclosed.contains(u) && downspt.getState(u).getWalkDistance() + up_su.getWalkDistance() <= up_su.getOptions().getMaxWalkDistance()) {
                    double thisMeetingCost = up_su.getWeight() + downspt.getState(u).getWeight();
                    if (VERBOSE)
                    	_log.debug("    meeting at {}", u);
                    if (thisMeetingCost < bestMeetingCost) {
                        bestMeetingCost = thisMeetingCost;
                        meeting = u;
                    }
                    continue;
                }

                Collection<Edge> outgoing = core.getOutgoing(u);
                if (opt.isArriveBy() && !outgoing.isEmpty()) {
                    // up path can only explore until core vertices on reverse paths
                    continue;
                }
                if (VERBOSE)
                    _log.debug("        {} edges in core", outgoing.size());

                Collection<Edge> upOutgoing = updown.getOutgoing(u);
                if (!upOutgoing.isEmpty()) {
                    if (VERBOSE)
                        _log.debug("        {} edges in overlay", upOutgoing.size());
                    if (outgoing.isEmpty()) {
                        outgoing = upOutgoing;
                    } else {
                        ArrayList<Edge> newOutgoing = new ArrayList<Edge>(outgoing.size()
                                + upOutgoing.size());
                        newOutgoing.addAll(outgoing);
                        newOutgoing.addAll(upOutgoing);
                        outgoing = newOutgoing;
                    }
                }
                
                if (extraEdges.containsKey(u)) {
                    List<Edge> newOutgoing = new ArrayList<Edge>();
                    newOutgoing.addAll(extraEdges.get(u));
                    if (VERBOSE)
                        _log.debug("        {} edges in extra", newOutgoing.size());
                    newOutgoing.addAll(outgoing);
                    outgoing = newOutgoing;
                }

                for (Edge edge : outgoing) {
                    if (VERBOSE)
                    	_log.debug("        edge up {}", edge);

                    if (edge instanceof OutEdge) {
                        continue;
                    }

                    State up_sv = edge.traverse(up_su);

                    // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                    // over.
                    if (up_sv == null) {
                        continue;
                    }

                    Vertex v = up_sv.getVertex();
                    if (upclosed.contains(v)) {
                        continue;
                    }

                    if (up_sv.exceedsWeightLimit(opt.maxWeight)) {
                        //too expensive to get here
                        continue;
                    }
                    if (!opt.isArriveBy() && up_sv.getTime() > opt.worstTime) {
                        continue;
                    }
                    if (upspt.add(up_sv)) {
                    	double weight = up_sv.getWeight();
                    	if (weight < bestMeetingCost)
                    		upqueue.insert(up_sv, weight);
                    }
                }
            }
            
            /* one step on the down tree */
            if (!done_down) {
                if (downqueue.empty()) {
                    done_down = true;
                    continue;
                }
                State down_su = downqueue.extract_min(); // get the lowest-weightSum
                if ( ! downspt.visit(down_su))
                	continue;

                if (down_su.exceedsWeightLimit(bestMeetingCost)) {
                    done_down = true;
                    continue;
                }

                Vertex down_u = down_su.getVertex();

                if (VERBOSE)
                	_log.debug("    extract down {}", down_u);

                if (upclosed.contains(down_u) && upspt.getState(down_u).getWalkDistance() + down_su.getWalkDistance() <= down_su.getOptions().getMaxWalkDistance()) {
                    double thisMeetingCost = down_su.getWeight() + upspt.getState(down_u).getWeight();
                    if (VERBOSE)
                    	_log.debug("    meeting at {}", down_u);
                    if (thisMeetingCost < bestMeetingCost) {
                        bestMeetingCost = thisMeetingCost;
                        meeting = down_u;
                    }
                }

                downclosed.add(down_u);
                
                Collection<Edge> incoming = core.getIncoming(down_u);
                if (!opt.isArriveBy() && !incoming.isEmpty()) {
                    // down path can only explore until core vertices on forward paths
                    continue;
                }
                if (VERBOSE)
                    _log.debug("        {} edges in core", incoming.size());

                Collection<Edge> downIncoming = updown.getIncoming(down_u); 
                if (!downIncoming.isEmpty()) {
                    if (incoming.isEmpty()) {
                        incoming = downIncoming;
                    } else {
                        ArrayList<Edge> newIncoming = new ArrayList<Edge>(incoming.size()
                                + downIncoming.size());
                        newIncoming.addAll(incoming);
                        newIncoming.addAll(downIncoming);
                        incoming = newIncoming;
                    }
                }
                if (VERBOSE)
                    _log.debug("        {} edges with overlay", incoming.size());

                if (extraEdges.containsKey(down_u)) {
                    List<Edge> newIncoming = new ArrayList<Edge>();
                    newIncoming.addAll(incoming);
                    newIncoming.addAll(extraEdges.get(down_u));
                    incoming = newIncoming;
                }
                if (VERBOSE)
                    _log.debug("        {} edges with overlay and extra", incoming.size());

                for (Edge edge : incoming) {
                    if (VERBOSE)
                    	_log.debug("        edge down {}", edge);

                    Vertex down_v = edge.getFromVertex();

                    if (downclosed.contains(down_v)) {
                        continue;
                    }

                    if (edge instanceof OutEdge) {
                        continue; 
                    }

                    // traverse will be backward because ArriveBy was set in the initial down state
                    State down_sv = edge.traverse(down_su);
                    if (VERBOSE)
                    	_log.debug("        result down {}", down_sv);

                    // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                    // over.
                    if (down_sv == null) {
                        continue;
                    }
                    if (down_sv.exceedsWeightLimit(opt.maxWeight)) {
                        if (VERBOSE)
                        	_log.debug("        down result too heavy {}", down_sv);
                        //too expensive to get here
                        continue;
                    }
                    if (opt.isArriveBy() && down_sv.getTime() < opt.worstTime) {
                        if (VERBOSE)
                        	_log.debug("        down result exceeds worst time {} {}", opt.worstTime, down_sv);
                        continue;
                    }
                    if (downspt.add(down_sv)) {
                    	double weight = down_sv.getWeight();
                    	if (weight < bestMeetingCost)
                    		downqueue.insert(down_sv, weight);
                    }
                }
            }
        }

        if (meeting == null) {
            return null;
        } else {
        	if (VERBOSE)
        		_log.debug("meeting point is {}", meeting);
        }

        /* Splice paths */
        State upMeet = upspt.getState(meeting); 
        State downMeet = downspt.getState(meeting); 
        State r = opt.isArriveBy() ? downMeet : upMeet;
        State s = opt.isArriveBy() ? upMeet : downMeet;
        while (s.getBackEdge() != null) {
        	r = s.getBackEdge().traverse(r);
        	// traversals might exceed limits here, even if they didn't during search
        	if (r == null)
        		return null; 
        	s = s.getBackState();
        }

        /* Unpack shortcuts */
        s = r.reversedClone();
        while (r.getBackEdge() != null) {
            Edge e = r.getBackEdge(); 
        	if (e instanceof Shortcut)
	      		s = ((Shortcut)e).unpackTraverse(s); 
	      	else
	      		s = e.traverse(s);
        	// traversals might exceed limits and fail during unpacking, even if they didn't during search
        	if (s == null)
        		return null; 
        	r = r.getBackState();
        }

        // no need to request optimization, we just unpacked the path backward
        GraphPath ret = new GraphPath(s, false);
       	return ret;

    }

    private Map<Vertex, ArrayList<Edge>> getExtraEdges(Vertex origin, Vertex target) {
        Map<Vertex, ArrayList<Edge>> extraEdges;
        if (origin instanceof StreetLocation) {
            extraEdges = new HashMap<Vertex, ArrayList<Edge>>();
            Iterable<DirectEdge> extra = ((StreetLocation)origin).getExtra();
            for (DirectEdge edge : extra) {
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
            Iterable<DirectEdge> extra = ((StreetLocation) target).getExtra();
            for (DirectEdge edge : extra) {
                Vertex tov = edge.getToVertex();
                ArrayList<Edge> edges = extraEdges.get(tov);
                if (edges == null) {
                    edges = new ArrayList<Edge>(); 
                    extraEdges.put(tov, edges);
                }
                edges.add(edge);
            }
        }
        return extraEdges;
    }


}
