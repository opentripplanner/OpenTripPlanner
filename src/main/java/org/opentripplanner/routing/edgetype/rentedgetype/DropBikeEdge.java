package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

/**
 * Edge allows dropping a bike on station. This edge is a loop on {@link TemporaryRentVehicleVertex} which, when traversed,
 * changes our current traverse mode, but leaves us in the same location. Dropping a bike on full station is impossible.
 */
public class DropBikeEdge extends DropoffVehicleEdge {
    private BikeRentalStation station;

    public DropBikeEdge(TemporaryRentVehicleVertex v, BikeRentalStation station) {
        super(v);
        this.station = station;
    }

    @Override
    protected boolean canDropoffVehicleHere(VehicleDescription vehicle) {
        return station.isStationCompatible(vehicle) && station.spacesAvailable > 0;
    }
}
