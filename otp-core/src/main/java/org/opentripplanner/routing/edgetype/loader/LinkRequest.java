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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraversalRequirements;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdgeBundle;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * This class keeps track of all of the edges created during a particular case of network linking
 */
public class LinkRequest {

    private static Logger LOG = LoggerFactory.getLogger(LinkRequest.class);

    NetworkLinkerLibrary linker;
    
    private Boolean result;

    private List<Edge> edgesAdded = new ArrayList<Edge>();

    private DistanceLibrary distanceLibrary;
    
    public LinkRequest(NetworkLinkerLibrary linker) {
        this.linker = linker;
        this.distanceLibrary = linker.getDistanceLibrary();
    }
    
    /**
     * The entry point for networklinker to link each bike rental station.
     * 
     * @param v
     * Sets result to true if the links were successfully added, otherwise false
     */
    public void connectVertexToStreets(BikeRentalStationVertex v) {
        Collection<StreetVertex> nearbyStreetVertices = getNearbyStreetVertices(v, null, null);
        if (nearbyStreetVertices == null) {
            result = false;
        } else {
            for (StreetVertex sv : nearbyStreetVertices) {
                addEdges(new StreetBikeRentalLink(sv, v), 
                         new StreetBikeRentalLink(v, sv));
            }
            result = true;
        }
    }

    public boolean getResult() {
        if (result == null) {
            throw new IllegalStateException("Can't get result of LinkRequest; no operation performed");
        }
        return result;
    }

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
    private Collection<StreetVertex> getNearbyStreetVertices(Vertex v, Collection<Edge> nearbyRouteEdges, RoutingRequest options) {
        Collection<StreetVertex> existing = linker.splitVertices.get(v);
        if (existing != null)
            return existing;

        String vertexLabel;
        if (v instanceof TransitVertex)
            vertexLabel = "link for " + ((TransitVertex)v).getStopId();
        else
            vertexLabel = "link for " + v;
        Coordinate coordinate = v.getCoordinate();

        /* is there a bundle of edges nearby to use or split? */
        GenericLocation location = new GenericLocation(coordinate);
        TraversalRequirements reqs = new TraversalRequirements(options);
        CandidateEdgeBundle edges = linker.index.getClosestEdges(location, reqs, null, nearbyRouteEdges, true);
        if (edges == null || edges.size() < 1) {
            // no edges were found nearby, or a bidirectional/loop bundle of edges was not identified
            LOG.debug("found too few edges: {} {}", v.getName(), v.getCoordinate());
            return null;
        }
        // if the bundle was caught endwise (T intersections and dead ends), 
        // get the intersection instead.
        if (edges.endwise()) {
            List<StreetVertex> list = Arrays.asList(edges.endwiseVertex);
            linker.splitVertices.put(v, list);
            return list;
        } else {
            /* is the stop right at an intersection? */
            StreetVertex atIntersection = linker.index.getIntersectionAt(coordinate);
            if (atIntersection != null) {
                // if so, the stop can be linked directly to all vertices at the intersection
                if (edges.getScore() > distanceLibrary.distance(atIntersection.getCoordinate(), coordinate))
                    return Arrays.asList(atIntersection);
            }
            return getSplitterVertices(vertexLabel, edges.toEdgeList(), coordinate);
        }
    }

