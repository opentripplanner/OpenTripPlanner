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
        Map<Vertex, ArrayList<Vertex>> neighborsForVertex = new HashMap<Vertex, ArrayList<Vertex>>();

//        RoutingRequest options = new RoutingRequest(new TraverseModeSet(TraverseMode.WALK, TraverseMode.TRANSIT));
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

        ArrayList<Subgraph> islands = new ArrayList<Subgraph>();
        /* associate each node with a subgraph */
        for (Vertex gv : graph.getVertices()) {
            if (!(gv instanceof StreetVertex)) {
                continue;
            }
            Vertex vertex = gv;
            if (subgraphs.containsKey(vertex)) {
                continue;
            }
            if (!neighborsForVertex.containsKey(vertex)) {
                continue;
            }
            Subgraph subgraph = computeConnectedSubgraph(neighborsForVertex, vertex);
            if (subgraph != null){
                for (Iterator<Vertex> vIter = subgraph.streetIterator(); vIter.hasNext();) {
                    Vertex subnode = vIter.next();
                    subgraphs.put(subnode, subgraph);
                }
                islands.add(subgraph);
            }
        }
        LOG.info(islands.size() + " sub graphs found");
        /* remove all tiny subgraphs and large subgraphs without stops */
        for (Subgraph island : islands) {
            boolean hadRemoved = false;
            if(island.stopSize() > 0){
            //for islands with stops
                if (island.streetSize() < islandWithStopMaxSize) {
                    depedestrianizeOrRemove(graph, island);
                    hadRemoved = true;
                }
            }else{
            //for islands without stops
                if (island.streetSize() < maxIslandSize) {
                    depedestrianizeOrRemove(graph, island);
                    hadRemoved = true;
                }
            }
            if (islandLog != null) {
                WriteNodesInSubGraph(island, islandLog, hadRemoved);
            }
        }
        if (graph.removeEdgelessVertices() > 0) {
            LOG.warn("Removed edgeless vertices after pruning islands");
        }
    }

    private static void depedestrianizeOrRemove(Graph graph, Subgraph island) {
        //iterate over the street vertex of the subgraph
        for (Iterator<Vertex> vIter = island.streetIterator(); vIter.hasNext();) {
            Vertex v = vIter.next();
            Collection<Edge> outgoing = new ArrayList<Edge>(v.getOutgoing());
            for (Edge e : outgoing) {
                if (e instanceof StreetEdge) {
                    StreetEdge pse = (StreetEdge) e;
                    StreetTraversalPermission permission = pse.getPermission();
                    permission = permission.remove(StreetTraversalPermission.PEDESTRIAN);
                    permission = permission.remove(StreetTraversalPermission.BICYCLE);
                    if (permission == StreetTraversalPermission.NONE) {
                        graph.removeEdge(pse);
                    } else {
                        pse.setPermission(permission);
                    }
                }
            }
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
            Map<Vertex, ArrayList<Vertex>> neighborsForVertex, Vertex startVertex) {
        Subgraph subgraph = new Subgraph();
        Queue<Vertex> q = new LinkedList<Vertex>();
        q.add(startVertex);
        while (!q.isEmpty()) {
            Vertex vertex = q.poll();
            for (Vertex neighbor : neighborsForVertex.get(vertex)) {
                if (!subgraph.contains(neighbor)) {
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