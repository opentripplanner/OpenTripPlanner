package org.opentripplanner.routing.core.vehicle_sharing;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VehicleDetailsSet {

    private final Set<FuelType> fuelTypes;

    private final Set<Gearbox> gearboxes;

    private final Set<Provider> providers;

    private final Set<VehicleType> vehicleTypes;

    public static final VehicleDetailsSet allowingAll;

    static {
        Set<FuelType> fuelTypes = Sets.newHashSet(FuelType.values());
        Set<Gearbox> gearboxes = Sets.newHashSet(Gearbox.values());
        Set<Provider> providers = Sets.newHashSet(Provider.values());
        Set<VehicleType> vehicleTypes = Sets.newHashSet(VehicleType.values());
        allowingAll = new VehicleDetailsSet(fuelTypes, gearboxes, providers, vehicleTypes);
    }

    private VehicleDetailsSet(Set<FuelType> fuelTypes, Set<Gearbox> gearboxes, Set<Provider> providers, Set<VehicleType> vehicleTypes) {
        this.fuelTypes = fuelTypes;
        this.gearboxes = gearboxes;
        this.providers = providers;
        this.vehicleTypes = vehicleTypes;
    }

    public VehicleDetailsSet(List<FuelType> fuelTypes, List<Gearbox> gearboxes, List<Provider> providers, List<VehicleType> vehicleTypes) {
        this.fuelTypes = !fuelTypes.isEmpty() ? Sets.newHashSet(fuelTypes) : allowingAll.fuelTypes;
        this.gearboxes = !gearboxes.isEmpty() ? Sets.newHashSet(gearboxes) : allowingAll.gearboxes;
        this.providers = !providers.isEmpty() ? Sets.newHashSet(providers) : allowingAll.providers;
        this.vehicleTypes = !vehicleTypes.isEmpty() ? Sets.newHashSet(vehicleTypes) : allowingAll.vehicleTypes;
    }

    public boolean isRentable(VehicleDescription vehicleDescription) {
        return (vehicleDescription.getFuelType() == null || fuelTypes.contains(vehicleDescription.getFuelType())) &&
                (vehicleDescription.getGearbox() == null || gearboxes.contains(vehicleDescription.getGearbox())) &&
                (vehicleDescription.getProvider() == null || providers.contains(vehicleDescription.getProvider())) &&
                (vehicleDescription.getVehicleType() == null || vehicleTypes.contains(vehicleDescription.getVehicleType()));
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
