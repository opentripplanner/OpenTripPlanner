package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

public class RentBikeEdge extends RentVehicleEdge {
    final private BikeRentalStation station;

    public RentBikeEdge(TemporaryRentVehicleVertex v, BikeRentalStation station) {
        super(v, station.getBikeFromStation());
        this.station = station;
    }

    @Override
    public String getName() {
        return "Rent bike at  " + station + " in node " + tov.getName();
    }

    @Override
    public State traverse(State state) {
        if (station.bikesAvailable <= 0) {
            return null;
        }
        if (state.getCurrentVehicle() == null) {
            return beginVehicleRenting(state);
        } else {
            return trySwitchVehicles(state);
        }
    }

    public BikeRentalStation getBikeRentalStation() {
        return station;
    }
}
