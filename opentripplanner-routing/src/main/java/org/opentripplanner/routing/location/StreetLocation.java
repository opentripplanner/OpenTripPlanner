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

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.OneStreetVertex;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;

/**
 * Represents a location on a street, somewhere between the two corners.
 * This is used when computing the first and last segments of a trip, for 
 * trips that start or end between two intersections.
 */
public class StreetLocation extends GenericVertex {
    
    private static final long serialVersionUID = 1L;

    /**
     * Creates a StreetLocation on the given street.  How far along is
     * controlled by the location parameter, which represents a distance 
     * along the edge between 0 (the from vertex) and 1 (the to vertex).
     *   
     * @param label
     * @param name
     * @param streets A List of nearby streets (which are presumed to be coincident/parallel edges).
     *                i.e. a list of edges which represent a physical real-world street.
     * @param nearestPoint
     *
     * @return the new StreetLocation
     */
    public static StreetLocation createStreetLocation(String label, String name, List<Street> streets, Coordinate nearestPoint) {
        return new StreetLocation(label, name, streets, nearestPoint);
    }

    public static StreetLocation createStreetLocation(String label, String name, Street street, Coordinate nearestPoint) {
        List<Street> streets = new LinkedList<Street>();
        streets.add(street);
        return createStreetLocation(label, name, streets, nearestPoint);
    }

    public List<Street> streets;
    public LinearLocation location;

    private StreetLocation(String label, String name, List<Street> streets, Coordinate nearestPoint) {
        super(label, nearestPoint.x, nearestPoint.y, name);

        this.streets = streets;
        for( Street street : streets ) {
            Vertex fromv = street.getFromVertex();
            Vertex tov = street.getToVertex();
            Coordinate startCoord = fromv.getCoordinate();
            Coordinate endCoord = tov.getCoordinate();

            LocationIndexedLine line = new LocationIndexedLine(street.getGeometry());
            LinearLocation l = line.indexOf(nearestPoint);
            if(location == null) location = l;

            LineString beginning = (LineString) line.extractLine(line.getStartIndex(), l);
            LineString ending    = (LineString) line.extractLine(l, line.getEndIndex());

            String streetName = street.getName();

            double weight1 = DistanceLibrary.distance(nearestPoint, startCoord);
            double bicycleWeight1 = weight1 * street.bicycleSafetyEffectiveLength / street.length;

            double weight2 = DistanceLibrary.distance(nearestPoint, endCoord);
            double bicycleWeight2 = weight2 * street.bicycleSafetyEffectiveLength / street.length;

            Street e1 = new Street(fromv, this, streetName, streetName, weight1, bicycleWeight1, street.getTraversalPermission(), street.getWheelchairAccessible());
            e1.setGeometry(beginning);
            e1.setElevationProfile(street.getElevationProfile(0, l.getSegmentFraction()*street.length));
            addIncoming(e1);


            Street e2 = new Street(this, tov, streetName, streetName, weight2, bicycleWeight2, street.getTraversalPermission(), street.getWheelchairAccessible());
            e2.setGeometry(ending);
            e2.setElevationProfile(street.getElevationProfile(l.getSegmentFraction()*street.length, street.length));
            addOutgoing(e2);
        }
    }

    public void reify(Graph graph) {
        if(streets == null)
            return;

        /* first, figure out where the endpoints are  */
        Vertex v1 = null, v2 = null;
        for (Street e : streets) {
            if (v1 == null && v2 != e.getToVertex()) {
                v1 = e.getToVertex();
            } else if( v1 == null && v2 != e.getFromVertex()) {
                v1 = e.getFromVertex();
            }
            if( v2 == null && v1 != e.getToVertex()) {
                v2 = e.getToVertex();
            } else if( v2 == null && v1 != e.getFromVertex()) {
                v2 = e.getFromVertex();
            }
        }

        /* now, reconfigure vertices to remove the street that this vertex splits
         * and add the streets that replace it.
         */
        for (Edge e : getIncoming()) {
            if(!(e instanceof Street))
                continue;

            Street s = (Street) e;
            Vertex intersection = s.getFromVertex();
            if(intersection != v1 && intersection != v2)
                continue;

            if (intersection instanceof OneStreetVertex) {
                ((OneStreetVertex) intersection).outStreet = s;
            } else {
                /* From a given vertex, both the incoming and outgoing edges which
                 * were split need to be removed, otherwise "dangling" edges are left
                 * on some vertices. (<streets> contains the split edges)
                 * The 'new' replacement edge also needs to be added as an outgoing
                 * edge to the intersection.
                 */
                GenericVertex v = (GenericVertex) intersection;

                Vector<Edge> outgoing = v.getOutgoing();
                Iterator<Edge> it_outgoing = outgoing.iterator();
                while(it_outgoing.hasNext()) {
                    Edge e2 = it_outgoing.next();
                    if (streets.contains(e2)) {
                        it_outgoing.remove();
                    }
                }

                Vector<Edge> incoming = v.getIncoming();
                Iterator<Edge> it_incoming = incoming.iterator();
                while(it_incoming.hasNext()) {
                    Edge e2 = it_incoming.next();
                    if (streets.contains(e2)) {
                        it_incoming.remove();
                    }
                }

                if(!outgoing.contains(e))
                    outgoing.add(e);

                v.setIncoming(incoming);
                v.setOutgoing(outgoing);
            }
        }
        for (Edge e : getOutgoing()) {
            if(!(e instanceof Street))
                continue;

            Street s = (Street) e;
            Vertex intersection = s.getToVertex();
            if(intersection != v1 && intersection != v2)
                continue;

            if (intersection instanceof OneStreetVertex) {
                ((OneStreetVertex) intersection).inStreet = s;
            } else {
                GenericVertex v = (GenericVertex) intersection;

                Vector<Edge> outgoing = v.getOutgoing();
                Iterator<Edge> it_outgoing = outgoing.iterator();
                while(it_outgoing.hasNext()) {
                    Edge e2 = it_outgoing.next();
                    if (streets.contains(e2)) {
                        it_outgoing.remove();
                    }
                }

                Vector<Edge> incoming = v.getIncoming();
                Iterator<Edge> it_incoming = incoming.iterator();
                while(it_incoming.hasNext()) {
                    Edge e2 = it_incoming.next();
                    if (streets.contains(e2)) {
                        it_incoming.remove();
                    }
                }

                if(!incoming.contains(e))
                    incoming.add(e);

                v.setIncoming(incoming);
                v.setOutgoing(outgoing);
            }
        }
        graph.addVertex(this);
        location = null;
    }
    
    @Override
    public String getName() {
        return super.getName() == null ? label : super.getName();
    }
}
