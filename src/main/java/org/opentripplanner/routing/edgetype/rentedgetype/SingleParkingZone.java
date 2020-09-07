package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.GeometryParkingZone;

import java.util.Objects;

public class SingleParkingZone {

    private final int providerId;

    private final VehicleType vehicleType;

    public SingleParkingZone(int providerId, VehicleType vehicleType) {
        this.providerId = providerId;
        this.vehicleType = vehicleType;
    }

    public boolean sameProviderIdAndVehicleType(GeometryParkingZone geometryParkingZone) {
        return providerId == geometryParkingZone.getProviderId()
                && vehicleType.equals(geometryParkingZone.getVehicleType());
    }

    boolean appliesToThisVehicle(VehicleDescription vehicle) {
        return vehicle.getProvider().getProviderId() == providerId && vehicle.getVehicleType().equals(vehicleType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, vehicleType);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SingleParkingZone) {
            return providerId == ((SingleParkingZone) other).providerId
                    && vehicleType == ((SingleParkingZone) other).vehicleType;
        }
        return false;
    }
}
