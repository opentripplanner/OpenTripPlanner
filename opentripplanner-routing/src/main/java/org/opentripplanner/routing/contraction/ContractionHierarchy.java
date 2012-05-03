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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.util.NullExtraEdges;
import org.opentripplanner.routing.vertextype.StreetVertex;
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

    public Graph graph;

    // _contractable_ core vertices -- we maintain both a queue and a set to allow 
    // fast set membership checking.
    public Set<Vertex> corev, chv;  

    private double contractionFactor;

    private transient RoutingRequest fwdOptions, backOptions;

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
        for (Edge e : u.getIncoming()) {
            if (! corev.contains(e.getFromVertex()))
                continue;
            State sv = e.traverse(su);
            if (sv == null)
                continue;
            vs.add(sv);
        }

        /* Compute the cost to each vertex with an outgoing edge from the target */
        su = new State(u, fwdOptions); // search forward
        double maxWWeight = 0;

        ArrayList<State> ws = new ArrayList<State>();
        //System.out.println("vertex " + u);
        for (Edge e : u.getOutgoing()) {
            if (! corev.contains(e.getToVertex()))
                continue;
            State sw = e.traverse(su);
            if (sw == null)
                continue;
            ws.add(sw);
            if (sw.exceedsWeightLimit(maxWWeight))
                maxWWeight = sw.getWeight();
        }

        /* figure out which shortcuts are needed */
        List<PotentialShortcut> shortcuts = new ArrayList<PotentialShortcut>();

        ArrayList<Callable<WitnessSearchResult>> tasks = 
                new ArrayList<Callable<WitnessSearchResult>>(vs.size());
        
        int nodeLimit = simulate ? NODE_LIMIT_SIMULATE : NODE_LIMIT_CONTRACT;
        int hopLimit = simulate ? HOP_LIMIT_SIMULATE : HOP_LIMIT_CONTRACT;

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
//                if (!simulate && spt != null) {
//                    /* while we're here, remove some non-optimal edges */
//                    ArrayList<Edge> toRemove = new ArrayList<Edge>();
//                    // starting from each v
//                    State sv0 = new State(wsresult.vertex, fwdOptions);
//                    for (Edge e : wsresult.vertex.getOutgoing()) {
//                        State sSpt = spt.getState(e.getToVertex());
//                        if (sSpt == null) {
//                            continue;
//                        }
//                        State sv1 = e.traverse(sv0);
//                        if (sv1 == null) {
//                            toRemove.add(e);
//                            continue;
//                        }
//                        if (sSpt.getWeight() < sv1.getWeight()) {
//                            // the path found by Dijkstra from u to e.tov is better
//                            // than the path through e. Therefore e can be deleted.
//                            toRemove.add(e);
//                        }
//                    }
//
//                    Vertex uv = wsresult.vertex;
//                    for (Edge e : toRemove) {
//                        // concurrent modification argh
//                        //e.detach();
////                        uv.removeOutgoing(e);
////                        e.getToVertex().removeIncoming(e);
//                    }
//                }
                searchSpace += wsresult.searchSpace;
                shortcuts.addAll(wsresult.shortcuts);
            }
        } catch (Exception e1) {
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

    	Dijkstra dijkstra = new Dijkstra(v.getVertex(), fwdOptions, u, hopLimit);
        dijkstra.setTargets(ws); 
        dijkstra.setRouteOn(corev);
        BasicShortestPathTree spt = dijkstra.getShortestPathTree(weightLimit, nodeLimit);

        ArrayList<PotentialShortcut> shortcuts = new ArrayList<PotentialShortcut>();

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
                // this is implicitly modifying edgelists while searches are happening.
                // must use threadsafe list modifications in Shortcut, or no multithreafding.
                shortcuts.add(new PotentialShortcut(v.getBackEdge(), w.getBackEdge(), 
                        timeThroughU, weightThroughU, walkDistance));
            }
        }
        
        return new WitnessSearchResult(shortcuts, spt, v.getVertex(), spt.getVertexCount());
    }

    public class PotentialShortcut {
        Edge e1, e2;
        int time;
        double weight, walk;
        PotentialShortcut (Edge e1, Edge e2, int time, double weight, double walk) {
            this.e1=e1;
            this.e2=e2;
            this.time = time;
            this.weight = weight;
            this.walk = walk;
        }
        public void make () {
            new Shortcut(e1, e2, time, weight, walk, mode);
        }
    }
    
    /**
     * Returns the importance of a vertex from the edge difference, number of deleted neighbors, and
     * search space size ("EDS").
     */
    private int getImportance(Vertex v, WitnessSearchResult shortcutsAndSearchSpace,
            int deletedNeighbors) {
        int degree_in = v.getDegreeIn();
        int degree_out = v.getDegreeOut();
        int edge_difference = shortcutsAndSearchSpace.shortcuts.size() - (degree_in + degree_out);
        int searchSpace = shortcutsAndSearchSpace.searchSpace;

        return edge_difference * 190 + deletedNeighbors * 120 + searchSpace;
    }

    /**
     * Initialize the priority queue for contracting, by computing the initial importance of every
     * node. Also initializes the set of nodes to be contracted.
     * 
     * @return
     */
    BinHeap<Vertex> initPriorityQueue(Graph g) {
        BinHeap<Vertex> pq = new BinHeap<Vertex>(g.getVertices().size());
        corev = new HashSet<Vertex>();
        chv = new HashSet<Vertex>();
        for (Vertex v : g.getVertices()) {
            if (v instanceof StreetVertex)
                corev.add(v);
        }
        for (Vertex v : corev) {
//            if (v.getDegreeIn() > 7 || v.getDegreeOut() > 7)
//                continue;
            WitnessSearchResult shortcuts = getShortcuts(v, true);
            int imp = getImportance(v, shortcuts, 0);
            pq.insert(v, imp);
        }
        return pq;
    }

    /**
     * Create a contraction hierarchy from a graph.
     * 
     * @param orig
     * @param options
     * @param contractionFactor
     *            A fraction from 0 to 1 of (the contractable portion of) the graph to contract
     */
    public ContractionHierarchy(Graph graph, RoutingRequest options, double contractionFactor) {
        
        this.graph = graph;
        fwdOptions = options;
        fwdOptions.setMaxWalkDistance(Double.MAX_VALUE);
        fwdOptions.setArriveBy(false);
        backOptions = fwdOptions.clone();
        backOptions.setArriveBy(true);
        this.contractionFactor = contractionFactor;

        // TODO LG Check this
        TraverseModeSet modes = this.fwdOptions.getModes(); 
        this.mode = modes.getCar() ? TraverseMode.CAR
                : modes.getBicycle() ? TraverseMode.BICYCLE : TraverseMode.WALK;
        build();
    }

    /**
     * Does the work of construting the CH.
     * 
     */
    void build() {

        createThreadPool();

        long start = System.currentTimeMillis();

        HashMap<Vertex, Integer> deletedNeighbors = new HashMap<Vertex, Integer>();
        _log.debug("Preparing contraction hierarchy -- this may take quite a while");
        _log.debug("Initializing priority queue");

        // initialize a priority queue containing only contractable vertices
        BinHeap<Vertex> pq = initPriorityQueue(graph);

        _log.debug("Contracting");
        long lastNotified = System.currentTimeMillis();
        int i = 0;
        int totalContractableVertices = pq.size(); // == corev.size()
        int nContractableEdges = countContractableEdges(graph);
        boolean edgesRemoved = false;

        while (!pq.empty()) {
            // stop contracting once a core is reached
            if (i > Math.ceil(totalContractableVertices * contractionFactor)) {
                break;
            }
            i += 1;

/*         
 * "Keeping the cost of contraction up-to-date in the priority queue is quite costly. The 
 * contraction of a node in a search tree of the local search can aﬀect the cost of contraction. So 
 * after the contraction of a node w, it would be necessary to recalculate the priority of each node
 * that has w in their local search spaces. Most local search spaces are small but there are 
 * exceptions.
 * If there is an edge with a large edge weight, e.g. a long-distance ferry connection, the search
 * space can grow arbitrarily and with it the number of nodes that trigger a change in the cost of
 * contraction. A simpliﬁed example can be found in Figure 6.
 * In our implementation we will omit exakt updates for the sake of performance, update only
 * the neighbors of the contracted node and eventually use lazy updates to cope with drastic 
 * increases of search space sizes." Geisberger (2008)
 */
            
//            if (potentialLazyUpdates > 30 && lazyUpdates > 28) {
//                rebuildPriorityQueue(hopLimit, deletedNeighbors);
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
                // resort the priority queue as necessary (lazy updates)
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
                        + nContractableEdges / (corev.size() + 0.00001));
                lastNotified = now;
            }

            // move edges from main graph to up and down graphs
            // vertices that are still in the graph are, by definition, of higher importance than
            // the one currently being plucked from the graph. Edges that go out are upward edges.
            // Edges that are coming in are downward edges.

            HashSet<Vertex> neighbors = new HashSet<Vertex>();

            // outgoing, therefore upward
            for (Edge de : vertex.getOutgoing()) {
                Vertex toVertex = de.getToVertex();
                neighbors.add(toVertex);
                nContractableEdges--;
            }

            // incoming, therefore downward
            for (Edge de : vertex.getIncoming()) {
                Vertex fromVertex = de.getFromVertex();
                neighbors.add(fromVertex);
                nContractableEdges--;
            }

            /* update neighbors' priority and deleted neighbors */
            for (Vertex n : neighbors) {
                if (n == null) {
                    _log.warn("a neighbor vertex of {} was null", vertex);
                    continue;
                }
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

            // need to explicitly add shortcuts to graph - they not added as they are found
            // to void concurrency problems. 
            for (PotentialShortcut ps : shortcutsAndSearchSpace.shortcuts) {
                ps.make();
                nContractableEdges += 1;
            }
            
            /* effectively move vertex out of core */
            // if this was done before searching 
            // this should replace the taboo vertex in Dijkstra.
            if (corev.remove(vertex)) {
                chv.add(vertex);
            } else {
                _log.warn("attempt to move vertex out of core when it was not still in core.");
            }
            if (corev.size() == 0) {
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
        //nThreads = 1; // temp fix for concurrent modification by shortcuts
        _log.debug("number of threads: " + nThreads);
        ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<Runnable>(1000);
        threadPool = new ThreadPoolExecutor(nThreads, nThreads, 10, TimeUnit.SECONDS, taskQueue);
    }

    private BinHeap<Vertex> rebuildPriorityQueue(int hopLimit,
            HashMap<Vertex, Integer> deletedNeighbors) {

    	_log.debug("    rebuild priority queue, hoplimit is now {}", hopLimit);
    	BinHeap<Vertex> newpq = new BinHeap<Vertex>(corev.size());
        for (Vertex v : corev) {
            if (! (v instanceof StreetVertex))
                continue;

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
        for (Vertex u : corev) {
            State su = new State(u, fwdOptions);
            Dijkstra dijkstra = new Dijkstra(u, fwdOptions, null, hopLimit);
            dijkstra.setRouteOn(corev);
            BasicShortestPathTree spt = dijkstra.getShortestPathTree(Double.POSITIVE_INFINITY,
                    Integer.MAX_VALUE);
            ArrayList<Edge> toRemove = new ArrayList<Edge>();
            for (Edge e : u.getOutgoing()) {
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
            for (Edge e : toRemove) {
                e.detach(); // TODO: is this really the right action to take?
            }
            removed += toRemove.size();
        }
        return removed;
    }

    private int countContractableEdges(Graph g) {
        int total = 0;
        for (Vertex v : g.getVertices()) {
            if (v instanceof StreetVertex)
                total += v.getDegreeOut();
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
            RoutingRequest opt) {

        if (origin == null || target == null) {
            return null;
        }

        RoutingRequest upOptions = opt.clone();
        RoutingRequest downOptions = opt.clone();

        //TODO: verify set to/from are correct (AMB)
    	upOptions.setArriveBy(false);
    	upOptions.dateTime = time;
    	upOptions.setRoutingContext(graph, origin, target);
        downOptions.setArriveBy(true);
        downOptions.dateTime = time;
        upOptions.setRoutingContext(graph, target, origin);

    	/** max walk distance cannot be less than distances to nearest transit stops */
    	double minWalkDistance = 
    	        origin.getDistanceToNearestTransitStop() + target.getDistanceToNearestTransitStop();
    	upOptions.setMaxWalkDistance(Math.max(upOptions.getMaxWalkDistance(), minWalkDistance));
    	downOptions.setMaxWalkDistance(Math.max(downOptions.getMaxWalkDistance(), minWalkDistance));
        
    	final boolean VERBOSE = false;
    	
    	// DEBUG no walk limit
    	//options.maxWalkDistance = Double.MAX_VALUE;
    	
    	final int INITIAL_SIZE = 1000; 
        
        if (VERBOSE)
        	_log.debug("origin {} target {}", origin, target);

        Map<Vertex, ArrayList<Edge>> extraEdges = getExtraEdges(origin, target);
        
        BasicShortestPathTree upspt = new BasicShortestPathTree(upOptions);
        BasicShortestPathTree downspt = new BasicShortestPathTree(downOptions);

        BinHeap<State> upqueue = new BinHeap<State>(INITIAL_SIZE);
        BinHeap<State> downqueue = new BinHeap<State>(INITIAL_SIZE);

        // These sets are used not only to avoid revisiting nodes, but to find meetings
        HashSet<Vertex> upclosed = new HashSet<Vertex>(INITIAL_SIZE);
        HashSet<Vertex> downclosed = new HashSet<Vertex>(INITIAL_SIZE);

        State upInit = new State(origin, upOptions);
        upspt.add(upInit);
        upqueue.insert(upInit, upInit.getWeight());

        /* FIXME: insert extra bike walking targets */

        State downInit = new State(target, downOptions);
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

                if (opt.isArriveBy() && !(u instanceof StreetVertex)) {
                    // up path can only explore until core vertices on reverse paths
                    continue;
                }
                Collection<Edge> outgoing = u.getOutgoing();
                if (VERBOSE)
                    _log.debug("        {} edges in core", outgoing.size());

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
                
                if (!opt.isArriveBy() && !(down_u instanceof StreetVertex)) {
                    // down path can only explore until core vertices on forward paths
                    continue;
                }
                Collection<Edge> incoming = down_u.getIncoming();
                if (VERBOSE)
                    _log.debug("        {} edges in core", incoming.size());

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
            Iterable<Edge> extra = ((StreetLocation)origin).getExtra();
            for (Edge edge : extra) {
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
            Iterable<Edge> extra = ((StreetLocation) target).getExtra();
            for (Edge edge : extra) {
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
