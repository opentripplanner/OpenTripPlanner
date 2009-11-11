package org.opentripplanner.routing.location;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.Street;
import org.opentripplanner.gtfs.GtfsLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class StreetLocation {
    /*
     * Represents a location on a street, somewhere between the two end points. This is used when
     * computing the first and last segments of a trip.
     */
    public Edge street;

    public double location; // a number from 0 to 1 representing how far along the street the
                            // location is; 0 means the start vertex and 1 means the end vertex

    public Vertex vertex;
    Edge[] halfEdges;
    public StreetLocation(String name, Edge street, double location, boolean incoming) {
        assert 0 <= location;
        assert location <= 1;
        this.street = street;
        this.location = location;
        Coordinate startCoord = street.fromv.getCoordinate();
        Coordinate endCoord = street.tov.getCoordinate();
        
        double x = startCoord.x * (1 - location) + endCoord.x * location;
        double y = startCoord.y * (1 - location) + endCoord.y * location;
        
        vertex = new Vertex(name, x, y);
        double weight1 = GtfsLibrary.distance(y, x, startCoord.y, startCoord.x);
        double weight2 = GtfsLibrary.distance(y, x, endCoord.y, endCoord.x);
        String streetName = street.payload.getName();
        
        if (incoming) {
            halfEdges = new Edge[] { 
                    new Edge(street.fromv, vertex, new Street(streetName, streetName, weight1)),
                    new Edge(street.tov, vertex, new Street(streetName, streetName, weight2)) 
                    };
        } else {
            halfEdges = new Edge[] { 
                    new Edge(vertex, street.fromv, new Street(streetName, streetName, weight1)),
                    new Edge(vertex, street.tov, new Street(streetName, streetName, weight2)) 
                    };
        }
        for (Edge e: halfEdges) {
            vertex.addOutgoing(e);
        }
    }
    
    public Edge getFromEdge() {
        return halfEdges[0];
    }
    
    public Edge getToEdge() {
        return halfEdges[1];
    }

}
