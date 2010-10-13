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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;

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
public class StreetLocation extends GenericVertex {

    private ArrayList<Edge> extra = new ArrayList<Edge>();

    private boolean wheelchairAccessible;

    public StreetLocation(String id, Coordinate nearestPoint, String name) {
        super(id, nearestPoint, name);
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
     * 
     * @param label
     * @param name
     * @param edges
     *            A List of nearby edges, which probably represent one street but might represent a
     *            more complicated situation (imagine a bus stop at the asterisk: *_||). These are
     *            ordered by ascending distance from nearestPoint
     * @param nearestPoint
     * 
     * @return the new StreetLocation
     */
    public static StreetLocation createStreetLocation(Graph graph, String label, String name,
            Collection<Edge> edges, Coordinate nearestPoint) {

        boolean wheelchairAccessible = false;

        /* linking vertex with epsilon transitions */
        StreetLocation location = new StreetLocation(label, nearestPoint, name);

        HashMap<Geometry, P2<StreetVertex>> cache = new HashMap<Geometry, P2<StreetVertex>>();
        for (Edge street : edges) {
            /* TODO: need to check for crossing uncrossable streets (in 
             * previous elements of edges) */

            Vertex fromv = street.getFromVertex();
            if (street instanceof PlainStreetEdge) {
                wheelchairAccessible |= ((PlainStreetEdge) street).isWheelchairAccessible();
            } else {
                wheelchairAccessible |= ((StreetVertex) fromv).isWheelchairAccessible();
            }
            boolean seen = cache.containsKey(street.getGeometry());
            /* forward edges and vertices */
            StreetVertex edgeLocation = createHalfLocation(graph, location, label + " to "
                    + street.getToVertex().getLabel(), name, nearestPoint, street, cache);

            if (!seen) {
                Edge l1in = new FreeEdge(location, edgeLocation);
                Edge l1out = new FreeEdge(edgeLocation, location);

                location.extra.add(l1in);
                location.extra.add(l1out);
            }
        }

        location.setWheelchairAccessible(wheelchairAccessible);

        return location;

    }

    private void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    private static StreetVertex createHalfLocation(Graph graph, StreetLocation base, String label,
            String name, Coordinate nearestPoint, Edge edge, HashMap<Geometry, P2<StreetVertex>> cache) {

        StreetEdge street = (StreetEdge) edge;
        Vertex fromv = street.getFromVertex();
        StreetVertex newFrom, location;
        Geometry geometry = street.getGeometry();
        if (cache.containsKey (geometry)) {
            P2<StreetVertex> cached = cache.get(geometry);
            newFrom = cached.getFirst();
            location = cached.getSecond();
        } else {
            P2<LineString> geometries = getGeometry(street, nearestPoint);

            double totalGeomLength = geometry.getLength();
            double lengthRatioIn = geometries.getFirst().getLength() / totalGeomLength;

            double lengthIn = street.getLength() * lengthRatioIn;
            double lengthOut = street.getLength() * (1 - lengthRatioIn);

            newFrom = new StreetVertex(label + " (vertex going in to splitter)", geometries.getFirst(), name,
                    lengthIn, false);
            newFrom.setElevationProfile(street.getElevationProfile(0, lengthIn));
            newFrom.setPermission(street.getPermission());

            location = new StreetVertex(label + " (vertex at splitter)", geometries.getSecond(), name, lengthOut,
                    false);
            location.setElevationProfile(street.getElevationProfile(lengthIn, totalGeomLength));
            location.setPermission(street.getPermission());
            
            cache.put(geometry, new P2<StreetVertex>(newFrom, location));

            FreeEdge free = new FreeEdge(fromv, newFrom);
            TurnEdge incoming = new TurnEdge(newFrom, location);

            base.extra.add(free);
            base.extra.add(incoming);
        }
        Vertex tov = street.getToVertex();
        Edge e;
        if (tov instanceof StreetVertex) {
            e = new TurnEdge(location, (StreetVertex) tov);
        } else {
            e = new OutEdge(location, tov);
        }
        base.extra.add(e);
        
        return location;
    }

    private static P2<LineString> getGeometry(Edge e, Coordinate nearestPoint) {
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
    
    public void reify(Graph graph) {
        if (graph.getVertex(label) != null) {
            // already reified
            return;
        }

        for (Edge e : extra) {
            graph.addEdge(e);
        }
    }

    public List<Edge> getExtra() {
        return extra;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

}
