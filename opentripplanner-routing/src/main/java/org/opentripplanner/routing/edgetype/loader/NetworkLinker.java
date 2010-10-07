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

package org.opentripplanner.routing.edgetype.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Map.Entry;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkLinker {

    private static Logger _log = LoggerFactory.getLogger(NetworkLinker.class);

    private Graph graph;

    HashMap<HashSet<Edge>, LinkedList<P2<PlainStreetEdge>>> replacements;

    private GeometryFactory geometryFactory;

    private StreetVertexIndexServiceImpl index;

    public NetworkLinker(Graph graph) {
        replacements = new HashMap<HashSet<Edge>, LinkedList<P2<PlainStreetEdge>>>();
        this.geometryFactory = new GeometryFactory();
        this.graph = graph;
    }

    /**
     * Link the transit network to the street network. Connect each transit vertex to the nearest
     * Street edge with a StreetTransitLink.
     * @param index 
     */
    public void createLinkage() {

        _log.debug("constructing index...");
        index = new StreetVertexIndexServiceImpl(graph);
        index.setup_modifiable();

        _log.debug("creating linkages...");
        int i = 0;
        ArrayList<GraphVertex> vertices = new ArrayList<GraphVertex>(graph.getVertices());

        for (GraphVertex gv : vertices) {
            Vertex v = gv.vertex;
            if (i % 500 == 0)
                _log.debug("vertices=" + i + "/" + vertices.size());
            i++;

            if (v instanceof TransitStop) {
                // only connect transit stops that (a) are entrances, or (b) have no associated
                // entrances
                TransitStop ts = (TransitStop) v;
                if (!ts.isEntrance()) {
                    boolean hasEntrance = false;

                    for (Edge e : gv.getOutgoing()) {
                        if (e instanceof PathwayEdge) {
                            hasEntrance = true;
                            break;
                        }
                    }
                    if (hasEntrance) {
                        // transit stop has entrances
                        continue;
                    }
                }

                Vertex location = getLocation(v, index);
                if (location == null) {
                    _log.warn("Stop " + ts + " not near any streets; it will not be usable");
                } else {
                    boolean wheelchairAccessible = ts.hasWheelchairEntrance();
                    graph.addEdge(new StreetTransitLink(location, v, wheelchairAccessible));
                    graph.addEdge(new StreetTransitLink(v, location, wheelchairAccessible));
                }
            }
        }
        /* insert newly created edges into the graph */
        for (Entry<HashSet<Edge>, LinkedList<P2<PlainStreetEdge>>> entry : replacements.entrySet()) {
            LinkedList<P2<PlainStreetEdge>> edges = entry.getValue();
            for (P2<PlainStreetEdge> edge : edges) {
                PlainStreetEdge e1 = edge.getFirst();
                graph.addEdge(e1);
                graph.addEdge(edge.getSecond());
            }
        }
    }

    private Vertex getLocation(Vertex v, StreetVertexIndexServiceImpl index) {
        Coordinate coordinate = v.getCoordinate();
        /* right at an intersection? */
        List<Vertex> atIntersection = index.getIntersectionAt(coordinate);
        if (atIntersection != null) {
            /* create a vertex linked to all vertices at intersection */
            Vertex linked = new GenericVertex("link for " + v.getStopId(), coordinate.x,
                    coordinate.y);
            graph.addVertex(linked);
            for (Vertex i : atIntersection) {
                graph.addEdge(new FreeEdge(linked, i));
            }
            return linked;
        }

        /* split an edge bundle? */
        Collection<Edge> edges = index.getClosestEdges(coordinate, null);
        if (edges == null || edges.size() < 2) {
            return null;
        }
        return createVertex("link for " + v.getStopId(), edges, coordinate);
    }

    /** Create a vertex splitting the set of edges. If necessary, create new edges. */

    private Vertex createVertex(String label, Collection<Edge> edges, Coordinate coordinate) {

        if (edges.size() < 2) {
            // can't replace too few edges
            return null;
        }
        // Is this set of edges already replaced?
        HashSet<Edge> edgeSet = new HashSet<Edge>(edges);
        LinkedList<P2<PlainStreetEdge>> replacement = replacements.get(edgeSet);
        if (replacement == null) {
            // create replacement
            replacement = new LinkedList<P2<PlainStreetEdge>>();
            P2<PlainStreetEdge> newEdges = replace(edges);
            if (newEdges == null) {
                return null;
            }
            replacement.add(newEdges);

            replacements.put(edgeSet, replacement);
        }

        // Figure out which replacement edge pair to split
        double bestDist = Double.MAX_VALUE;
        P2<PlainStreetEdge> bestPair = null;

        Point p = geometryFactory.createPoint(coordinate);
        for (P2<PlainStreetEdge> pair : replacement) {
            Edge e1 = pair.getFirst();
            double dist = e1.getGeometry().distance(p);
            if (dist < bestDist) {
                bestDist = dist;
                bestPair = pair;
            }
        }

        return split(replacement, label, bestPair, coordinate);
    }

    /**
     * Split a matched pair of edges at the given coordinate
     */
    private Vertex split(LinkedList<P2<PlainStreetEdge>> replacement, String label,
            P2<PlainStreetEdge> bestPair, Coordinate coordinate) {

        PlainStreetEdge e1 = bestPair.getFirst();
        PlainStreetEdge e2 = bestPair.getSecond();
        String name = e1.getName();
        Vertex v1 = e1.getFromVertex();
        Vertex v2 = e1.getToVertex();

        LineString forwardGeometry = e1.getGeometry();
        LineString backGeometry = e2.getGeometry();

        P2<LineString> forwardGeometryPair = StreetLocation.splitGeometryAtPoint(forwardGeometry,
                coordinate);
        P2<LineString> backGeometryPair = StreetLocation.splitGeometryAtPoint(backGeometry,
                coordinate);

        LineString toMidpoint = forwardGeometryPair.getFirst();
        Coordinate midCoord = toMidpoint.getEndPoint().getCoordinate();

        double totalGeomLength = forwardGeometry.getLength();
        double lengthRatioIn = toMidpoint.getLength() / totalGeomLength;
        if (lengthRatioIn < 0.00001) {
            return v1;
        } else if (lengthRatioIn > 0.99999) {
            return v2;
        }

        double lengthIn = e1.getLength() * lengthRatioIn;
        double lengthOut = e1.getLength() * (1 - lengthRatioIn);

        GenericVertex midpoint = new GenericVertex("split at " + label, midCoord, name);

        // We are replacing two edges with four edges
        PlainStreetEdge forward1 = new PlainStreetEdge(v1, midpoint, toMidpoint, name, lengthIn, 
                e1.getPermission(), false);
        PlainStreetEdge backward1 = new PlainStreetEdge(midpoint, v2, 
                forwardGeometryPair.getSecond(), name, lengthOut, e1.getPermission(), true);

        PlainStreetEdge forward2 = new PlainStreetEdge(v2, midpoint, backGeometryPair.getFirst(),
                name, lengthOut, e2.getPermission(), false);
        PlainStreetEdge backward2 = new PlainStreetEdge(midpoint, v1, backGeometryPair.getSecond(),
                name, lengthIn, e2.getPermission(), true);

        ListIterator<P2<PlainStreetEdge>> it = replacement.listIterator();
        while (it.hasNext()) {
            P2<PlainStreetEdge> pair = it.next();
            if (pair == bestPair) {
                it.set(new P2<PlainStreetEdge>(forward1, backward2));
                it.add(new P2<PlainStreetEdge>(backward1, forward2));
                break;
            }
        }
        return midpoint;
    }

    /**
     * Create a pair of replacement edges (and the necessary linking vertices)
     * @param edges the set of turns (mostly) to replace
     * @return
     */
    private P2<PlainStreetEdge> replace(Collection<Edge> edges) {

        P2<Entry<StreetVertex, Set<Edge>>> ends = findEndVertices(edges);

        Entry<StreetVertex, Set<Edge>> start = ends.getFirst();
        Entry<StreetVertex, Set<Edge>> end = ends.getSecond();
        StreetVertex startVertex = start.getKey();

        StreetVertex endVertex = null;
        if (end != null) {
            endVertex = end.getKey();
        } else {
            /* it is assumed that we are splitting two edges, since the only
             * way we get one edge is where there's a one-way, car-only 
             * street, where a bus would never let someone out.
             */
            return null;
        }

        /* presently, start has a bunch of edges going the wrong way.  We want to actually find
         * a set of vertices in the same spot...
         */
        
        Vertex newStart = new GenericVertex("replace " + startVertex.getLabel(), startVertex.getX(), startVertex.getY());
        newStart = graph.addVertex(newStart);
        graph.addEdge(new FreeEdge(startVertex, newStart));
        
        List<Vertex> startVertices = index.getIntersectionAt(startVertex.getCoordinate());
        for (Vertex v : startVertices) {
            if (v != startVertex) {
                graph.addEdge(new FreeEdge(newStart, v));
            }
        }
        
        /* and likewise end */
        Vertex newEnd = new GenericVertex("replace " + endVertex.getLabel(), endVertex.getX(), endVertex.getY());
        newEnd = graph.addVertex(newEnd);
        graph.addEdge(new FreeEdge(endVertex, newEnd));
        
        List<Vertex> endVertices = index.getIntersectionAt(endVertex.getCoordinate());
        for (Vertex v : endVertices) {
            if (v != endVertex) {
                graph.addEdge(new FreeEdge(newEnd, v));
            }
        }
        
        /* create the replacement edges */
        PlainStreetEdge forward = new PlainStreetEdge(newStart, newEnd, startVertex
                .getGeometry(), startVertex.getName(), startVertex.getLength(), startVertex
                .getPermission(), false);

        PlainStreetEdge backward = new PlainStreetEdge(newEnd, newStart, endVertex
                .getGeometry(), endVertex.getName(), endVertex.getLength(), endVertex
                .getPermission(), true);

        forward.setWheelchairAccessible(startVertex.isWheelchairAccessible());
        backward.setWheelchairAccessible(startVertex.isWheelchairAccessible());

        P2<PlainStreetEdge> replacement = new P2<PlainStreetEdge>(forward, backward);
        return replacement;
    }

    private P2<Entry<StreetVertex, Set<Edge>>> findEndVertices(Collection<Edge> edges) {
        // find most common start and end points, which will be ends of this street
        HashMap<StreetVertex, Set<Edge>> numEdgesStartingAt = new HashMap<StreetVertex, Set<Edge>>();
        for (Edge edge : edges) {
            Set<Edge> starting = numEdgesStartingAt.get(edge.getFromVertex());
            if (starting == null) {
                starting = new HashSet<Edge>();
                numEdgesStartingAt.put((StreetVertex) edge.getFromVertex(), starting);
            }
            starting.add(edge);
        }

        int maxStarting = 0;
        int maxEnding = 0;
        Entry<StreetVertex, Set<Edge>> startingVertex = null;
        Entry<StreetVertex, Set<Edge>> endingVertex = null;
        for (Entry<StreetVertex, Set<Edge>> entry : numEdgesStartingAt.entrySet()) {
            int numEdges = entry.getValue().size();
            if (numEdges >= maxStarting) {
                endingVertex = startingVertex;
                maxEnding = maxStarting;
                maxStarting = numEdges;
                startingVertex = entry;
            } else if (numEdges > maxEnding) {
                endingVertex = entry;
                maxEnding = numEdges;
            }
        }

        return new P2<Entry<StreetVertex, Set<Edge>>>(startingVertex, endingVertex);
    }

}
