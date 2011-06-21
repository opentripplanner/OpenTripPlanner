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
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.VertexIngress;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.util.NullExtraEdges;
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
	private static final int HOP_LIMIT_SIMULATE = 10;
    private static final int HOP_LIMIT_CONTRACT = Integer.MAX_VALUE;
    private static final int NODE_LIMIT_SIMULATE = 500;
    private static final int NODE_LIMIT_CONTRACT = Integer.MAX_VALUE;

    private static final Logger _log = LoggerFactory.getLogger(ContractionHierarchy.class);

    private static final long serialVersionUID = 6651191142274381518L;

    public Graph graph;

    public Graph up;

    public Graph down;

    private double contractionFactor;

    private transient TraverseOptions options, backOptions;

    private transient TraverseMode mode;

    private ThreadPoolExecutor threadPool;


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

        State su = new State(u, backOptions); // search backward

        /* Compute the cost from each vertex with an incoming edge to the target */
        int searchSpace = 0;
        ArrayList<VertexIngress> vs = new ArrayList<VertexIngress>();
        for (Edge e : graph.getIncoming(u)) {
            if (!isContractable(e)) {
                continue;
            }
            State sv = e.traverse(su);
            if (sv == null) {
                continue;
            }
            // could just use keep states instead of making vertexIngress
            vs.add(new VertexIngress(sv.getVertex(), (DirectEdge)e, sv.getWeight(), sv.getAbsTimeDeltaMsec(), sv.getWalkDistance()));
        }

        /* Compute the cost to each vertex with an outgoing edge from the target */
        su = new State(u, options); // search forward
        double maxWWeight = 0;

        HashSet<Vertex> wSet = new HashSet<Vertex>();
        ArrayList<VertexIngress> ws = new ArrayList<VertexIngress>();
        for (DirectEdge e : filter(graph.getOutgoing(u),DirectEdge.class)) {
            if (!isContractable(e)) {
                continue;
            }
            State sw = e.traverse(su);
            if (sw == null) {
                continue;
            }
            Vertex w = sw.getVertex();
            wSet.add(w);
            ws.add(new VertexIngress(w, e, sw.getWeight(), sw.getTimeDeltaMsec(), sw.getWalkDistance()));
            if (sw.exceedsWeightLimit(maxWWeight)) {
                maxWWeight = sw.getWeight();
            }
        }

        /* figure out which shortcuts are needed */
        List<Shortcut> shortcuts = new ArrayList<Shortcut>();

        ArrayList<Callable<WitnessSearchResult>> tasks = new ArrayList<Callable<WitnessSearchResult>>(
                vs.size());
        
        int nodeLimit = simulate ? NODE_LIMIT_SIMULATE : NODE_LIMIT_CONTRACT;
        int hopLimit = simulate ? HOP_LIMIT_SIMULATE : HOP_LIMIT_CONTRACT;

        // FOR EACH V
        for (VertexIngress v : vs) {
            //allow about a second of inefficiency in routes in the name of planning
            //efficiency (+ 1)
            double weightLimit = v.weight + maxWWeight + 1;
            WitnessSearch task = new WitnessSearch(u, hopLimit, nodeLimit, 
                    weightLimit, wSet, ws, v);
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
                    State sv0 = new State(wsresult.vertex, options);
                    for (DirectEdge e : filter(graph.getOutgoing(wsresult.vertex),DirectEdge.class)) {
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

                    GraphVertex ugv = graph.getGraphVertex(wsresult.vertex);

                    for (DirectEdge e : toRemove) {
                        ugv.removeOutgoing(e);
                        graph.getGraphVertex(e.getToVertex()).removeIncoming(e);
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
        private HashSet<Vertex> wSet;
        private List<VertexIngress> ws;
        private VertexIngress v;
        private int nodeLimit;

        public WitnessSearch(Vertex u, int hopLimit, int nodeLimit,
                double weightLimit, HashSet<Vertex> wSet, 
                List<VertexIngress> ws, VertexIngress v) {
            this.u = u;
            this.hopLimit = hopLimit;
            this.nodeLimit = nodeLimit;
            this.weightLimit = weightLimit;
            this.wSet = wSet;
            this.ws = ws;
            this.v = v;
        }

        public WitnessSearchResult call() {
            return searchWitnesses(u, hopLimit, nodeLimit, weightLimit, wSet,
                    ws, v);
        }
    }

    private WitnessSearchResult searchWitnesses(Vertex u, int hopLimit, 
    		int nodeLimit, double weightLimit, HashSet<Vertex> wSet, 
    		List<VertexIngress> ws, VertexIngress v) {

    	Dijkstra dijkstra = new Dijkstra(graph, v.vertex, options, u, hopLimit);
        dijkstra.setTargets(wSet); // set is now cloned inside dijkstra, since it is used destructively (AMB)
        BasicShortestPathTree spt = dijkstra.getShortestPathTree(weightLimit, nodeLimit);

        ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();

        // FOR EACH W
        for (VertexIngress w : ws) {

            if (v.vertex == w.vertex) {
                continue;
            }

            double weightThroughU = v.weight + w.weight;
            State pathAroundU = spt.getState(w.vertex);

            // if the path around the vertex is longer than the path through,
            // add the path through to the shortcuts
            if (pathAroundU == null || pathAroundU.exceedsWeightLimit(weightThroughU + .01)) {
                int timeThroughU = (int) ((w.time + v.time) / 1000);
                double walkDistance = v.walkDistance + w.walkDistance;
                Shortcut vuw = new Shortcut(v.edge, w.edge, timeThroughU, weightThroughU, walkDistance, mode);
                shortcuts.add(vuw);
            }
        }
        
        return new WitnessSearchResult(shortcuts, spt, v.vertex, spt.getVertexCount());
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
        int degree_in = graph.getDegreeIn(v);
        int degree_out = graph.getDegreeOut(v);
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
    BinHeap<Vertex> initPriorityQueue(Graph graph) {
        Collection<GraphVertex> vertices = graph.getVertices();
        BinHeap<Vertex> pq = new BinHeap<Vertex>(vertices.size());

        int i = 0;
        for (GraphVertex gv : vertices) {
        	if (++i % 100000 == 0)
        		_log.debug("    vertex {}", i);
        	
            Vertex v = gv.vertex;
            if (!isContractable(v)) {
                continue;
            }
            if (gv.getDegreeIn() > 7 || gv.getDegreeOut() > 7) {
                continue;
            }
            WitnessSearchResult shortcuts = getShortcuts(v, true);
            int imp = getImportance(v, shortcuts, 0);
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
        if (v instanceof StreetVertex || v instanceof EndpointVertex) {
            for (Edge e : graph.getOutgoing(v)) {
                if( ! (e instanceof DirectEdge))
                    return false;
                DirectEdge de = (DirectEdge) e;
                Vertex tov = de.getToVertex();
                if (!(tov instanceof StreetVertex || tov instanceof EndpointVertex)) {
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
     * @param optimize
     * @param mode
     * @param contractionFactor
     *            A fraction from 0 to 1 of (the contractable portion of) the graph to contract
     */
    public ContractionHierarchy(Graph orig, OptimizeType optimize, TraverseMode mode,
            double contractionFactor) {
        graph = new Graph();
        // clone graph
        for (GraphVertex gv : orig.getVertices()) {
            graph.addGraphVertex(new GraphVertex(gv));
        }

        this.mode = mode;
        options = new TraverseOptions(new TraverseModeSet(mode));
        options.optimizeFor = optimize;
        options.maxWalkDistance = Double.MAX_VALUE;
        options.freezeTraverseMode();
        backOptions = options.clone();
        backOptions.setArriveBy(true);
        this.contractionFactor = contractionFactor;

        init();
        useCoreVerticesFrom(orig);
    }

    private void useCoreVerticesFrom(Graph orig) {
        for (GraphVertex gv : graph.getVertices()) {
            GraphVertex origgv = orig.getGraphVertex(gv.vertex.getLabel());
            if (origgv.equals(gv)) {
                graph.addGraphVertex(origgv);
            }
        }
    }

    void init() {

        createThreadPool();

        up = new Graph();
        down = new Graph();

        long start = System.currentTimeMillis();

        HashMap<Vertex, Integer> deletedNeighbors = new HashMap<Vertex, Integer>();
        _log.debug("Preparing contraction hierarchy -- this may take quite a while");
        _log.debug("init prio queue");

        BinHeap<Vertex> pq = initPriorityQueue(graph);

        _log.debug("contract");
        long lastNotified = System.currentTimeMillis();
        int i = 0;
        int nVertices = pq.size();
        int totalVertices = nVertices;
        int nEdges = countEdges(graph);
        boolean edgesRemoved = false;

        while (!pq.empty()) {
            // stop contracting once a core is reached
            if (i++ > Math.ceil(totalVertices * contractionFactor)) {
                break;
            }

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
                _log.debug("contracted: " + i + " / " + totalVertices + " (" + i / (double)totalVertices 
                		+ ") total time "
                        + (now - start) / 1000.0 + "sec, average degree "
                        + nEdges / (nVertices + 0.00001));
                lastNotified = now;
            }

            // move edges from main graph to up and down graphs
            // vertices that are still in the graph are, by definition, of higher importance than
            // the one currently being plucked from the graph. Edges that go out are upward edges.
            // Edges that are coming in are downward edges.

            // incoming, therefore downward

            GraphVertex downVertex = down.getGraphVertex(down.addVertex(vertex));

            HashSet<Vertex> neighbors = new HashSet<Vertex>();

            for (Edge ee : graph.getIncoming(vertex)) {
                nEdges--;
                GraphVertex originalFromVertex = graph.getGraphVertex(ee.getFromVertex());
                down.addVertex(originalFromVertex.vertex);

                originalFromVertex.removeOutgoing(ee);

                downVertex.addIncoming(ee);
                neighbors.add(originalFromVertex.vertex);
            }

            // outgoing, therefore upward
            GraphVertex upVertex = up.getGraphVertex(up.addVertex(vertex));

            for (DirectEdge ee : filter(graph.getOutgoing(vertex),DirectEdge.class)) {
                nEdges--;
                GraphVertex originalToVertex = graph.getGraphVertex(ee.getToVertex());
                up.addVertex(originalToVertex.vertex);

                originalToVertex.removeIncoming(ee);

                upVertex.addOutgoing(ee);
                neighbors.add(originalToVertex.vertex);
            }

            /*
             * remove vertex from original graph.
             */
            graph.removeVertex(vertex);

            /* update neighbors' priority and deleted neighbors */

            for (Vertex n : neighbors) {
                int deleted = 0;
                if (deletedNeighbors.containsKey(n)) {
                    deleted = deletedNeighbors.get(n);
                }
                deleted += 1;
                deletedNeighbors.put(n, deleted);
                //update prio queue
                WitnessSearchResult nwsr = getShortcuts(n, true);
                double new_prio = getImportance(n, nwsr, deleted);
                pq.rekey(n, new_prio);
            }

            List<Shortcut> shortcuts = shortcutsAndSearchSpace.shortcuts;
            // Add shortcuts to graph
            for (Shortcut shortcut : shortcuts) {
                graph.addEdge(shortcut.getFromVertex(), shortcut.getToVertex(), shortcut);
            }

            nVertices--;
            nEdges += shortcuts.size();
            if (nVertices == 0) {
                continue;
            }

            float averageDegree = nEdges / nVertices;
            
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
    	BinHeap<Vertex> newpq = new BinHeap<Vertex>(graph.getVertices().size());
        int i = 0;
        for (GraphVertex gv : graph.getVertices()) {
        	if (++i % 100000 == 0)
        		_log.debug("    vertex {}", i);

            Vertex v = gv.vertex;
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
        for (GraphVertex gu : graph.getVertices()) {
            Vertex u = gu.vertex;
            State su = new State(u, options);
            Dijkstra dijkstra = new Dijkstra(graph, u, options, null, hopLimit);
            BasicShortestPathTree spt = dijkstra.getShortestPathTree(Double.POSITIVE_INFINITY,
                    Integer.MAX_VALUE);
            ArrayList<DirectEdge> toRemove = new ArrayList<DirectEdge>();
            for (DirectEdge e : filter(graph.getOutgoing(u),DirectEdge.class)) {
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
                gu.removeOutgoing(e);
                graph.getGraphVertex(e.getToVertex()).removeIncoming(e);
            }
            removed += toRemove.size();
        }
        return removed;
    }

    private int countEdges(Graph graph) {
        int total = 0;
        for (GraphVertex gv : graph.getVertices()) {
        	for (Edge e : gv.getOutgoing())
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
        
        // try goal-directed CH
        RemainingWeightHeuristic h = opt.remainingWeightHeuristic;
        if (opt.isArriveBy())
        	h.computeInitialWeight(downInit, origin);
        else
        	h.computeInitialWeight(upInit, target);

        
        Vertex meeting = null;
        double bestMeetingCost = Double.POSITIVE_INFINITY;

        boolean done_up = false;
        boolean done_down = false;
        
        while (!(done_up && done_down)) { // Until the priority queue is empty:
        	// up and down steps could be a single method with parameters changed
            if (!done_up) {
                /*
                 * one step on the up tree
                 */
                if (upqueue.empty()) {
                    done_up = true;
                    continue;
                }
                
                State up_su = upqueue.extract_min(); // get the lowest-weightSum
                if ( ! upspt.visit(up_su))
                	continue;
                
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

                if (downclosed.contains(u) && downspt.getState(u).getWalkDistance() + up_su.getWalkDistance() <= up_su.getOptions().maxWalkDistance) {
                    double thisMeetingCost = up_su.getWeight() + downspt.getState(u).getWeight();
                    if (VERBOSE)
                    	_log.debug("    meeting at {}", u);
                    if (thisMeetingCost < bestMeetingCost) {
                        bestMeetingCost = thisMeetingCost;
                        meeting = u;
                    }
                    continue;
                }

                GraphVertex gu = graph.getGraphVertex(u);
                if (VERBOSE)
                	_log.debug("    up main graph vertex {}", gu);
                if (opt.isArriveBy() && gu != null) {
                    // up path can only explore until core vertices on reverse paths
                    continue;
                }

                Collection<Edge> outgoing = null;
                if (gu != null) {
                    outgoing = gu.getOutgoing();
                }
                gu = up.getGraphVertex(u);
                if (VERBOSE)
                	_log.debug("    up overlay graph vertex {}", gu);
                if (gu != null) {
                    if (outgoing == null) {
                        outgoing = gu.getOutgoing();
                    } else {
                        Collection<Edge> upOutgoing = gu.getOutgoing();
                        ArrayList<Edge> newOutgoing = new ArrayList<Edge>(outgoing.size()
                                + upOutgoing.size());
                        newOutgoing.addAll(outgoing);
                        newOutgoing.addAll(upOutgoing);
                        outgoing = newOutgoing;
                    }
                }
                
                if (extraEdges.containsKey(u)) {
                    List<Edge> newOutgoing = new ArrayList<Edge>();
                    if (outgoing != null)
                            newOutgoing.addAll(outgoing);
                    newOutgoing.addAll(extraEdges.get(u));
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
                    	double weightEstimate = up_sv.getWeight();
                    	if ( ! opt.isArriveBy())
                    		weightEstimate += h.computeForwardWeight(up_sv, target);
                    	if (weightEstimate < bestMeetingCost)
                    		upqueue.insert(up_sv, weightEstimate);
                    }
                }
            }
            /*
             * one step on the down tree
             */
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

                if (upclosed.contains(down_u) && upspt.getState(down_u).getWalkDistance() + down_su.getWalkDistance() <= down_su.getOptions().maxWalkDistance) {
                    double thisMeetingCost = down_su.getWeight() + upspt.getState(down_u).getWeight();
                    if (VERBOSE)
                    	_log.debug("    meeting at {}", down_u);
                    if (thisMeetingCost < bestMeetingCost) {
                        bestMeetingCost = thisMeetingCost;
                        meeting = down_u;
                    }
                }

                downclosed.add(down_u);
                GraphVertex maingu = graph.getGraphVertex(down_u);
                if (!opt.isArriveBy() && maingu != null) {
                    // down path can only explore until core vertices on forward paths
                    continue;
                }
                GraphVertex downgu = down.getGraphVertex(down_u);
                Collection<Edge> incoming = null; 
                if (downgu != null) { 
                    incoming = downgu.getIncoming();
                }
                if (maingu != null) {
                    if (incoming == null) {
                        incoming = maingu.getIncoming();
                    } else {
                        Collection<Edge> mainIncoming = maingu.getIncoming();
                        ArrayList<Edge> newIncoming = new ArrayList<Edge>(incoming.size()
                                + mainIncoming.size());
                        newIncoming.addAll(incoming);
                        newIncoming.addAll(mainIncoming);
                        incoming = newIncoming;
                    }
                }

                if (extraEdges.containsKey(down_u)) {
                    List<Edge> newIncoming = new ArrayList<Edge>();
                    if (incoming != null)
                    	newIncoming.addAll(incoming);

                    newIncoming.addAll(extraEdges.get(down_u));
                    incoming = newIncoming;
                }

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
                    	double weightEstimate = down_sv.getWeight();
                    	if (opt.isArriveBy())
                    		weightEstimate += h.computeReverseWeight(down_sv, origin);
                    	if (weightEstimate < bestMeetingCost)
                    		downqueue.insert(down_sv, weightEstimate);
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
