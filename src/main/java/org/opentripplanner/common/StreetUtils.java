
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

package org.opentripplanner.common;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import org.opentripplanner.common.geometry.Subgraph;
import org.opentripplanner.graph_builder.annotation.GraphConnectivity;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;

public class StreetUtils {

    private static Logger LOG = LoggerFactory.getLogger(StreetUtils.class);
    private static int islandCounter = 0;

    /* Island pruning strategy:
       1. Extract islands without using noThruTraffic edges at all
       2. Then create expanded islands by accepting noThruTraffic edges, but do not jump across original islands!
          Note: these expanded islands can overlap.
       3  Relax connectivity even more: generate islands by allowing jumps between islands. Find out unreachable edges of small islands.
       4. Analyze small expanded islands (from step 2). Convert edges which are reachable only via noThruTraffic edges
          to noThruTraffic state. Depedestrianize unreachable edges. Removed unconnected edges.
     */

    public static void pruneFloatingIslands(Graph graph, int maxIslandSize,
            int islandWithStopMaxSize, String islandLogName) {
        LOG.debug("pruning");
        PrintWriter islandLog = null;
        if (islandLogName != null && !islandLogName.isEmpty()) {
            try {
                islandLog = new PrintWriter(new File(islandLogName));
            } catch (Exception e) {
                LOG.error("Failed to write islands log file", e);
            }
        }
        if (islandLog != null) {
            islandLog.printf("%s\t%s\t%s\t%s\t%s\n","id","stopCount", "streetCount","wkt" ,"hadRemoved");
        }
        Map<Vertex, Subgraph> subgraphs = new HashMap<Vertex, Subgraph>();
        Map<Vertex, Subgraph> extgraphs = new HashMap<Vertex, Subgraph>();
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<Vertex, ArrayList<Vertex>>();
        Map<Edge, Boolean> isolated = new HashMap<Edge, Boolean>();
        ArrayList<Subgraph> islands = new ArrayList<Subgraph>();
        int count;

        /* establish vertex neighbourhood without noThruTrafficEdges */
        collectNeighbourVertices(graph, neighborsForVertex, false);

        /* associate each connected vertex with a subgraph */
        count = collectSubGraphs(graph, neighborsForVertex, subgraphs, null, null);
        LOG.info("Islands without noThruTraffic edges: " + count);

        /* Expand vertex neighbourhood with noThruTrafficEdges
           Note that we can reuse the original neighbour map here
           and simply process a smaller set of noThruTrafficEdges */
        collectNeighbourVertices(graph, neighborsForVertex, true);

        /* Next: generate subgraphs without considering access limitations */
        count = collectSubGraphs(graph, neighborsForVertex, extgraphs, null, islands);
        LOG.info("Islands with noThruTraffic edges: " + count);

        /* collect unreachable edges to a map */
        processIslands(graph, islands, isolated, null, true, maxIslandSize, islandWithStopMaxSize);

        extgraphs = new HashMap<Vertex, Subgraph>(); // let old map go
        islands = new ArrayList<Subgraph>(); // reset this too

        /* Recompute expanded subgraphs by accepting noThruTraffic edges in graph expansion.
           However, expansion is not allowed to jump from an original island to another one
         */
        collectSubGraphs(graph, neighborsForVertex, extgraphs, subgraphs, islands);

        /* Next round: generate purely noThruTraffic islands if such ones exist */
        count = collectSubGraphs(graph, neighborsForVertex, extgraphs, null, islands);
        LOG.info("noThruTraffic island count: " + count);

        LOG.info("Total " + islands.size() + " sub graphs found");

        /* remove all tiny subgraphs and large subgraphs without stops */
        count = processIslands(graph, islands, isolated, islandLog, false, maxIslandSize, islandWithStopMaxSize);
        LOG.info("Modified " + count + " islands");

        if (graph.removeEdgelessVertices() > 0) {
            LOG.warn("Removed edgeless vertices after pruning islands");
        }
    }

    private static int processIslands(
        Graph graph, ArrayList<Subgraph> islands,
        Map<Edge, Boolean> isolated, PrintWriter log,
        boolean markIsolated, int maxIslandSize, int islandWithStopMaxSize) {

        Map<String, Integer> stats = new HashMap<String, Integer>();

        stats.put("isolated", 0);
        stats.put("removed", 0);
        stats.put("noThru", 0);
        stats.put("depedestrianized",0);

        int count = 0;
        for (Subgraph island : islands) {
            boolean hadRemoved = false;
            if(island.stopSize() > 0){
                //for islands with stops
                if (island.streetSize() < islandWithStopMaxSize) {
                    depedestrianizeOrRemove(graph, island, isolated, stats, markIsolated);
                    hadRemoved = true;
                    count++;
                }
            }else{
                //for islands without stops
                if (island.streetSize() < maxIslandSize) {
                    depedestrianizeOrRemove(graph, island, isolated, stats, markIsolated);
                    hadRemoved = true;
                    count++;
                }
            }
            if (log != null) {
                WriteNodesInSubGraph(island, log, hadRemoved);
            }
        }
        if (markIsolated) {
            LOG.info("Detected " + stats.get("isolated") + " isolated edges");
        } else {
            LOG.info("Removed " + stats.get("removed") + " edges");
            LOG.info("Depedestrianized " + stats.get("depedestrianized") + " edges");
            LOG.info("Converted " + stats.get("noThru") + " edges to noTruTraffic");
        }
        return count;
    }

