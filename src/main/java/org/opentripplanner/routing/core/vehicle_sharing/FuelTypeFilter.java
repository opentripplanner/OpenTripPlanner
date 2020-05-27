package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Set;

public class FuelTypeFilter implements VehicleFilter {

    private final Set<FuelType> fuelTypes;

    public FuelTypeFilter(Set<FuelType> fuelTypes) {
        this.fuelTypes = fuelTypes;
    }

    @Override
    public boolean isValid(VehicleDescription vehicle) {
        return fuelTypes.contains(vehicle.getFuelType());
    }
}
