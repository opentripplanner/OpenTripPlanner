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

package org.opentripplanner.graph_builder.impl.reach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.customize.ClassCustomizer;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.reach.EdgeWithReach;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilder} plugin that computes the reach of every street node
 * 
 */
public class ReachComputerGraphBuilderImpl implements GraphBuilder {

    private double initialStreetEpsilon = 1200;

    private double maxStreetEpsilon = 1200;

    private double streetEpsilonMultiplier = 2;

    private int edgeTreesVertexLimit = 200;

    private static Logger log = LoggerFactory.getLogger(ReachComputerGraphBuilderImpl.class);

    double epsilon;

    Map<Vertex, Double> inPenalty = new HashMap<Vertex, Double>();

    Map<Vertex, Double> outPenalty = new HashMap<Vertex, Double>();

    public List<String> provides() {
        return Arrays.asList("reach");
    }
    
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }
    
    /** For the first run of the partial trees phase, the reach cut-off */
    public void setInitialStreetEpsilon(double epsilon) {
        this.initialStreetEpsilon = epsilon;
    }

    /** The max reach for the partial trees phase */
    public void setMaxStreetEpsilon(double epsilon) {
        this.maxStreetEpsilon = epsilon;
    }

    /** How much the reach is increased for each run of the partial trees phase */
    public void streetEpsilonMultiplier(double multiplier) {
        this.streetEpsilonMultiplier = multiplier;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        TraverseOptions options = new TraverseOptions(TraverseMode.WALK);
        options.walkReluctance = 1;
        options.setWalkSpeed(1);

        addReachToGraph(graph);

        OverlayGraph walkGraph = makeWalkingGraph(graph, options);

        Set<Vertex> streetVertices = getWalkingVertices(walkGraph, options);

        options.setArriveBy(true);
        removeEdgeTrees(walkGraph, streetVertices, options);

        options.setArriveBy(false);
        removeEdgeTrees(walkGraph, streetVertices, options);

        partialTreesForStreets(walkGraph, streetVertices, options);

        // clean up to save memory
        inPenalty = outPenalty = null;
    }

    private void addReachToGraph(Graph graph) {

        HashMap<Class<? extends Edge>, Class<? extends Edge>> classMapping = new HashMap<Class<? extends Edge>, Class<? extends Edge>>();

        HashSet<Edge> edgesToRemove = new HashSet<Edge>();
        HashSet<Edge> edgesToAdd = new HashSet<Edge>();
        for (Vertex vertex : graph.getVertices()) {
            for (Edge e : vertex.getOutgoing()) {
                if (e instanceof StreetEdge) {
                    StreetEdge se = ((StreetEdge) e);
                    edgesToRemove.add(se);
                    edgesToAdd.add(reachifyEdge(graph, classMapping, se));
                }
            }
            for (Edge e : vertex.getIncoming()) {
                if (edgesToRemove.contains(e)) {
                    continue;
                }
                if (e instanceof StreetEdge) {
                    StreetEdge se = ((StreetEdge) e);
                    edgesToRemove.add(se);
                    edgesToAdd.add(reachifyEdge(graph, classMapping, se));
                }
            }
        }
        for (Edge e: edgesToRemove) {
            e.detach();
        }

// should no longer be necessary        
//        for (Edge e: edgesToAdd) {
//            graph.addVerticesFromEdge(e);
//        }
    }

    @SuppressWarnings("unchecked")
    private Edge reachifyEdge(Graph graph, HashMap<Class<? extends Edge>, Class<? extends Edge>> classMapping, Edge e) {
        Class<? extends Edge> oldClass = e.getClass();
        Class<? extends Edge> newClass = classMapping.get(oldClass);

        if (newClass == null) {
            String newClassName = oldClass.getName() + "WithReach";
            ClassCustomizer customizer = new ClassCustomizer(EdgeWithReach.class, oldClass.getName(),
                    newClassName);
            customizer.setClassPath(graph.getBundle().getExtraClassPath());
            customizer.addDoubleField("reach");
            // create the new class
            
            newClass = (Class<? extends Edge>) customizer.saveClass();
            classMapping.put(oldClass, newClass);
        }
        EdgeWithReach newEdge = (EdgeWithReach) ClassCustomizer.reclass(e, newClass);
        newEdge.setReach(Double.MAX_VALUE);
        return newEdge;
    }

    /**
     * Returns just the non-transit portion of the graph, ignoring any transit
     * @param graph
     * @param options a set of TraverseOptions with modes containing TraverseMode.WALK or BICYCLE or CAR 
     * @return
     */
    private OverlayGraph makeWalkingGraph(Graph graph, TraverseOptions options) {
        OverlayGraph newGraph = new OverlayGraph(graph);
        for (Vertex vertex : graph.getVertices()) {
            options.setArriveBy(false);
            for (Edge e : vertex.getOutgoing()) {
                if (e instanceof StreetEdge || e instanceof StreetTransitLink) {
                    if (e.traverse(new State(vertex, options)) != null) {
                        newGraph.addOutgoing(vertex, e);
                    }
                }
            }
            options.setArriveBy(true);
            for (Edge e : vertex.getIncoming()) {
                if (e instanceof StreetEdge || e instanceof StreetTransitLink) {
                    if (e.traverse(new State(vertex, options)) != null) {
                        newGraph.addIncoming(vertex, e);
                    }
                }
            }
        }
        return newGraph;
    }

    /**
     * This step computes the reach for (and removes) small subgraphs that have only one way to
     * access the rest of the graph. The definition of "small subgraphs" is a bit funny because of
     * the edge-based nature of the graph. The challenge is that any loop in the subgraph allows
     * Dijkstra to escape the subgraph without passing through the initial vertex.
     * 
     * This actually alters the graph to remove the trees, so make sure you pass in a graph you can
     * safely trash.
     * 
     * @param graph
     * @param streetVertices
     * @param options
     */
    private void removeEdgeTrees(OverlayGraph ograph, Set<Vertex> streetVertices, TraverseOptions options) {
        GenericDijkstra dijkstra = new GenericDijkstra(options);
        dijkstra.setShortestPathTreeFactory(new ShortestPathTreeFactory() {
            @Override
            public ShortestPathTree create() {
                ReachMiniSPT spt = new ReachMiniSPT(getEdgeTreesVertexLimit() * 2, Double.MAX_VALUE);
                return spt;
            }
        });

        int i = 0;
        int removed = 0;
        for (Vertex v : streetVertices) {
            i++;
            if (i % 5000 == 0) {
                log.info("Removing edge trees : " + i + " / " + streetVertices.size() + " ("
                        + removed + " edges removed)");
            }
            EdgeTreesPhaseTerminationCondition termination = new EdgeTreesPhaseTerminationCondition(
                    getEdgeTreesVertexLimit());
            dijkstra.setSearchTerminationStrategy(termination);
            dijkstra.setSkipTraverseResultStrategy(new EdgeTreesSkipTraversalResultStrategy(v));
            ReachMiniSPT spt = (ReachMiniSPT) dijkstra.getShortestPathTree(new ReachState(v,
                    options));
            if (termination.getUnsafeTermination()) {
                // this vertex does not lead into a subgraph of size < EDGE_TREES_VERTEX_LIMIT,
                // or leads to a transit vertex
                continue;
            }
            // compute reach for this the edges that lead directly out of this vertex

            ArrayList<EdgeWithReach> edgesToRemove = new ArrayList<EdgeWithReach>();

            Collection<Edge> edges;
            if (options.isArriveBy()) {
                edges = ograph.getIncoming(v);
            } else {
                edges = ograph.getOutgoing(v);
            }

            for (EdgeWithReach e : IterableLibrary.filter(edges, EdgeWithReach.class)) {
                double reach = spt.getHeight(spt.getState(v), outPenalty);
                MapUtils.addToMaxMap(inPenalty, v, reach);
                MapUtils.addToMaxMap(outPenalty, v, reach);
                e.setReach(reach);
                edgesToRemove.add(e);
            }
            for (EdgeWithReach e : edgesToRemove) {
                ograph.removeEdge(e);
                removed++;
            }
        }
    }

    void partialTreesForStreets(OverlayGraph ograph, Collection<Vertex> streetVertices,
            TraverseOptions options) {

        /* compute the walking graph */

        HashSet<Edge> added = new HashSet<Edge>();
        OverlayGraph workingGraph = new OverlayGraph();
        for (Vertex v : streetVertices) {
            for (Edge e : v.getOutgoing()) {
                if (e instanceof EdgeWithReach) {
                    if (!added.contains(e)) {
                        added.add(e);
                        workingGraph.addEdge(e);
                    }
                }
            }
            for (Edge e : v.getIncoming()) {
                if (e instanceof EdgeWithReach) {
                    if (!added.contains(e)) {
                        added.add(e);
                        workingGraph.addEdge(e);
                    }
                }
            }
        }
        added.clear();
        ograph = workingGraph;

        /* compute reach over all walking paths */
        epsilon = initialStreetEpsilon;
        while (epsilon <= maxStreetEpsilon) {
            ograph = partialTreesPhase(ograph, streetVertices, options, false);
            epsilon *= streetEpsilonMultiplier;
        }
    }

    private Set<Vertex> getWalkingVertices(OverlayGraph ograph, TraverseOptions options) {
        Set<Vertex> streetVertices = new HashSet<Vertex>();
        for (Vertex vertex : ograph.getVertices()) {
            if (vertex instanceof TurnVertex) {
                GenericDijkstra dijkstra = new GenericDijkstra(options, ograph);
                ShortestPathTree spt = dijkstra.getShortestPathTree(new State(vertex, options));
                for (State s : spt.getAllStates()) {
                    Vertex v2 = s.getVertex();
                    if (v2 instanceof TurnVertex || v2 instanceof IntersectionVertex) {
                        streetVertices.add(s.getVertex());
                    }
                }
                options.setArriveBy(true);
                spt = dijkstra.getShortestPathTree(new State(vertex, options));
                for (State s : spt.getAllStates()) {
                    Vertex v2 = s.getVertex();
                    if (v2 instanceof TurnVertex || v2 instanceof IntersectionVertex) {
                        streetVertices.add(s.getVertex());
                    }
                }
                options.setArriveBy(false);
                break;
            }
        }
        return streetVertices;
    }

    OverlayGraph partialTreesPhase(final OverlayGraph oGraph, Collection<Vertex> streetVertices,
        final TraverseOptions options, boolean transitStops) {
        
        log.info("Partial trees phase at epsilon = " + epsilon);

        GenericDijkstra dijkstra = new GenericDijkstra(options, oGraph);
        if (!transitStops) {
            dijkstra.setSearchTerminationStrategy(new PartialTreesPhaseTerminationCondition());
        }
        dijkstra.setShortestPathTreeFactory(new ShortestPathTreeFactory() {
            @Override
            public ShortestPathTree create() {
                ReachMiniSPT spt = new ReachMiniSPT(epsilon);
                return spt;
            }
        });

        dijkstra.setPriorityQueueFactory(new OTPPriorityQueueFactory() {
            @SuppressWarnings("unchecked")
            public OTPPriorityQueue<State> create(int maxSize) {
                int size = (int) epsilon;
                int graphSize = oGraph.getVertices().size();
                if (graphSize < size) {
                    size = graphSize;
                }
                OTPPriorityQueue<State> pq = new BinHeap<State>(size);
                return pq;
            }
        });

        HashMap<Edge, Double> reachEstimateForEdge = new HashMap<Edge, Double>();
        int i = 0;
        long dijkstraTime = 0;
        long heightTime = 0;
        for (final Vertex v : streetVertices) {
            i += 1;
            final ReachState initialState = new ReachState(v, options);
            long startTime = System.currentTimeMillis();

            ReachMiniSPT spt = (ReachMiniSPT) dijkstra.getShortestPathTree(initialState);
            spt.computeChildren();

            long endTime = System.currentTimeMillis();
            dijkstraTime += endTime - startTime;

            if (i % 100 == 0) {
                log.info("partial trees: " + i + " / " + streetVertices.size() + " dij time "
                        + (dijkstraTime / 1000.0) + " height time " + (heightTime / 1000.0));
                dijkstraTime = 0;
                heightTime = 0;
            }

            /*
             * find all reaches less than epsilon
             */

            for (ReachState state : spt.getInnerCircle()) {
                for (ReachState child : state.getChildren()) {

                    Edge edge = child.getBackEdge();
                    if (edge == null) {
                        continue;
                    }

                    Double edgeInPenalty = inPenalty.get(v);
                    if (edgeInPenalty == null) {
                        edgeInPenalty = 0.0;
                    }

                    double depth = child.getWeight() + edgeInPenalty;
                    startTime = System.currentTimeMillis();
                    double height = spt.getHeight(child, outPenalty) + child.getWeightDelta();
                    endTime = System.currentTimeMillis();
                    heightTime += endTime - startTime;
                    double reach = depth;
                    if (reach > height) {
                        reach = height;
                    }

                    MapUtils.addToMaxMap(reachEstimateForEdge, edge, reach);
                }
            }
        }

        // eliminate edges which have reach less than epsilon

        OverlayGraph newGraph = new OverlayGraph();

        int edgesRemoved = 0;
        HashSet<Edge> edges = new HashSet<Edge>();
        for (Vertex v : oGraph.getVertices()) {
            for (EdgeWithReach edge : IterableLibrary.filter(v.getOutgoing(),
                    EdgeWithReach.class)) {
                if (!edges.contains(edge)) {
                    edgesRemoved += processEdge(newGraph, edge, reachEstimateForEdge, transitStops);
                    edges.add(edge);
                }
            }
            for (EdgeWithReach edge : IterableLibrary.filter(v.getIncoming(),
                    EdgeWithReach.class)) {
                if (!edges.contains(edge)) {
                    edgesRemoved += processEdge(newGraph, edge, reachEstimateForEdge, transitStops);
                    edges.add(edge);
                }
            }
        }
        log.info("Removed " + edgesRemoved + " / " + edges.size() + " edges");
        return newGraph;
    }

    private int processEdge(OverlayGraph newGraph, EdgeWithReach edge,
            HashMap<Edge, Double> reachEstimateForEdge, boolean transitStops) {
        Double reach = reachEstimateForEdge.get(edge);
        if (reach == null) {
            newGraph.addEdge(edge);
            return 0;
        }

        if (reach >= epsilon) {
            newGraph.addEdge(edge);
            return 0;
        }

        if (edge.getReach() > reach) {
            edge.setReach(reach);
        }
        // extend the out penalty for tov;
        Vertex fromv = edge.getFromVertex();
        Vertex tov = edge.getToVertex();
        MapUtils.addToMaxMap(outPenalty, tov, reach);
        MapUtils.addToMaxMap(inPenalty, fromv, reach);
        return 1;
    }

    public int getEdgeTreesVertexLimit() {
        return edgeTreesVertexLimit;
    }

    public void setEdgeTreesVertexLimit(int edgeTreesVertexLimit) {
        this.edgeTreesVertexLimit = edgeTreesVertexLimit;
    }

}
