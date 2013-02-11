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

package org.opentripplanner.routing.location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.AreaEdge;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.CandidateEdge;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * Represents a location on a street, somewhere between the two corners. This is used when computing the first and last segments of a trip, for trips
 * that start or end between two intersections. Also for situating bus stops in the middle of street segments.
 */
public class StreetLocation extends StreetVertex {

    private static DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private ArrayList<Edge> extra = new ArrayList<Edge>();

    private boolean wheelchairAccessible;

    private ArrayList<StreetEdge> edges;

    private Graph graph;

    // maybe name should just be pulled from street being split
    public StreetLocation(Graph graph, String id, Coordinate nearestPoint, String name) {
        // calling constructor with null graph means this vertex is temporary
        super(null, id, nearestPoint.x, nearestPoint.y, name);
        this.graph = graph;
    }

    private static final long serialVersionUID = 1L;

    /**
     * Creates a StreetLocation on the given street (set of PlainStreetEdges). How far along is controlled by the location parameter, which represents
     * a distance along the edge between 0 (the from vertex) and 1 (the to vertex).
     * 
     * @param graph
     * 
     * @param label
     * @param name
     * @param edges A collection of nearby edges, which represent one street.
     * @param nearestPoint
     * 
     * @return the new StreetLocation
     */
    public static StreetLocation createStreetLocation(Graph graph, String label, String name,
            Iterable<StreetEdge> edges, Coordinate nearestPoint, Coordinate originalCoordinate) {

        /* linking vertex with epsilon transitions */
        StreetLocation location = createStreetLocation(graph, label, name, edges, nearestPoint);

        /* Extra edges for this area */
        Set<StreetEdge> allEdges = new HashSet<StreetEdge>();
        TraverseModeSet modes = new TraverseModeSet();
        for (StreetEdge street : edges) {
            allEdges.add(street);
            if (street instanceof AreaEdge) {
                for (StreetEdge e : ((AreaEdge) street).getArea().getEdges()) {
                    if (!allEdges.contains(e)) {
                        CandidateEdge ce = new CandidateEdge(e, originalCoordinate, 0, modes);
                        if (ce.endwise() || ce.getDistance() > .0005) {
                            // skip inappropriate area edges
                            continue;
                        }
                        StreetLocation areaSplitter = createStreetLocation(graph, label, name,
                                Arrays.asList(e), ce.getNearestPointOnEdge());
                        location.extra.addAll(areaSplitter.getExtra());
                        location.extra.add(new FreeEdge(location, areaSplitter));
                        location.extra.add(new FreeEdge(areaSplitter, location));
                        allEdges.add(e);
                    }
                }
            }
        }
        location.setSourceEdges(allEdges);

        return location;
    }

    public static StreetLocation createStreetLocation(Graph graph, String label, String name,
            Iterable<StreetEdge> edges, Coordinate nearestPoint) {

        boolean wheelchairAccessible = false;

        StreetLocation location = new StreetLocation(graph, label, nearestPoint, name);
        for (StreetEdge street : edges) {
            Vertex fromv = street.getFromVertex();
            Vertex tov = street.getToVertex();
            wheelchairAccessible |= ((StreetEdge) street).isWheelchairAccessible();
            /* forward edges and vertices */
            Vertex edgeLocation;
            if (distanceLibrary.distance(nearestPoint, fromv.getCoordinate()) < 1) {
                // no need to link to area edges caught on-end
                edgeLocation = fromv;
                new FreeEdge(location, edgeLocation);
                new FreeEdge(edgeLocation, location);
            } else if (distanceLibrary.distance(nearestPoint, tov.getCoordinate()) < 1) {
                // no need to link to area edges caught on-end
                edgeLocation = tov;
                new FreeEdge(location, edgeLocation);
                new FreeEdge(edgeLocation, location);
            } else {
                edgeLocation = location;
                createHalfLocation(graph, location, label + " to " + tov.getLabel(), name,
                        nearestPoint, street);
                double distanceToNearestTransitStop = Math.min(
                        tov.getDistanceToNearestTransitStop(),
                        fromv.getDistanceToNearestTransitStop());
                edgeLocation.setDistanceToNearestTransitStop(distanceToNearestTransitStop);
            }
        }
        location.setWheelchairAccessible(wheelchairAccessible);
        return location;

    }

