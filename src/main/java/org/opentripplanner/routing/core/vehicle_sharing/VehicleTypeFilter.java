package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Set;

public class VehicleTypeFilter implements VehicleFilter {

    private final Set<VehicleType> vehicleTypes;

    public VehicleTypeFilter(Set<VehicleType> vehicleTypes) {
        this.vehicleTypes = vehicleTypes;
    }

    @Override
    public boolean isValid(VehicleDescription vehicle) {
        return vehicleTypes.contains(vehicle.getVehicleType());
    }
}