    private static void collectNeighbourVertices(
        Graph graph, Map<Vertex, ArrayList<Vertex>> neighborsForVertex, boolean noThruTraffic) {

        // RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK, TraverseMode.TRANSIT));
        RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK));

        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            State s0 = new State(gv, options);
            for (Edge e : gv.getOutgoing()) {
                Vertex in = gv;
                if (!(e instanceof StreetEdge || e instanceof StreetTransitLink ||
                      e instanceof ElevatorEdge || e instanceof FreeEdge)) {
                    continue;
                }
                if ((e instanceof StreetEdge && ((StreetEdge)e).isNoThruTraffic()) != noThruTraffic) {
                    continue;
                }
                State s1 = e.traverse(s0);
                if (s1 == null) {
                    continue;
                }
                Vertex out = s1.getVertex();

                ArrayList<Vertex> vertexList = neighborsForVertex.get(in);
                if (vertexList == null) {
                    vertexList = new ArrayList<Vertex>();
                    neighborsForVertex.put(in, vertexList);
                }
                vertexList.add(out);

                vertexList = neighborsForVertex.get(out);
                if (vertexList == null) {
                    vertexList = new ArrayList<Vertex>();
                    neighborsForVertex.put(out, vertexList);
                }
                vertexList.add(in);
            }
        }
    }

    private static int collectSubGraphs(
        Graph graph,
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex,
        Map<Vertex, Subgraph> newgraphs, // put new subgraphs here
        Map<Vertex, Subgraph> subgraphs, // optional isolation map from a previous round
        ArrayList<Subgraph> islands) {   // final list of islands or null

        int count=0;
        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            Vertex vertex = gv;

            if (subgraphs != null && !subgraphs.containsKey(vertex)) {
                // do not start new graph generation from non-classified vertex
                continue;
            }
            if (newgraphs.containsKey(vertex)) { // already processed
                continue;
            }
            if (!neighborsForVertex.containsKey(vertex)) {
                continue;
            }
            Subgraph subgraph = computeConnectedSubgraph(neighborsForVertex, vertex, subgraphs);
            if (subgraph != null){
                for (Iterator<Vertex> vIter = subgraph.streetIterator(); vIter.hasNext();) {
                    Vertex subnode = vIter.next();
                    newgraphs.put(subnode, subgraph);
                }
                if (islands != null) {
                    islands.add(subgraph);
                }
                count++;
            }
        }
        return count;
    }

    private static void depedestrianizeOrRemove(
        Graph graph, Subgraph island,
        Map<Edge, Boolean> isolated, Map<String, Integer> stats, boolean markIsolated) {
        //iterate over the street vertex of the subgraph
        for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            Collection<Edge> outgoing = new ArrayList<Edge>(v.getOutgoing());
            for (Edge e : outgoing) {
                if (e instanceof StreetEdge) {
                    if (markIsolated) {
                        isolated.put(e, true);
                        stats.put("isolated", stats.get("isolated") + 1);
                    } else {
                        StreetEdge pse = (StreetEdge) e;
                        if (!isolated.containsKey(e)) {
                            // not a true island edge but has limited access
                            // so convert to noThruTraffic
                            pse.setNoThruTraffic(true);
                            stats.put("noThru", stats.get("noThru") + 1);
                        } else {
                            StreetTraversalPermission permission = pse.getPermission();
                            permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                            permission = permission.remove(StreetTraversalPermission.BICYCLE);
                            if (permission == StreetTraversalPermission.NONE) {
                                graph.removeEdge(pse);
                                stats.put("removed", stats.get("removed") + 1);
                            } else {
                                pse.setPermission(permission);
                                stats.put("depedestrianized", stats.get("depedestrianized") + 1);
                            }
                        }
                    }
                }
            }
        }
        if (markIsolated) {
            return;
        }
        for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            if (v.getDegreeOut() + v.getDegreeIn() == 0) {
                graph.remove(v);
            }
        }
        //remove street conncetion form
        for (Iterator<Vertex> vIter = island.stopIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            Collection<Edge> edges = new ArrayList<Edge>(v.getOutgoing());
            edges.addAll(v.getIncoming());
            for (Edge e : edges) {
                if (e instanceof StreetTransitLink) {
                    graph.removeEdge(e);
                }
            }
        }
        LOG.debug(graph.addBuilderAnnotation(new GraphConnectivity(island.getRepresentativeVertex(), island.streetSize())));
    }

    private static Subgraph computeConnectedSubgraph(
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex, Vertex startVertex, Map<Vertex, Subgraph> anchors) {
        Subgraph subgraph = new Subgraph();
        Queue<Vertex> q = new LinkedList<Vertex>();
        Subgraph anchor = null;

        if (anchors != null) {
            // anchor subgraph expansion to this subgraph
            anchor = anchors.get(startVertex);
        }
        q.add(startVertex);
        while (!q.isEmpty()) {
            Vertex vertex = q.poll();
            for (Vertex neighbor : neighborsForVertex.get(vertex)) {
                if (!subgraph.contains(neighbor)) {
                    if (anchor != null) {
                        Subgraph compare = anchors.get(neighbor);
                        if ( compare != null && compare != anchor) { // do not enter a new island
                            continue;
                        }
                    }
                    subgraph.addVertex(neighbor);
                    q.add(neighbor);
                }
            }
        }
        return subgraph;
//        if(subgraph.size()>1) return subgraph;
//        return null;
    }

    private static void WriteNodesInSubGraph(Subgraph subgraph, PrintWriter islandLog, boolean hadRemoved){
        Geometry convexHullGeom = subgraph.getConvexHull();
        if (convexHullGeom != null && !(convexHullGeom instanceof Polygon)) {
            convexHullGeom = convexHullGeom.buffer(0.0001,5);
        }
        islandLog.printf("%d\t%d\t%d\t%s\t%b\n", islandCounter, subgraph.stopSize(), 
                subgraph.streetSize(), convexHullGeom, hadRemoved);
        islandCounter++;
    }
}
