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
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TurnVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * Represents a location on a street, somewhere between the two corners. This is used when computing
 * the first and last segments of a trip, for trips that start or end between two intersections.
 * Also for situating bus stops in the middle of street segments.
 */
public class StreetLocation extends AbstractVertex {

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
     * Creates a StreetLocation on the given street (set of turns). How far along is controlled by
     * the location parameter, which represents a distance along the edge between 0 (the from
     * vertex) and 1 (the to vertex).
     * 
     * This is a little bit complicated, because TurnEdges get most of their data from
     * their fromVertex. So we have to create a separate fromVertex located at the start of the set
     * of turns, with a free edge into it from the from vertex.
     * @param graph 
     * 
     * @param label
     * @param name
     * @param edges
     *            A collection of nearby edges, which probably represent one street but might represent a
     *            more complicated situation (imagine a bus stop at the asterisk: *_||). These are
     *            ordered by ascending distance from nearestPoint
     * @param nearestPoint
     * 
     * @return the new StreetLocation
     */
    public static StreetLocation createStreetLocation(Graph graph, String label, String name,
            Iterable<StreetEdge> edges, Coordinate nearestPoint) {

        boolean wheelchairAccessible = false;

        /* linking vertex with epsilon transitions */
        StreetLocation location = new StreetLocation(graph, label, nearestPoint, name);

        HashMap<Geometry, P2<TurnVertex>> cache = new HashMap<Geometry, P2<TurnVertex>>();
        for (StreetEdge street : edges) {
            /* TODO: need to check for crossing uncrossable streets (in 
             * previous elements of edges) */

            Vertex fromv = street.getFromVertex();
            Vertex tov = street.getToVertex();
            if (street instanceof PlainStreetEdge) {
                wheelchairAccessible |= ((PlainStreetEdge) street).isWheelchairAccessible();
            } else {
                wheelchairAccessible |= ((TurnVertex) fromv).isWheelchairAccessible();
            }
            boolean seen = cache.containsKey(street.getGeometry());
            /* forward edges and vertices */
            Vertex edgeLocation;
            if (DistanceLibrary.distance(nearestPoint, fromv.getCoordinate()) < 0.0001) {
                edgeLocation = fromv;
            } else if (DistanceLibrary.distance(nearestPoint, tov.getCoordinate()) < 0.0001) {
                edgeLocation = tov;
            } else {
                edgeLocation = createHalfLocation(graph, location, label + " to "
                        + tov.getLabel(), name, nearestPoint, street, cache);
            }

            if (!seen) {
                FreeEdge l1in = new FreeEdge(location, edgeLocation);
                FreeEdge l1out = new FreeEdge(edgeLocation, location);

                location.extra.add(l1in);
                location.extra.add(l1out);
            }
            
            double distance = fromv.getDistanceToNearestTransitStop();
            if (distance < location.getDistanceToNearestTransitStop()) {
                location.setDistanceToNearestTransitStop(distance);
            }
        }

        location.setWheelchairAccessible(wheelchairAccessible);
        location.setSourceEdges(edges);
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

    private static TurnVertex createHalfLocation(Graph graph, StreetLocation base, String label,
            String name, Coordinate nearestPoint, Edge edge, HashMap<Geometry, P2<TurnVertex>> cache) {

        StreetEdge street = (StreetEdge) edge;
        Vertex fromv = street.getFromVertex();
        TurnVertex newFrom, location;
        Geometry geometry = street.getGeometry();
        if (cache.containsKey (geometry)) {
            P2<TurnVertex> cached = cache.get(geometry);
            location = cached.getSecond();
        } else {
            P2<LineString> geometries = getGeometry(street, nearestPoint);

            double totalGeomLength = geometry.getLength();
            double lengthRatioIn = geometries.getFirst().getLength() / totalGeomLength;

            double lengthIn = street.getLength() * lengthRatioIn;
            double lengthOut = street.getLength() * (1 - lengthRatioIn);

            newFrom = new TurnVertex(null, label + " (vertex going in to splitter)", geometries.getFirst(), street.getName(),
                    lengthIn, false, street.getNotes());
            newFrom.setElevationProfile(street.getElevationProfile(0, lengthIn), false);
            newFrom.setPermission(street.getPermission());
            newFrom.setNoThruTraffic(street.isNoThruTraffic());
            newFrom.setStreetClass(street.getStreetClass());

            location = new TurnVertex(null, label + " (vertex at splitter)", geometries.getSecond(), street.getName(), lengthOut,
                    false, street.getNotes());
            location.setElevationProfile(street.getElevationProfile(lengthIn, lengthIn + lengthOut), false);
            location.setPermission(street.getPermission());
            location.setStreetClass(street.getStreetClass());
            location.setNoThruTraffic(street.isNoThruTraffic());
            
            cache.put(geometry, new P2<TurnVertex>(newFrom, location));

            FreeEdge free = new FreeEdge(fromv, newFrom);
            TurnEdge incoming = new TurnEdge(newFrom, location);
            base.extra.add(free);
            base.extra.add(incoming);

        }
        Vertex tov = street.getToVertex();
        StreetEdge e;
        if (tov instanceof TurnVertex) {
            e = new TurnEdge(location, (TurnVertex) tov);
            if (edge instanceof TurnEdge) {
                ((TurnEdge)e).setRestrictedModes(((TurnEdge) edge).getRestrictedModes());
            }
        } else {
            OutEdge outEdge = new OutEdge(location, (StreetVertex) tov);
            e = outEdge;
        }
        base.extra.add(e);
        
        return location;
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
    
//    public void reify(Graph graph) {
//        if (graph.getVertex(label) != null) {
//            // already reified
//            return;
//        }
//
//        for (Edge e : extra) {
//            graph.addVerticesFromEdge(e);
//        }
//    }

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

    @Override public int removeTemporaryEdges() {
        int nRemoved = 0;
        for (Edge e : getExtra()) {
            graph.removeTemporaryEdge(e);
            // edges might already be detached
            if (e.detach() != 0)
                nRemoved += 1;
        }
        return nRemoved;
    }
    
}
