package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Set;

public class GearboxFilter implements VehicleFilter {

    private final Set<Gearbox> gearboxes;

    public GearboxFilter(Set<Gearbox> gearboxes) {
        this.gearboxes = gearboxes;
    }

    @Override
    public boolean isValid(VehicleDescription vehicle) {
        return !vehicle.getVehicleType().equals(VehicleType.CAR) || gearboxes.contains(vehicle.getGearbox());
    }
}
