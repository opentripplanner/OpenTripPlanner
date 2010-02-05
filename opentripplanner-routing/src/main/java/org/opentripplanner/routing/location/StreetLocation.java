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

import java.util.List;
import java.util.LinkedList;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.linearref.LinearLocation;

/**
 * Represents a location on a street, somewhere between the two corners.
 * This is used when computing the first and last segments of a trip, for 
 * trips that start or end between two intersections.
 */
public class StreetLocation extends GenericVertex {
    
    private static final long serialVersionUID = 1L;

    public List<Street> streets;

    public LinearLocation location; /* a number from 0 to 1 representing how far along the street the
                               location is; 0 means the start vertex and 1 means the end vertex */

    /**
     * Creates a StreetLocation on the given street.  How far along is
     * controlled by the location parameter, which represents a distance 
     * along the edge between 0 (the from vertex) and 1 (the to vertex).
     *   
     * @param name
     * @param streets A List of nearby streets (which are presumed to be coincident/parallel edges).
     *                i.e. a list of edges which represent a physical real-world street.
     * @param location
     *
     * @return the new StreetLocation
     */
    public static StreetLocation createStreetLocation(String name, List<Street> streets, LinearLocation location) {

        Geometry g = streets.get(0).getGeometry();
        Coordinate nearestPoint = location.getCoordinate(g);

        return new StreetLocation(name, streets, location, nearestPoint.x, nearestPoint.y);
    }

    public static StreetLocation createStreetLocation(String name, Street street, LinearLocation location) {
        List<Street> streets = new LinkedList<Street>();
        streets.add(street);
        return createStreetLocation(name, streets, location);
    }

    private StreetLocation(String name, List<Street> streets, LinearLocation location, double x,
            double y) {
        super(name, x, y);
        this.streets = streets;
        this.location = location;

        for( Street street : streets ) {
            Vertex fromv = street.getFromVertex();
            Vertex tov = street.getToVertex();
            Coordinate startCoord = fromv.getCoordinate();
            Coordinate endCoord = tov.getCoordinate();

            String streetName = street.getName();

            double weight1 = DistanceLibrary.distance(y, x, startCoord.y, startCoord.x);
            double bicycleWeight1 = weight1 * street.bicycleSafetyEffectiveLength / street.length;

            double weight2 = DistanceLibrary.distance(y, x, endCoord.y, endCoord.x);
            double bicycleWeight2 = weight2 * street.bicycleSafetyEffectiveLength / street.length;

            Street e1 = new Street(fromv, this, streetName, streetName, weight1, bicycleWeight1, street.getTraversalPermission(), street.getWheelchairAccessible());
            e1.setGeometry(toLineString(fromv.getCoordinate(), this.getCoordinate()));
            addIncoming(e1);


            Street e2 = new Street(this, tov, streetName, streetName, weight2, bicycleWeight2, street.getTraversalPermission(), street.getWheelchairAccessible());
            e2.setGeometry(toLineString(this.getCoordinate(), tov.getCoordinate()));
            addOutgoing(e2);
        }
    }

    private LineString toLineString(Coordinate start, Coordinate end) {
        Coordinate[] coords = { start, end };
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING),
                4326);
        return factory.createLineString(coords);
    }

    public Vertex reify(Graph graph) {
        String name = getName();
        GenericVertex reified = new GenericVertex(getLabel(), getX(), getY(), name);
        graph.addVertex(reified);
        for (Edge e : getIncoming()) {
            Street s = (Street) e;
            Vertex fromv = e.getFromVertex();
            Street street = new Street(fromv, reified, name, name, s.getLength(), s.getBicycleSafetyEffectiveLength(), s.getTraversalPermission(), s.getWheelchairAccessible());
            street.setGeometry(s.getGeometry());
            graph.addEdge(street);
        }
        for (Edge e : getOutgoing()) {
            Street s = (Street) e;
            Vertex tov = e.getToVertex();
            Street street = new Street(reified, tov, name, name, s.getLength(), s.getBicycleSafetyEffectiveLength(), s.getTraversalPermission(), s.getWheelchairAccessible());
            street.setGeometry(s.getGeometry());
            graph.addEdge(street);
        }
        return reified;
    }
    
    @Override
    public String getName() {
        return super.getName() == null ? label : super.getName();
    }
}
