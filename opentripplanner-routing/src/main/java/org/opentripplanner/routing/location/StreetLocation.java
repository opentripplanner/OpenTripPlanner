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
import org.opentripplanner.routing.edgetype.EndpointVertex;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.StreetVertex;
import org.opentripplanner.routing.edgetype.TurnEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * Represents a location on a street, somewhere between the two corners. This is used when computing
 * the first and last segments of a trip, for trips that start or end between two intersections. Also
 * for situating bus stops in the middle of street segments.
 */
public class StreetLocation extends GenericVertex {

    private ArrayList<Edge> extra = new ArrayList<Edge>();
    private Coordinate nearestPoint;
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
     * This is a little bit complicated, because TurnEdges and OutEdges get most of their data
     * from their fromVertex.  So we have to create a separate fromVertex located at the start
     * of the set of turns, with a free edge into it from the from vertex.
     * 
     * @param label
     * @param name
     * @param turns
     *            A List of nearby edges (which are presumed to be coincident/parallel edges).
     *            i.e. a list of edges which represent a physical real-world street in one direction.
     * @param nearestPoint
     * 
     * @return the new StreetLocation
     */
    public static StreetLocation createStreetLocation(Graph graph, String label, String name, Collection<Edge> turns,
            Coordinate nearestPoint) {
               
        boolean wheelchairAccessible = false;
        
        /* group edges by from vertex */
        HashMap<Vertex, ArrayList<Edge>> counts = new HashMap<Vertex, ArrayList<Edge>>();
        
        for (Edge street : turns) {
            Vertex fromv = street.getFromVertex();
            if (!(fromv instanceof StreetVertex)) {
                continue;
            }
            if (!counts.containsKey(fromv)) {
                counts.put(fromv, new ArrayList<Edge>());
            }
            counts.get(fromv).add(street);
            wheelchairAccessible |= ((StreetVertex) fromv).isWheelchairAccessible();
        }
        
        List<Edge> streetsUp = null;
        List<Edge> streetsDown = null;
        for (ArrayList<Edge> streetset : counts.values()) {
            if (streetsUp == null) {
                streetsUp = streetset; 
            } else if (streetsDown == null) {
                if (streetsUp.size() < streetset.size()) {
                    streetsDown = streetsUp;
                    streetsUp = streetset;
                } else {
                    streetsDown = streetset;
                }
            } else if (streetsUp.size() < streetset.size()) {
                streetsDown = streetsUp;
                streetsUp = streetset;
            } else if (streetsDown.size() < streetset.size()) {
                streetsDown = streetset;
            }
        }
 
        /* linking vertex with epsilon transitions */
        StreetLocation location = new StreetLocation(label, nearestPoint, name);
        location.nearestPoint = nearestPoint;
        location.setWheelchairAccessible(wheelchairAccessible);
        
        /* forward edges and vertices */
        StreetVertex location1 = createHalfLocation(graph, location, label + " up", name, nearestPoint, streetsUp);     

        Edge l1in = new FreeEdge(location, location1);
        Edge l1out = new FreeEdge(location1, location);
        location.extra.add(l1in);
        location.extra.add(l1out);
       
        if (streetsDown != null) {
            StreetVertex location2 = createHalfLocation(graph, location, label + " down", name, nearestPoint, streetsDown);
            Edge l2in = new FreeEdge(location, location2);
            Edge l2out = new FreeEdge(location2, location);
            location.extra.add(l2in);
            location.extra.add(l2out);
        }        
        
        return location;
        
    }

    private void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    @Override
    public double getX() {
        return nearestPoint.x;
    }
    
    @Override
    public double getY() {
        return nearestPoint.y;
    }
    
    @Override
    public Coordinate getCoordinate() {
        return nearestPoint;
    }
    
    private static StreetVertex createHalfLocation(Graph graph, StreetLocation base, String label, String name, Coordinate nearestPoint,
            List<Edge> streets) {
        
        StreetVertex fromv = (StreetVertex) streets.get(0).getFromVertex();
        P2<LineString> geometries = getGeometry(fromv, nearestPoint);
        
        double totalGeomLength = fromv.getGeometry().getLength();
        double lengthRatioIn = geometries.getFirst().getLength() / totalGeomLength;

        double lengthIn = fromv.getLength() * lengthRatioIn;
        double lengthOut = fromv.getLength() * (1 - lengthRatioIn);

        StreetVertex newFrom = new StreetVertex(label + " from", geometries.getFirst(), name, lengthIn, false);
        newFrom.setElevationProfile(fromv.getElevationProfile(0, lengthIn));
        newFrom.setPermission(fromv.getPermission());
        
        StreetVertex location = new StreetVertex(label, geometries.getSecond(), name, lengthOut, false);
        location.setElevationProfile(fromv.getElevationProfile(lengthIn, totalGeomLength));
        location.setPermission(fromv.getPermission());
        
        FreeEdge free = new FreeEdge(fromv, newFrom);
        TurnEdge incoming = new TurnEdge(newFrom, location);
        
        base.extra.add(free);
        base.extra.add(incoming);
        
        for (Edge street : streets) {
            Vertex tov = street.getToVertex();
            Edge e;
            if (tov instanceof StreetVertex) {
                e = new TurnEdge(location, (StreetVertex) tov);
            } else {
                e = new OutEdge(location, (EndpointVertex) tov);
            }
            base.extra.add(e);
        }
        return location;
    }

    private static P2<LineString> getGeometry(StreetVertex fromv, Coordinate nearestPoint) {
        StreetVertex startVertex = fromv;

        LocationIndexedLine line = new LocationIndexedLine(startVertex.getGeometry());
        LinearLocation l = line.indexOf(nearestPoint);

        LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
        LineString ending = (LineString) line.extractLine(l, line.getEndIndex());

        return new P2<LineString>(beginning, ending);
        
    }

//    @SuppressWarnings("unchecked")
    public void reify(Graph graph) {
        if (graph.getVertex(label) != null) {
            //already reified
            return; 
        }

        for (Edge e: extra) {
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
