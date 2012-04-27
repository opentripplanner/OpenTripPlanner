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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.extra_graph.EdgesForRoute;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.edgetype.TinyTurnEdge;
import org.opentripplanner.routing.edgetype.factory.LocalStopFinder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl;
import org.opentripplanner.routing.impl.StreetVertexIndexServiceImpl.CandidateEdgeBundle;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

public class NetworkLinkerLibrary {

    private static Logger _log = LoggerFactory.getLogger(NetworkLinkerLibrary.class);

    /* for each original bundle of (turn)edges making up a street, a list of 
       edge pairs that will replace it */
    HashMap<HashSet<StreetEdge>, LinkedList<P2<PlainStreetEdge>>> replacements = 
        new HashMap<HashSet<StreetEdge>, LinkedList<P2<PlainStreetEdge>>>();
    
    /* a map to track which vertices were associated with each transit stop, to avoid repeat splitting */
    private HashMap<Vertex, Collection<StreetVertex>> splitVertices = 
            new HashMap<Vertex, Collection<StreetVertex>> (); 

    private GeometryFactory geometryFactory = new GeometryFactory();

    /* by default traverse options allow walking only, which is what we want */
    private TraverseOptions options = new TraverseOptions();

    private Graph graph;

    private StreetVertexIndexServiceImpl index;

    private EdgesForRoute edgesForRoute;

    private TransitIndexService transitIndex;

    public NetworkLinkerLibrary(Graph graph, HashMap<Class<?>, Object> extra) {
        this.graph = graph;
        this.transitIndex = graph.getService(TransitIndexService.class);
        EdgesForRoute edgesForRoute = (EdgesForRoute) extra.get(EdgesForRoute.class);
        this.edgesForRoute = edgesForRoute;
        _log.debug("constructing index...");
        this.index = new StreetVertexIndexServiceImpl(graph);
        this.index.setup();
    }

    /**
     * The entry point for networklinker to link each transit stop.
     * 
     * @param v
     * @param wheelchairAccessible
     * @return true if the links were successfully added, otherwise false
     */
    public boolean connectVertexToStreets(TransitStop v, boolean wheelchairAccessible) {
        List<Edge> nearbyEdges = null;
        if (edgesForRoute != null && transitIndex != null) {
            nearbyEdges = new ArrayList<Edge>();
            for (AgencyAndId route : transitIndex.getRoutesForStop(v.getStopId())) {
                List<Edge> edges = edgesForRoute.get(route);
                if (edges != null) {
                    nearbyEdges.addAll(edges);
                }
            }
        }
        Collection<StreetVertex> nearbyStreetVertices = getNearbyStreetVertices(v, nearbyEdges);
        if (nearbyStreetVertices == null) {
            return false;
        } else {
            for (StreetVertex sv : nearbyStreetVertices) {
                new StreetTransitLink(sv, v, wheelchairAccessible);
                new StreetTransitLink(v, sv, wheelchairAccessible);
            }
            return true;
        }
    }