    private void setSourceEdges(Iterable<StreetEdge> edges) {
        this.edges = new ArrayList<StreetEdge>();
        for (StreetEdge edge : edges) {
            this.edges.add(edge);
        }
    }

    public List<StreetEdge> getSourceEdges() {
        return edges;
    }

    private static void createHalfLocation(Graph graph, StreetLocation base, String label,
            String name, Coordinate nearestPoint, StreetEdge street) {

        StreetVertex tov = (StreetVertex) street.getToVertex();
        StreetVertex fromv = (StreetVertex) street.getFromVertex();
        Geometry geometry = street.getGeometry();

        P2<LineString> geometries = getGeometry(street, nearestPoint);

        double totalGeomLength = geometry.getLength();
        double lengthRatioIn = geometries.getFirst().getLength() / totalGeomLength;

        double lengthIn = street.getLength() * lengthRatioIn;
        double lengthOut = street.getLength() * (1 - lengthRatioIn);

        PlainStreetEdge newLeft = new PlainStreetEdge(fromv, base, geometries.getFirst(), name,
                lengthIn, street.getPermission(), false);
        PlainStreetEdge newRight = new PlainStreetEdge(base, tov, geometries.getSecond(), name,
                lengthOut, street.getPermission(), false);

        newLeft.setElevationProfile(street.getElevationProfile(0, lengthIn), false);
        newLeft.setNoThruTraffic(street.isNoThruTraffic());
        newLeft.setStreetClass(street.getStreetClass());
        newLeft.setWheelchairNote(street.getWheelchairNotes());
        newLeft.setNote(street.getNotes());

        newRight.setElevationProfile(street.getElevationProfile(lengthIn, lengthIn + lengthOut),
                false);
        newRight.setStreetClass(street.getStreetClass());
        newRight.setNoThruTraffic(street.isNoThruTraffic());
        newRight.setWheelchairNote(street.getWheelchairNotes());
        newRight.setNote(street.getNotes());
        for (TurnRestriction turnRestriction : street.getTurnRestrictions()) {
            newRight.addTurnRestriction(turnRestriction);
        }
        base.extra.add(newLeft);
        base.extra.add(newRight);
    }

    private static P2<LineString> getGeometry(StreetEdge e, Coordinate nearestPoint) {
        Geometry geometry = e.getGeometry();
        return splitGeometryAtPoint(geometry, nearestPoint);
    }

    public static P2<LineString> splitGeometryAtPoint(Geometry geometry, Coordinate nearestPoint) {
        LocationIndexedLine line = new LocationIndexedLine(geometry);
        LinearLocation l = line.indexOf(nearestPoint);

        LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
        LineString ending = (LineString) line.extractLine(l, line.getEndIndex());

        return new P2<LineString>(beginning, ending);
    }

    // public void reify(Graph graph) {
    // if (graph.getVertex(label) != null) {
    // // already reified
    // return;
    // }
    //
    // for (Edge e : extra) {
    // graph.addVerticesFromEdge(e);
    // }
    // }

    public List<Edge> getExtra() {
        return extra;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public boolean equals(Object o) {
        if (o instanceof StreetLocation) {
            StreetLocation other = (StreetLocation) o;
            return other.getCoordinate().equals(getCoordinate());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getCoordinate().hashCode();
    }

    public void addExtraEdgeTo(Vertex target) {
        extra.add(new FreeEdge(this, target));
        extra.add(new FreeEdge(target, this));
    }

    @Override
    public int removeTemporaryEdges() {
        int nRemoved = 0;
        for (Edge e : getExtra()) {
            graph.removeTemporaryEdge(e);
            // edges might already be detached
            if (e.detach() != 0)
                nRemoved += 1;
        }
        return nRemoved;
    }

    @Override
    public void finalize() {
        removeTemporaryEdges();
    }

}
