package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Vertex;

public class DropBikeEdge extends DropoffVehicleEdge {
    private BikeRentalStation station;

    public DropBikeEdge(Vertex v, BikeRentalStation station) {
        super(v);
        this.station = station;
    }

    @Override
    protected boolean canDropoffVehicleHere(VehicleDescription vehicle) {
        return station.isStationCompatible(vehicle) && station.spacesAvailable > 0;
    }
}