    /**
     * The entry point for networklinker to link each bike rental station.
     * 
     * @param v
     * @return true if the links were successfully added, otherwise false
     */
    public boolean connectVertexToStreets(BikeRentalStationVertex v) {
        Collection<StreetVertex> nearbyStreetVertices = getNearbyStreetVertices(v, null);
        if (nearbyStreetVertices == null) {
            return false;
        } else {
            for (StreetVertex sv : nearbyStreetVertices) {
                new StreetBikeRentalLink(sv, v);
                new StreetBikeRentalLink(v, sv);
            }
            return true;
        }
    }

//    /**
//     * Add edges from street locations to the target vertex.
//     * 
//     * @param v
//     * @param wheelchairAccessible
//     * @return true if the links were successfully added, otherwise false
//     */
//    public boolean determineIncomingEdgesForVertex(Vertex v, boolean wheelchairAccessible) {
//        Vertex location = getLocation(v);
//        if (location == null) {
//            getLocation(v);
//            return false;
//        } else {
//            graph.addEdge(new StreetTransitLink(location, v, wheelchairAccessible));
//            return true;
//        }
//    }
//
//    /**
//     * Add edges from the target vertex to street locations.
//     * 
//     * @param v
//     * @param wheelchairAccessible
//     * @return true if the links were successfully added, otherwise false
//     */
//    public boolean determineOutgoingEdgesForVertex(Vertex v, boolean wheelchairAccessible) {
//        Vertex location = getLocation(v);
//        if (location == null) {
//            getLocation(v);
//            return false;
//        } else {
//            graph.addEdge(new StreetTransitLink(v, location, wheelchairAccessible));
//            return true;
//        }
//    }

//    public void addAllReplacementEdgesToGraph() {
//
//        for (Entry<HashSet<StreetEdge>, LinkedList<P2<PlainStreetEdge>>> 
//             entry : replacements.entrySet()) {
//            /* insert newly created edges into the graph */
//            for (P2<PlainStreetEdge> edge : entry.getValue()) {
//                graph.addVerticesFromEdge(edge.getFirst() );
//                graph.addVerticesFromEdge(edge.getSecond());
//            }
//            /* remove original (replaced) edges from the graph */
//            for (Edge edge : entry.getKey()) {
//                // uncomment to remove replaced edges
//                // graph.removeEdge((Edge)edge);
//            }
//        }
//    }

    /****
     * Private Methods
     ****/

    /**
     * For the given vertex, find or create some vertices nearby in the street network.
     * Once the vertices are found they are remembered, and subsequent calls to this 
     * method with the same Vertex argument will return the same collection of vertices. 
     * This method is potentially called multiple times with the same Vertex as an argument, 
     * via the "determineIncomingEdgesForVertex" and "determineOutgoingEdgesForVertex" methods.
     * 
     * Used by both the network linker and for adding temporary "extra" edges at the origin 
     * and destination of a search.
     */
    private Collection<StreetVertex> getNearbyStreetVertices(Vertex v, Collection<Edge> nearbyRouteEdges) {
        Collection<StreetVertex> existing = splitVertices.get(v);
        if (existing != null)
            return existing;

        String vertexLabel;
        if (v instanceof TransitVertex)
            vertexLabel = "link for " + ((TransitVertex)v).getStopId();
        else
            vertexLabel = "link for " + v;
        Coordinate coordinate = v.getCoordinate();

        /* is there a bundle of edges nearby to use or split? */
        CandidateEdgeBundle edges = index.getClosestEdges(coordinate, options, null, nearbyRouteEdges);
        if (edges == null || edges.size() < 2) {
            // no edges were found nearby, or a bidirectional/loop bundle of edges was not identified
            _log.debug("found too few edges: {} {}", v.getName(), v.getCoordinate());
            return null;
        }
        // if the bundle was caught endwise (T intersections and dead ends), 
        // get the intersection instead.
        if (edges.endwise()) {
            return index.getIntersectionAt(edges.endwiseVertex.getCoordinate());
        } else {
            /* is the stop right at an intersection? */
            List<StreetVertex> atIntersection = index.getIntersectionAt(coordinate);
            if (atIntersection != null) {
                // if so, the stop can be linked directly to all vertices at the intersection
                if (edges.getScore() > atIntersection.get(0).distance(coordinate))
                    return atIntersection;
            }
            return getSplitterVertices(vertexLabel, edges.toEdgeList(), coordinate);
        }
    }

