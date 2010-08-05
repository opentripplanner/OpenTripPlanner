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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.opentripplanner.routing.algorithm.Dijkstra;
import org.opentripplanner.routing.algorithm.NegativeWeightException;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.core.VertexIngress;
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.FibHeap;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.LabelBasicShortestPathTree;
import org.opentripplanner.routing.spt.SPTEdge;
import org.opentripplanner.routing.spt.SPTVertex;
import org.opentripplanner.routing.spt.BasicShortestPathTree;
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
    private final Logger _log = LoggerFactory.getLogger(ContractionHierarchy.class);

    private static final long serialVersionUID = 6651191142274381518L;

    public Graph graph;

    public Graph up;

    public Graph down;

    private double contractionFactor;

    private transient TraverseOptions options;

    private ThreadPoolExecutor threadPool;

    /**
     * Returns the set of shortcuts around a vertex, as well as the size of the space searched.
     * 
     * @param vertex
     * @param hopLimit
     * @param optimize
     * 
     * @return
     */
    public WitnessSearchResult getShortcuts(Vertex vertex, int hopLimit, boolean simulate) {

        State state = new State(0);

        /* Compute the cost from each vertex with an incoming edge to the target */
        int searchSpace = 0;
        ArrayList<VertexIngress> us = new ArrayList<VertexIngress>();
        for (Edge e : graph.getIncoming(vertex)) {
            if (!isContractable(e)) {
                continue;
            }
            TraverseResult result = e.traverse(state, options);
            Vertex u = e.getFromVertex();
            us.add(new VertexIngress(u, e, result.weight, result.state.getTime()));
        }

        /*
         * Compute the cost to each vertex with an outgoing edge to the target, and from each of
         * their incoming vertices ("neighbors")
         */
        HashMap<Vertex, List<VertexIngress>> neighbors = new HashMap<Vertex, List<VertexIngress>>();
        double maxWWeight = 0;
        double minXWeight = Double.POSITIVE_INFINITY;

        HashSet<Vertex> wSet = new HashSet<Vertex>();
        ArrayList<VertexIngress> ws = new ArrayList<VertexIngress>();
        for (Edge e : graph.getOutgoing(vertex)) {
            if (!isContractable(e)) {
                continue;
            }
            TraverseResult result = e.traverse(state, options);
            Vertex w = e.getToVertex();
            wSet.add(w);
            ws.add(new VertexIngress(w, e, result.weight, result.state.getTime()));
            if (result.weight > maxWWeight) {
                maxWWeight = result.weight;
            }

            for (Edge incoming : graph.getIncoming(w)) {
                
                if (!isContractable(e)) {
                    continue;
                }
                Vertex x = incoming.getFromVertex();
                if (x == vertex) {
                    continue;
                }
                result = incoming.traverse(state, options);
                List<VertexIngress> xneighbors = neighbors.get(x);
                if (xneighbors == null) {
                    xneighbors = new ArrayList<VertexIngress>();
                    neighbors.put(x, xneighbors);
                }
                xneighbors.add(new VertexIngress(w, e, result.weight, result.state.getTime()));
                if (result.weight < minXWeight) {
                    minXWeight = result.weight;
                }
            }
        }

        /* figure out which shortcuts are needed */
        List<Shortcut> shortcuts = new ArrayList<Shortcut>();

        ArrayList<Callable<WitnessSearchResult>> tasks = new ArrayList<Callable<WitnessSearchResult>>(
                us.size());
        
        int nodeLimit = simulate ? 500 : Integer.MAX_VALUE;

        // FOR EACH U
        for (VertexIngress u : us) {
            //allow about a second of inefficiency in routes in the name of planning 
            //efficiency (+ 1)
            double weightLimit = u.weight + maxWWeight - minXWeight + 1;
            WitnessSearch task = new WitnessSearch(vertex, hopLimit, nodeLimit, state, neighbors,
                    weightLimit, wSet, ws, u);
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
                    /* while we're here, remove some extra edges */
                    ArrayList<Edge> toRemove = new ArrayList<Edge>();
                    for (Edge e : graph.getOutgoing(wsresult.vertex)) {
                        SPTVertex s = spt.getVertex(e.getToVertex());
                        if (s == null) {
                            continue;
                        }
                        TraverseResult result = e.traverse(state, options);
                        if (s.weightSum < result.weight) {
                            // the path found by Dijkstra from u to e.tov is better
                            // than the path through e. Therefore e can be deleted.
                            toRemove.add(e);
                        }
                    }

                    GraphVertex ugv = graph.getGraphVertex(wsresult.vertex);

                    for (Edge e : toRemove) {
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
        private Vertex vertex;

        private double weightLimit;

        private HashMap<Vertex, List<VertexIngress>> neighbors;

        private State dummy;

        private int hopLimit;

        private HashSet<Vertex> wSet;

        private List<VertexIngress> ws;

        private VertexIngress u;

        private int nodeLimit;

        public WitnessSearch(Vertex vertex, int hopLimit, int nodeLimit, State dummy,
                HashMap<Vertex, List<VertexIngress>> neighbors, double weightLimit,
                HashSet<Vertex> wSet, List<VertexIngress> ws, VertexIngress u) {
            this.vertex = vertex;
            this.hopLimit = hopLimit;
            this.nodeLimit = nodeLimit;
            this.dummy = dummy;
            this.neighbors = neighbors;
            this.weightLimit = weightLimit;
            this.wSet = wSet;
            this.ws = ws;
            this.u = u;
        }

        public WitnessSearchResult call() {
            return searchWitnesses(vertex, hopLimit, nodeLimit, dummy, neighbors, weightLimit, wSet,
                    ws, u);
        }
    }

    @SuppressWarnings("unchecked")
    private WitnessSearchResult searchWitnesses(Vertex vertex, int hopLimit, int nodeLimit,
            State dummy, HashMap<Vertex, List<VertexIngress>> neighbors, double baseWeightLimit,
            HashSet<Vertex> wSet, List<VertexIngress> ws, VertexIngress u) {
        Dijkstra dijkstra = new Dijkstra(graph, u.vertex, options, vertex, hopLimit - 1);
        dijkstra.setNeighbors(neighbors);

        dijkstra.setTargets((HashSet<String>) wSet.clone());
        BasicShortestPathTree spt = dijkstra.getShortestPathTree(baseWeightLimit, nodeLimit);

        ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();

        // FOR EACH W
        for (VertexIngress w : ws) {

            if (u.vertex == w.vertex) {
                continue;
            }

            double weightLimit = u.weight + w.weight;
            SPTVertex curs = spt.getVertex(w.vertex);

            // if the path around the vertex is longer than the path through,
            // add the path through to the shortcuts
            if (curs == null || curs.weightSum > weightLimit + .01) {
                int time = (int) ((w.time + u.time) / 1000);
                Shortcut duw = new Shortcut(u.edge, w.edge, time, weightLimit);
                shortcuts.add(duw);
            }
        }
        
        return new WitnessSearchResult(shortcuts, spt, u.vertex, spt.size());
    }

    /**
     * Determine whether an edge can be contracted. Presently, we only contract non-time-dependent
     * edges.
     */
    private static boolean isContractable(Edge edge) {
        return (edge instanceof TurnEdge || edge instanceof OutEdge || edge instanceof FreeEdge || edge instanceof Shortcut);
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
    FibHeap<Vertex> initPriorityQueue(Graph graph, int hopLimit) {
        Collection<GraphVertex> vertices = graph.getVertices();
        FibHeap<Vertex> pq = new FibHeap<Vertex>(vertices.size());

        for (GraphVertex gv : vertices) {
            Vertex v = gv.vertex;
            if (!isContractable(v)) {
                continue;
            }
            if (gv.getDegreeIn() > 7 || gv.getDegreeOut() > 7) {
                continue;
            }
            WitnessSearchResult shortcuts = getShortcuts(v, hopLimit, true);
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
                Vertex tov = e.getToVertex();
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

        options = new TraverseOptions(new TraverseModeSet(mode));
        options.optimizeFor = optimize;
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

        int hopLimit = 1;

        FibHeap<Vertex> pq = initPriorityQueue(graph, hopLimit);

        _log.debug("contract");
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

            Vertex vertex = pq.extract_min();

            WitnessSearchResult shortcutsAndSearchSpace;
            // make sure priority of current vertex
            if (pq.empty()) {
                shortcutsAndSearchSpace = getShortcuts(vertex, hopLimit, false);

            } else {
                // resort the priority queue as necessary
                while (true) {
                    shortcutsAndSearchSpace = getShortcuts(vertex, hopLimit, true);
                    int deleted = 0;
                    if (deletedNeighbors.containsKey(vertex)) {
                        deleted = deletedNeighbors.get(vertex);
                    }
                    double new_prio = getImportance(vertex, shortcutsAndSearchSpace, deleted);
                    Double new_min = pq.min_priority();
                    if (new_prio <= new_min) {
                        break;
                    } else {
                        pq.insert(vertex, new_prio);
                        vertex = pq.extract_min();
                    }
                }

                shortcutsAndSearchSpace = getShortcuts(vertex, hopLimit, false);
            }

            if (i % 1000 == 0 || totalVertices - i < 1000) {
                _log.debug("contracted: " + i + " / " + totalVertices + "  total time: "
                        + (System.currentTimeMillis() - start) / 1000.0 + " average degree "
                        + nEdges / (nVertices + 0.00001));
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

            for (Edge ee : graph.getOutgoing(vertex)) {
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

            for (Vertex v : neighbors) {
                int deleted = 0;
                if (deletedNeighbors.containsKey(v)) {
                    deleted = deletedNeighbors.get(v);
                }
                deleted += 1;
                deletedNeighbors.put(v, deleted);

            }

            List<Shortcut> shortcuts = shortcutsAndSearchSpace.shortcuts;
            // Add shortcuts to graph
            for (Shortcut shortcut : shortcuts) {
                graph.addEdge(shortcut.getFromVertex(), shortcut.getToVertex(), shortcut);
            }

            // Update hop limit if necessary, according to the 1235 strategy
            nVertices--;
            nEdges += shortcuts.size();
            if (nVertices == 0) {
                continue;
            }

            float averageDegree = nEdges / nVertices;
            int oldHopLimit = hopLimit;
            if (averageDegree > 10) {
                if (edgesRemoved) {
                    hopLimit = 5;
                } else {
                    hopLimit = 3;
                    int removed = removeNonoptimalEdges(5);
                    nEdges -= removed;
                    edgesRemoved = true;
                }
            } else {
                if (averageDegree > 3.3 && hopLimit == 1) {
                    hopLimit = 2;
                }
            }

            // after a hop limit upgrade, rebuild the priority queue
            if (oldHopLimit != hopLimit) {
                pq = rebuildPriorityQueue(options, hopLimit, deletedNeighbors);
            }
        }
        threadPool = null;
    }

    private void createThreadPool() {
        int nThreads = Runtime.getRuntime().availableProcessors();
        _log.debug("number of threads: " + nThreads);
        ArrayBlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<Runnable>(1000);
        threadPool = new ThreadPoolExecutor(nThreads, nThreads, 10, TimeUnit.SECONDS, taskQueue);
    }

    private FibHeap<Vertex> rebuildPriorityQueue(TraverseOptions options, int hopLimit,
            HashMap<Vertex, Integer> deletedNeighbors) {
        FibHeap<Vertex> newpq = new FibHeap<Vertex>(graph.getVertices().size());
        for (GraphVertex gv : graph.getVertices()) {
            Vertex v = gv.vertex;
            if (!isContractable(v)) {
                continue;
            }
            WitnessSearchResult wsresult = getShortcuts(v, hopLimit, true);
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

        int removed = 0;
        State dummy = new State();
        for (GraphVertex gv : graph.getVertices()) {
            Vertex v = gv.vertex;
            Dijkstra dijkstra = new Dijkstra(graph, v, options, null, hopLimit);
            BasicShortestPathTree spt = dijkstra.getShortestPathTree(Double.POSITIVE_INFINITY,
                    Integer.MAX_VALUE);
            ArrayList<Edge> toRemove = new ArrayList<Edge>();
            for (Edge e : graph.getOutgoing(v)) {
                if (!isContractable(e)) {
                    continue;
                }
                SPTVertex curs = null;
                Vertex toVertex = e.getToVertex();
                curs = spt.getVertex(toVertex);
                TraverseResult result = e.traverse(dummy, options);
                if (curs != null && curs.getParent().getFromVertex().mirror != v
                        && curs.weightSum <= result.weight + .01) {
                    toRemove.add(e);
                }
            }
            for (Edge e : toRemove) {
                gv.removeOutgoing(e);
                graph.getGraphVertex(e.getToVertex()).removeIncoming(e);
            }
            removed += toRemove.size();
        }
        return removed;
    }

    private int countEdges(Graph graph) {
        int total = 0;
        for (GraphVertex gv : graph.getVertices()) {
            total += gv.getDegreeOut();
        }
        return total;
    }

    /**
     * Bidirectional Dijkstra's algorithm with some twists: For forward searches, The search from
     * the target stops when it hits the uncontracted core of the graph and the search from the
     * source continues across the (time-dependent) core.
     * 
     */
    public GraphPath getShortestPath(Vertex origin, Vertex target, State init,
            TraverseOptions options) {

        if (origin == null || target == null) {
            return null;
        }

        Map<Vertex, ArrayList<Edge>> extraEdges = getExtraEdges(origin, target);
        
        LabelBasicShortestPathTree upspt = new LabelBasicShortestPathTree();
        LabelBasicShortestPathTree downspt = new LabelBasicShortestPathTree();

        FibHeap<SPTVertex> upqueue = new FibHeap<SPTVertex>(up.getVertices().size()
                + graph.getVertices().size() + extraEdges.size());
        FibHeap<SPTVertex> downqueue = new FibHeap<SPTVertex>(down.getVertices().size()
                + graph.getVertices().size() + extraEdges.size());

        SPTVertex spt_origin = upspt.addVertex(origin, init, 0, options);
        upqueue.insert(spt_origin, spt_origin.weightSum);

        /* FIXME: insert extra bike walking targets */

        SPTVertex spt_target = downspt.addVertex(target, init, 0, options);
        downqueue.insert(spt_target, spt_target.weightSum);

        // These sets are used not only to avoid revisiting nodes, but to find meetings
        HashSet<Vertex> upclosed = new HashSet<Vertex>();
        HashSet<Vertex> downclosed = new HashSet<Vertex>();

        Vertex meeting = null;
        double bestMeetingCost = Double.POSITIVE_INFINITY;

        boolean done_up = false;
        boolean done_down = false;
        
        while (!(done_up && done_down)) { // Until the priority queue is empty:
            if (!done_up) {
                if (upqueue.empty()) {
                    done_up = true;
                    continue;
                }
                SPTVertex up_u = upqueue.extract_min(); // get the lowest-weightSum

                Vertex fromv = up_u.mirror;
                String fromLabel = fromv.getLabel();

                if (up_u.weightSum > bestMeetingCost) {
                    done_up = true;
                    continue;
                }

                upclosed.add(fromv);

                if (downclosed.contains(fromv)) {
                    double thisMeetingCost = up_u.weightSum + downspt.getVertex(fromv).weightSum;
                    if (thisMeetingCost < bestMeetingCost) {
                        bestMeetingCost = thisMeetingCost;
                        meeting = fromv;
                    }
                    continue;
                }

                GraphVertex fromgv = graph.getGraphVertex(fromLabel);
                if (options.getArriveBy() && fromgv != null) {
                    // up path can only explore until core vertices on reverse paths
                    continue;
                }

                Collection<Edge> outgoing = null;
                if (fromgv != null) {
                    outgoing = fromgv.getOutgoing();
                }
                fromgv = up.getGraphVertex(fromLabel);
                if (fromgv != null) {
                    if (outgoing == null) {
                        outgoing = fromgv.getOutgoing();
                    } else {
                        Collection<Edge> upOutgoing = fromgv.getOutgoing();
                        ArrayList<Edge> newOutgoing = new ArrayList<Edge>(outgoing.size()
                                + upOutgoing.size());
                        newOutgoing.addAll(outgoing);
                        newOutgoing.addAll(upOutgoing);
                        outgoing = newOutgoing;
                    }
                }
                
                if (extraEdges.containsKey(fromv)) {
                    List<Edge> newOutgoing = new ArrayList<Edge>();
                    if (outgoing != null) {
                        for (Edge edge : outgoing)
                            newOutgoing.add(edge);
                    }
                    newOutgoing.addAll(extraEdges.get(fromv));
                    outgoing = newOutgoing;
                }

                State state = up_u.state;
                for (Edge edge : outgoing) {
                    Vertex toVertex = edge.getToVertex();

                    if (upclosed.contains(toVertex)) {
                        continue;
                    }

                    TraverseResult wr = edge.traverse(state, options);

                    // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                    // over.
                    if (wr == null) {
                        continue;
                    }

                    if (wr.weight < 0) {
                        throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge "
                                + edge);
                    }

                    double new_w = up_u.weightSum + wr.weight;
                    if (new_w > options.maxWeight) {
                        //too expensive to get here
                        continue;
                    }
                    if (!options.getArriveBy() && wr.state.getTime() > options.worstTime) {
                        continue;
                    }
                    SPTVertex up_v = upspt.addVertex(toVertex, wr.state, new_w, options);
                    if (up_v != null) {
                        up_v.setParent(up_u, edge);
                        upqueue.insert_or_dec_key(up_v, new_w);
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
                SPTVertex down_u = downqueue.extract_min(); // get the lowest-weightSum

                Vertex tov = down_u.mirror;

                if (down_u.weightSum > bestMeetingCost) {
                    done_down = true;
                    continue;
                }

                String toLabel = tov.getLabel();
                if (upclosed.contains(tov)) {
                    double thisMeetingCost = down_u.weightSum + upspt.getVertex(tov).weightSum;
                    if (thisMeetingCost < bestMeetingCost) {
                        bestMeetingCost = thisMeetingCost;
                        meeting = tov;
                    }
                }

                downclosed.add(tov);
                GraphVertex maingv = graph.getGraphVertex(toLabel);
                if (!options.getArriveBy() && maingv != null) {
                    // down path can only explore until core vertices on forward paths
                    continue;
                }
                GraphVertex downgv = down.getGraphVertex(tov);
                Collection<Edge> incoming = null; 
                if (downgv != null) { 
                    incoming = downgv.getIncoming();
                }
                if (maingv != null) {
                    if (incoming == null) {
                        incoming = maingv.getIncoming();
                    } else {
                        Collection<Edge> mainIncoming = maingv.getIncoming();
                        ArrayList<Edge> newIncoming = new ArrayList<Edge>(incoming.size()
                                + mainIncoming.size());
                        newIncoming.addAll(incoming);
                        newIncoming.addAll(mainIncoming);
                        incoming = newIncoming;
                    }
                }

                if (extraEdges.containsKey(tov)) {
                    List<Edge> newIncoming = new ArrayList<Edge>();
                    if (incoming != null) {
                        for (Edge edge : incoming)
                            newIncoming.add(edge);
                    }
                    newIncoming.addAll(extraEdges.get(tov));
                    incoming = newIncoming;
                }

                State state = down_u.state;
                for (Edge edge : incoming) {
                    Vertex fromVertex = edge.getFromVertex();

                    if (downclosed.contains(fromVertex)) {
                        continue;
                    }

                    TraverseResult wr = edge.traverseBack(state, options);

                    // When an edge leads nowhere (as indicated by returning NULL), the iteration is
                    // over.
                    if (wr == null) {
                        continue;
                    }

                    if (wr.weight < 0) {
                        throw new NegativeWeightException(String.valueOf(wr.weight) + " on edge "
                                + edge);
                    }

                    double new_w = down_u.weightSum + wr.weight;
                    if (new_w > options.maxWeight) {
                        //too expensive to get here
                        continue;
                    }
                    if (options.getArriveBy() && wr.state.getTime() < options.worstTime) {
                        continue;
                    }
                    SPTVertex down_v = downspt.addVertex(fromVertex, wr.state, new_w, options);
                    if (down_v != null) {
                        down_v.setParent(down_u, edge);
                        downqueue.insert_or_dec_key(down_v, new_w);
                    }
                }
            }
        }

        if (meeting == null) {
            return null;
        }
        /* merge spts into path */
        // GET AND JOIN PATHS TO MEETUP VERTEX
        GraphPath upPath = upspt.getPath(meeting);
        GraphPath downPath = downspt.getPath(meeting);

        GraphPath path = new GraphPath();
        path.edges.addAll(upPath.edges);

        ListIterator<SPTEdge> it = downPath.edges.listIterator(downPath.edges.size());
        while (it.hasPrevious()) {
            SPTEdge e = it.previous();
            SPTVertex swap = e.tov;
            e.tov = e.fromv;
            e.fromv = swap;
            path.edges.add(e);
        }

        if (path.edges == null) {
            return null;
        }

        path.edges = flatten(path.edges);
        // clean up edges & vertices
        if (options.getArriveBy()) {
            cleanPathEdgesBack(init, path, options);
        } else {
            cleanPathEdges(init, path, options);
        }
        return path;
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
            Iterable<Edge> extra = ((StreetLocation)target).getExtra();
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

    private void cleanPathEdges(State init, GraphPath path, TraverseOptions options) {

        State state = init;
        double totalWeight = 0;
        SPTVertex prevVertex = path.edges.firstElement().getFromVertex();

        for (SPTEdge e : path.edges) {
            e.fromv = prevVertex;

            e.fromv.state = state;
            e.fromv.weightSum = totalWeight;
            path.vertices.add(e.fromv);

            TraverseResult result = e.traverse(state, options);

            state = result.state;
            SPTVertex tov = e.getToVertex();
            tov.state = state;
            totalWeight += result.weight;
            tov.weightSum = totalWeight;
            prevVertex = tov;
        }
        path.vertices.add(path.edges.lastElement().getToVertex());
    }

    private void cleanPathEdgesBack(State init, GraphPath path, TraverseOptions options) {

        State state = init;
        double totalWeight = 0;

        Collections.reverse(path.edges);

        SPTVertex prevVertex = path.edges.firstElement().getToVertex();

        for (SPTEdge e : path.edges) {
            e.tov = prevVertex;

            e.tov.state = state;
            e.tov.weightSum = totalWeight;
            path.vertices.add(e.tov);

            TraverseResult result = e.traverseBack(state, options);

            state = result.state;
            SPTVertex fromv = e.getFromVertex();
            fromv.state = state;
            totalWeight += result.weight;
            fromv.weightSum = totalWeight;
            prevVertex = fromv;
        }
        path.vertices.add(path.edges.lastElement().getFromVertex());

        Collections.reverse(path.edges);
        Collections.reverse(path.vertices);
    }

    private Vector<SPTEdge> flatten(List<SPTEdge> edges) {
        Vector<SPTEdge> out = new Vector<SPTEdge>();
        if (edges.size() == 0) {
            return out;
        }
        TraverseOptions options = edges.get(0).fromv.options;
        for (SPTEdge edge : edges) {

            if (edge.payload instanceof Shortcut) {
                SPTVertex last = edge.getFromVertex();
                Shortcut shortcut = (Shortcut) edge.payload;
                for (Edge e : flatten(shortcut)) {
                    SPTVertex next = new SPTVertex(e.getToVertex(), null, 0, options);
                    out.add(new SPTEdge(last, next, e));
                    last = next;
                }
                out.lastElement().tov = edge.getToVertex();
            } else {
                out.add(edge);
            }
        }
        return out;
    }

    private ArrayList<Edge> flatten(Shortcut shortcut) {
        ArrayList<Edge> out = new ArrayList<Edge>();
        if (shortcut.edge1 instanceof Shortcut) {
            out.addAll(flatten((Shortcut) shortcut.edge1));
        } else {
            out.add(shortcut.edge1);
        }
        if (shortcut.edge2 instanceof Shortcut) {
            out.addAll(flatten((Shortcut) shortcut.edge2));
        } else {
            out.add(shortcut.edge2);
        }
        return out;
    }
}