    /** 
     * Given a bundle of parallel, coincident edges, find a vertex splitting the set of edges as close as
     * possible to the given coordinate. If necessary, create new edges reflecting the split and update the 
     * replacement edge lists accordingly. 
     * 
     * Split edges are not added to the graph immediately, so that they can be re-split later if another stop
     * is located near the same bundle of original edges.
     */
    private Collection<StreetVertex> getSplitterVertices(String label, Collection<StreetEdge> edges, Coordinate coordinate) {

        // It is assumed that we are splitting at least one edge.
        if (edges.size() < 1) {
            return null;
        }

        // Has this set of original edges already been replaced by split edges?
        HashSet<StreetEdge> edgeSet = new HashSet<StreetEdge>(edges);
        LinkedList<P2<PlainStreetEdge>> replacement = linker.replacements.get(edgeSet);
        if (replacement == null) {
            replacement = new LinkedList<P2<PlainStreetEdge>>();
            Iterator<StreetEdge> iter = edges.iterator();
            StreetEdge first = iter.next();
            StreetEdge second = null;
            while (iter.hasNext()) {
                StreetEdge edge = iter.next();
                if (edge.getFromVertex() == first.getToVertex() && edge.getToVertex() == first.getFromVertex()) {
                    second = edge;
                }
            }
            PlainStreetEdge secondClone;
            if (second == null) {
                secondClone = null;
            } else {
                secondClone = ((PlainStreetEdge) second).clone();
            }
            P2<PlainStreetEdge> newEdges = new P2<PlainStreetEdge>(((PlainStreetEdge) first).clone(), secondClone); 
            replacement.add(newEdges);
            linker.replacements.put(edgeSet, replacement);
        }

        // If the original replacement edge pair has already been split,
        // decide out which sub-segment the current coordinate lies on.
        double bestDist = Double.MAX_VALUE;
        P2<PlainStreetEdge> bestPair = null;
        Point p = GeometryUtils.getGeometryFactory().createPoint(coordinate);
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
        LineString forwardGeometry = e1.getGeometry();
        
        StreetVertex e2v1 = null;
        StreetVertex e2v2 = null;
        P2<LineString> backGeometryPair = null;
        if (e2 != null) {
            e2v1 = (StreetVertex) e2.getFromVertex();
            e2v2 = (StreetVertex) e2.getToVertex();
            LineString backGeometry = e2.getGeometry();
            backGeometryPair = GeometryUtils.splitGeometryAtPoint(backGeometry,
                    coordinate);
        }
        
        P2<LineString> forwardGeometryPair = GeometryUtils.splitGeometryAtPoint(forwardGeometry,
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
            ArrayList<StreetVertex> out = new ArrayList<StreetVertex>();
            out.add(e1v1);
            if (e2 != null) {
                out.add(e2v2);
            }
            return out;
        } else if (lengthRatioIn > 0.99999) {
            ArrayList<StreetVertex> out = new ArrayList<StreetVertex>();
            out.add(e1v2);
            if (e2 != null) {
                out.add(e1v2);
            }
            return out;
        }

        double lengthIn  = e1.getLength() * lengthRatioIn;
        double lengthOut = e1.getLength() * (1 - lengthRatioIn);

        // Split each edge independently. If a only one splitter vertex is used, routing may take 
        // shortcuts thought the splitter vertex to avoid turn penalties.
        IntersectionVertex e1midpoint = new IntersectionVertex(linker.graph, "split 1 at " + label, midCoord.x, midCoord.y, name);
        // We are replacing two edges with four edges
        PlainStreetEdge forward1 = new PlainStreetEdge(e1v1, e1midpoint, toMidpoint, name, lengthIn,
                e1.getPermission(), false);
        PlainStreetEdge forward2 = new PlainStreetEdge(e1midpoint, e1v2,
                forwardGeometryPair.getSecond(), name, lengthOut, e1.getPermission(), true);

        if (e1 instanceof AreaEdge) {
            ((AreaEdge) e1).getArea().addVertex(e1midpoint, linker.graph);
        }

        addEdges(forward1, forward2);

        PlainStreetEdge backward1 = null;
        PlainStreetEdge backward2 = null;
        IntersectionVertex e2midpoint = null;
        if (e2 != null) {
            e2midpoint  = new IntersectionVertex(linker.graph, "split 2 at " + label, midCoord.x, midCoord.y, name);
            backward1 = new PlainStreetEdge(e2v1, e2midpoint, backGeometryPair.getFirst(),
                    name, lengthOut, e2.getPermission(), false);
            backward2 = new PlainStreetEdge(e2midpoint, e2v2, backGeometryPair.getSecond(),
                    name, lengthIn, e2.getPermission(), true);
            if (e2 instanceof AreaEdge) {
                ((AreaEdge) e2).getArea().addVertex(e2midpoint, linker.graph);
            }
            double backwardBseLengthIn = e2.getBicycleSafetyEffectiveLength() * lengthRatioIn;
            double backwardBseLengthOut = e2.getBicycleSafetyEffectiveLength() * (1 - lengthRatioIn);
            backward1.setBicycleSafetyEffectiveLength(backwardBseLengthIn);
            backward2.setBicycleSafetyEffectiveLength(backwardBseLengthOut);
            backward1.setElevationProfile(e2.getElevationProfile(0, lengthOut), false);
            backward2.setElevationProfile(e2.getElevationProfile(lengthIn, totalGeomLength), false);
            addEdges(backward1, backward2);
        }

        double forwardBseLengthIn = e1.getBicycleSafetyEffectiveLength() * lengthRatioIn;
        double forwardBseLengthOut = e1.getBicycleSafetyEffectiveLength() * (1 - lengthRatioIn);
        forward1.setBicycleSafetyEffectiveLength(forwardBseLengthIn);
        forward2.setBicycleSafetyEffectiveLength(forwardBseLengthOut);

        forward1.setElevationProfile(e1.getElevationProfile(0, lengthIn), false);
        forward2.setElevationProfile(e1.getElevationProfile(lengthOut, totalGeomLength), false);

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
        linker.graph.removeTemporaryEdge(e1);
        edgesAdded.remove(e1);
        //e1.detach();
        
        if (e2 != null) {
            linker.graph.removeTemporaryEdge(e2);
            edgesAdded.remove(e2);
            //e2.detach();
            // return the two new splitter vertices
            return Arrays.asList((StreetVertex) e1midpoint, e2midpoint);
        } else {
            // return the one new splitter vertices
            return Arrays.asList((StreetVertex) e1midpoint);
        }

    }

    private void addEdges(Edge... newEdges) {
        edgesAdded.addAll(Arrays.asList(newEdges));
    }

    public List<Edge> getEdgesAdded() {
        return edgesAdded;
    }

    public void connectVertexToStreets(TransitStop v, boolean wheelchairAccessible) {
        List<Edge> nearbyEdges = null;
        if (linker.edgesForRoute != null && linker.transitIndex != null) {
            nearbyEdges = new ArrayList<Edge>();
            for (AgencyAndId route : linker.transitIndex.getRoutesForStop(v.getStopId())) {
                List<Edge> edges = linker.edgesForRoute.get(route);
                if (edges != null) {
                    nearbyEdges.addAll(edges);
                }
            }
        }
        TraverseModeSet modes = v.getModes().clone();
        modes.setMode(TraverseMode.WALK, true);
        RoutingRequest request = new RoutingRequest(modes);
        Collection<StreetVertex> nearbyStreetVertices = getNearbyStreetVertices(v, nearbyEdges, request);
        if (nearbyStreetVertices == null) {
            result = false;
        } else {
            for (StreetVertex sv : nearbyStreetVertices) {
                new StreetTransitLink(sv, v, wheelchairAccessible);
                new StreetTransitLink(v, sv, wheelchairAccessible);
            }
            result = true;
        }
    }

}
