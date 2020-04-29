package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptySet;

public class VehicleDetailsSet {

    private final Set<FuelType> fuelTypes;

    private final Set<Gearbox> gearboxes;

    private final Set<String> providersDisallowed;

    private final Set<VehicleType> vehicleTypes;

    public static final VehicleDetailsSet allowingAll = new VehicleDetailsSet(emptySet(), emptySet(), emptySet(),
            emptySet());

    public VehicleDetailsSet(Set<FuelType> fuelTypes, Set<Gearbox> gearboxes, Set<String> providersDisallowed,
                             Set<VehicleType> vehicleTypes) {
        this.fuelTypes = fuelTypes;
        this.gearboxes = gearboxes;
        this.providersDisallowed = providersDisallowed;
        this.vehicleTypes = vehicleTypes;
    }

    public boolean isRentable(VehicleDescription vehicleDescription) {
        return (fuelTypes.isEmpty() || vehicleDescription.getFuelType() == null || fuelTypes.contains(vehicleDescription.getFuelType())) &&
                (gearboxes.isEmpty() || vehicleDescription.getGearbox() == null || gearboxes.contains(vehicleDescription.getGearbox())) &&
                (providersDisallowed.isEmpty() || vehicleDescription.getProvider() == null || !providersDisallowed.contains(vehicleDescription.getProvider().getName())) &&
                (vehicleTypes.isEmpty() || vehicleDescription.getVehicleType() == null || vehicleTypes.contains(vehicleDescription.getVehicleType()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleDetailsSet that = (VehicleDetailsSet) o;
        return Objects.equals(fuelTypes, that.fuelTypes) &&
                Objects.equals(gearboxes, that.gearboxes) &&
                Objects.equals(providersDisallowed, that.providersDisallowed) &&
                Objects.equals(vehicleTypes, that.vehicleTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fuelTypes, gearboxes, providersDisallowed, vehicleTypes);
    }
}
