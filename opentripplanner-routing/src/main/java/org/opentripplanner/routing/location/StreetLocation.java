package org.opentripplanner.routing.location;

import java.util.ArrayList;
import java.util.Vector;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class StreetLocation extends GenericVertex {
    /*
     * Represents a location on a street, somewhere between the two end points. This is used when
     * computing the first and last segments of a trip, for trips that start between two
     * intersections.
     */
    public Edge street;

    public double location; /* a number from 0 to 1 representing how far along the street the
                               location is; 0 means the start vertex and 1 means the end vertex */

    ArrayList<Edge> incoming = new ArrayList<Edge>();

    ArrayList<Edge> outgoing = new ArrayList<Edge>();

    public static StreetLocation createStreetLocation(String name, Edge street, double location,
            boolean incoming) {

        Vertex fromv = street.getFromVertex();
        Vertex tov = street.getToVertex();
        Coordinate startCoord = fromv.getCoordinate();
        Coordinate endCoord = tov.getCoordinate();
        double x = startCoord.x * (1 - location) + endCoord.x * location;
        double y = startCoord.y * (1 - location) + endCoord.y * location;

        return new StreetLocation(name, street, location, incoming, x, y);
    }

    private StreetLocation(String name, Edge street, double location, boolean incoming, double x,
            double y) {
        super(name, x, y);

        assert 0 <= location;
        assert location <= 1;

        Vertex fromv = street.getFromVertex();
        Vertex tov = street.getToVertex();
        Coordinate startCoord = fromv.getCoordinate();
        Coordinate endCoord = tov.getCoordinate();
        this.street = street;
        this.location = location;

        String streetName = street.getName();

        double weight1 = DistanceLibrary.distance(y, x, startCoord.y, startCoord.x);
        double weight2 = DistanceLibrary.distance(y, x, endCoord.y, endCoord.x);

        if (incoming) {
            Street e1 = new Street(fromv, this, streetName, streetName, weight1);
            addIncoming(e1);
            Street e2 = new Street(tov, this, streetName, streetName, weight2);
            addIncoming(e2);
        } else {
            Street e1 = new Street(this, fromv, streetName, streetName, weight1);
            addOutgoing(e1);
            Street e2 = new Street(this, tov, streetName, streetName, weight2);
            addOutgoing(e2);
        }
    }
}
