package org.opentripplanner.routing.vertextype;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.edgetype.rentedgetype.RentVehicleEdge;
import org.opentripplanner.routing.location.StreetLocation;

/**
 * Vertex, which represents actual location of rentable vehicle linked to graph. There should be one
 * {@link RentVehicleEdge}, from and to this vertex (creating a loop), which represents renting given vehicle.
 */
public class TemporaryRentVehicleVertex extends StreetLocation implements TemporaryVertex {

    public TemporaryRentVehicleVertex(String id, Coordinate nearestPoint, String name) {
        super(id, nearestPoint, name);
    }

    @Override
    public boolean isEndVertex() {
        return false;
    }
}
