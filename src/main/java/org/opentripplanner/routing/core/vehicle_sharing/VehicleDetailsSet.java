package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Objects;
import java.util.Set;

public class VehicleDetailsSet {

    private final Set<FuelType> fuelTypes;

    private final Set<Gearbox> gearboxes;

    private final Set<Integer> providers;

    private final Set<VehicleType> vehicleTypes;

    public VehicleDetailsSet(Set<FuelType> fuelTypes, Set<Gearbox> gearboxes, Set<Integer> providers, Set<VehicleType> vehicleTypes) {
        this.fuelTypes = fuelTypes;
        this.gearboxes = gearboxes;
        this.providers = providers;
        this.vehicleTypes = vehicleTypes;
    }

    public boolean isRentable(VehicleDescription vehicleDescription) {
        return (fuelTypes.isEmpty() || vehicleDescription.getFuelType() == null || fuelTypes.contains(vehicleDescription.getFuelType())) &&
                (gearboxes.isEmpty() || vehicleDescription.getGearbox() == null || gearboxes.contains(vehicleDescription.getGearbox())) &&
                (providers.isEmpty() || vehicleDescription.getProvider() == null || providers.contains(vehicleDescription.getProvider().getId())) &&
                (vehicleTypes.isEmpty() || vehicleDescription.getVehicleType() == null || vehicleTypes.contains(vehicleDescription.getVehicleType()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleDetailsSet that = (VehicleDetailsSet) o;
        return Objects.equals(fuelTypes, that.fuelTypes) &&
                Objects.equals(gearboxes, that.gearboxes) &&
                Objects.equals(providers, that.providers) &&
                Objects.equals(vehicleTypes, that.vehicleTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fuelTypes, gearboxes, providers, vehicleTypes);
    }
}