    /** 
     * Given a bundle of parallel, coincident (turn)edges, find a vertex splitting the set of edges as close as
     * possible to the given coordinate. If necessary, create new edges reflecting the split and update the 
     * replacement edge lists accordingly. 
     * 
     * Split edges are not added to the graph immediately, so that they can be re-split later if another stop
     * is located near the same bundle of original edges.
     */
    private Collection<StreetVertex> getSplitterVertices(String label, Collection<StreetEdge> edges, Coordinate coordinate) {

        // It is assumed that we are splitting at least two edges.
        if (edges.size() < 2) {
            return null;
        }

        // Has this set of original edges already been replaced by split edges?
        HashSet<StreetEdge> edgeSet = new HashSet<StreetEdge>(edges);
        LinkedList<P2<PlainStreetEdge>> replacement = replacements.get(edgeSet);
        if (replacement == null) {
            // first time this edge bundle is being split
            replacement = new LinkedList<P2<PlainStreetEdge>>();
            // make a single pair of PlainStreetEdges equivalent to this bundle
            P2<PlainStreetEdge> newEdges = replace(edges);
            if (newEdges == null) {
                return null;
            }
            replacement.add(newEdges);
            replacements.put(edgeSet, replacement);
        }

        // If the original replacement edge pair has already been split,
        // decide out which sub-segment the current coordinate lies on.
        double bestDist = Double.MAX_VALUE;
        P2<PlainStreetEdge> bestPair = null;
        Point p = geometryFactory.createPoint(coordinate);
        for (P2<PlainStreetEdge> pair : replacement) {
            PlainStreetEdge e1 = pair.getFirst();
            double dist = e1.getGeometry().distance(p);
            if (dist < bestDist) {
                bestDist = dist;
                bestPair = pair;
            }
        }
        
        // split the (sub)segment edge pair as needed, returning vertices at the split point
        return split(replacement, label, bestPair, coordinate);
    }

    /**
     * Split a matched (bidirectional) pair of edges at the given coordinate, unless the coordinate is
     * very close to one of the existing endpoints. Returns the vertices located at the split point.
     */
    private Collection<StreetVertex> split(LinkedList<P2<PlainStreetEdge>> replacement, String label,
            P2<PlainStreetEdge> bestPair, Coordinate coordinate) {

        PlainStreetEdge e1 = bestPair.getFirst();
        PlainStreetEdge e2 = bestPair.getSecond();

        String name = e1.getName();
        StreetVertex e1v1 = (StreetVertex) e1.getFromVertex();
        StreetVertex e1v2 = (StreetVertex) e1.getToVertex();

        StreetVertex e2v1 = (StreetVertex) e2.getFromVertex();
        StreetVertex e2v2 = (StreetVertex) e2.getToVertex();

        LineString forwardGeometry = e1.getGeometry();
        LineString backGeometry = e2.getGeometry();

        P2<LineString> forwardGeometryPair = StreetLocation.splitGeometryAtPoint(forwardGeometry,
                coordinate);
        P2<LineString> backGeometryPair = StreetLocation.splitGeometryAtPoint(backGeometry,
                coordinate);

        LineString toMidpoint = forwardGeometryPair.getFirst();
        Coordinate midCoord = toMidpoint.getEndPoint().getCoordinate();

        // determine how far along the original pair the split would occur
        double totalGeomLength = forwardGeometry.getLength();
        double lengthRatioIn = toMidpoint.getLength() / totalGeomLength;

        // If coordinate is coincident with an endpoint of the edge pair, splitting is unnecessary. 
        // note: the pair potentially being split was generated by the 'replace' method,
        // so the two PlainStreetEdges are known to be pointing in opposite directions.
        if (lengthRatioIn < 0.00001) {
            return Arrays.asList(e1v1, e2v2);
        } else if (lengthRatioIn > 0.99999) {
            return Arrays.asList(e1v2, e2v1);
        }

        double lengthIn  = e1.getLength() * lengthRatioIn;
        double lengthOut = e1.getLength() * (1 - lengthRatioIn);

        // Split each edge independently. If a only one splitter vertex is used, routing may take 
        // shortcuts thought the splitter vertex to avoid turn penalties.
        StreetVertex e1midpoint = new IntersectionVertex(graph, "split 1 at " + label, midCoord, name);
        StreetVertex e2midpoint = new IntersectionVertex(graph, "split 2 at " + label, midCoord, name);
        
        // We are replacing two edges with four edges
        PlainStreetEdge forward1 = new PlainStreetEdge(e1v1, e1midpoint, toMidpoint, name, lengthIn,
                e1.getPermission(), false);
        PlainStreetEdge forward2 = new PlainStreetEdge(e1midpoint, e1v2,
                forwardGeometryPair.getSecond(), name, lengthOut, e1.getPermission(), true);

        PlainStreetEdge backward1 = new PlainStreetEdge(e2v1, e2midpoint, backGeometryPair.getFirst(),
                name, lengthOut, e2.getPermission(), false);
        PlainStreetEdge backward2 = new PlainStreetEdge(e2midpoint, e2v2, backGeometryPair.getSecond(),
                name, lengthIn, e2.getPermission(), true);

        double forwardBseLengthIn = e1.getBicycleSafetyEffectiveLength() * lengthRatioIn;
        double forwardBseLengthOut = e1.getBicycleSafetyEffectiveLength() * (1 - lengthRatioIn);
        forward1.setBicycleSafetyEffectiveLength(forwardBseLengthIn);
        forward2.setBicycleSafetyEffectiveLength(forwardBseLengthOut);

        double backwardBseLengthIn = e2.getBicycleSafetyEffectiveLength() * lengthRatioIn;
        double backwardBseLengthOut = e2.getBicycleSafetyEffectiveLength() * (1 - lengthRatioIn);
        backward1.setBicycleSafetyEffectiveLength(backwardBseLengthIn);
        backward2.setBicycleSafetyEffectiveLength(backwardBseLengthOut);

        forward1.setElevationProfile(e1.getElevationProfile(0, lengthIn), false);
        backward1.setElevationProfile(e2.getElevationProfile(0, lengthOut), false);
        forward2.setElevationProfile(e1.getElevationProfile(lengthOut, totalGeomLength), false);
        backward2.setElevationProfile(e2.getElevationProfile(lengthIn, totalGeomLength), false);

        // swap the new split edge into the replacements list, and remove the old ones
        ListIterator<P2<PlainStreetEdge>> it = replacement.listIterator();
        while (it.hasNext()) {
            P2<PlainStreetEdge> pair = it.next();
            if (pair == bestPair) {
                it.set(new P2<PlainStreetEdge>(forward1, backward2));
                it.add(new P2<PlainStreetEdge>(forward2, backward1));
                break;
            }
        }
        
        // disconnect the two old edges from the graph
        e1.detach();
        e2.detach();
        
        // return the two new splitter vertices
        return Arrays.asList(e1midpoint, e2midpoint);
    }

