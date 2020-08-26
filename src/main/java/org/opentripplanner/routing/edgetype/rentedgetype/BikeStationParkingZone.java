package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;

public class BikeStationParkingZone extends SingleParkingZone {
    private BikeRentalStation bikeRentalStation;

    public BikeStationParkingZone(BikeRentalStation station) {
        super(station.provider.getProviderId(), VehicleType.BIKE);
    }

    @Override
    boolean appliesToThisVehicle(VehicleDescription vehicle) {
        return bikeRentalStation.spacesAvailable > 0 && super.appliesToThisVehicle(vehicle);
    }
}