    /**
     * Create a pair of PlainStreetEdges pointing in opposite directions, equivalent to the given
     * bundle of (turn)edges. Create linking vertices as necessary.
     * 
     * @param  edges
     *         the set of turns (mostly) to replace
     * @return the new replacement edge pair
     */
    private P2<PlainStreetEdge> replace(Collection<StreetEdge> edges) {
        /* find the two most common starting points in this edge bundle */ 
        P2<Entry<TurnVertex, Set<Edge>>> ends = findEndVertices(edges);

        Entry<TurnVertex, Set<Edge>> start = ends.getFirst();
        Entry<TurnVertex, Set<Edge>> end = ends.getSecond();
        TurnVertex startVertex = start.getKey();

        TurnVertex endVertex = null;
        if (end != null) {
            endVertex = end.getKey();
        } else {
            // It is assumed that we are splitting at least two edges, since the only way we would get 
            // one edge is on a one-way, car-only street, where a bus would never let someone out.
            return null;
        }

        /*
         * Presently, end contains a set of (turn)edges running back toward start. 
         * We also need an intersection vertex at end to serve as a PlainStreetEdge endpoint.
         * newEnd will be at the same location as origin of endVertex's outgoing edges,
         * and thus the same location as the destination of startVertex's outgoing edges.
         */
        StreetVertex newEnd = new IntersectionVertex(graph, "replace " + endVertex.getLabel(), endVertex.getX(),
                endVertex.getY(), endVertex.getName());

        for (Edge e: startVertex.getOutgoing()) {
            final Vertex toVertex = e.getToVertex();
            if (!toVertex.getCoordinate().equals(endVertex.getCoordinate())) {
                continue;
            }
            if (e instanceof TurnEdge) {
                final TurnEdge turnEdge = (TurnEdge) e;
                TinyTurnEdge newTurn = new TinyTurnEdge(newEnd, toVertex, turnEdge.getPermission());
                newTurn.setRestrictedModes(turnEdge.getRestrictedModes());
                newTurn.setTurnCost(turnEdge.turnCost);
            } else {
                new FreeEdge(newEnd, toVertex);
            }
        }

        /* and likewise for start */
        StreetVertex newStart = new IntersectionVertex(graph, "replace " + startVertex.getLabel(),
                startVertex.getX(), startVertex.getY(), startVertex.getName());

        for (Edge e: endVertex.getOutgoing()) {
            final Vertex toVertex = e.getToVertex();
            if (!toVertex.getCoordinate().equals(startVertex.getCoordinate())) {
                continue;
            }
            if (e instanceof TurnEdge) {
                final TurnEdge turnEdge = (TurnEdge) e;
                TinyTurnEdge newTurn = new TinyTurnEdge(newStart, toVertex, turnEdge.getPermission());
                newTurn.setRestrictedModes(turnEdge.getRestrictedModes());
                newTurn.setTurnCost(turnEdge.turnCost);
            } else {
                new FreeEdge(newStart, toVertex);
            }
        }

        /* create a pair of PlainStreetEdges equivalent to the bundle of original (turn)edges */
        PlainStreetEdge forward = new PlainStreetEdge(startVertex, newEnd, startVertex.getGeometry(),
                startVertex.getName(), startVertex.getLength(), startVertex.getPermission(), false);

        PlainStreetEdge backward = new PlainStreetEdge(endVertex, newStart, endVertex.getGeometry(),
                endVertex.getName(), endVertex.getLength(), endVertex.getPermission(), true);

        forward.setWheelchairAccessible(startVertex.isWheelchairAccessible());
        backward.setWheelchairAccessible(endVertex.isWheelchairAccessible());

        forward.setElevationProfile(startVertex.getElevationProfile(), false);
        backward.setElevationProfile(endVertex.getElevationProfile(), false);

        forward.setBicycleSafetyEffectiveLength(startVertex.getBicycleSafetyEffectiveLength());
        backward.setBicycleSafetyEffectiveLength(endVertex.getBicycleSafetyEffectiveLength());

        P2<PlainStreetEdge> replacement = new P2<PlainStreetEdge>(forward, backward);
        
        /* return the replacements. note that they have not yet been added to the graph. */
        return replacement;
    }

    private P2<Entry<TurnVertex, Set<Edge>>> findEndVertices(Collection<StreetEdge> edges) {
        // find the two most common edge start points, which will be the endpoints of this street
        HashMap<TurnVertex, Set<Edge>> edgesStartingAt = new HashMap<TurnVertex, Set<Edge>>();
        for (Edge edge : edges) {
            Set<Edge> starting = edgesStartingAt.get(edge.getFromVertex());
            if (starting == null) {
                starting = new HashSet<Edge>();
                edgesStartingAt.put((TurnVertex) edge.getFromVertex(), starting);
            }
            starting.add(edge);
        }
        
        int maxStarting = 0;
        int maxEnding = 0;
        Entry<TurnVertex, Set<Edge>> startingVertex = null;
        Entry<TurnVertex, Set<Edge>> endingVertex = null;
        for (Entry<TurnVertex, Set<Edge>> entry : edgesStartingAt.entrySet()) {
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

        return new P2<Entry<TurnVertex, Set<Edge>>>(startingVertex, endingVertex);
    }

    public void markLocalStops() {
        LocalStopFinder localStopFinder = new LocalStopFinder(index, graph);
        localStopFinder.markLocalStops();
    }

}
