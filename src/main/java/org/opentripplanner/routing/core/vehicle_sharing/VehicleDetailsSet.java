package org.opentripplanner.routing.core.vehicle_sharing;

import com.google.common.collect.Sets;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class VehicleDetailsSet {

    private final Set<FuelType> fuelTypes;

    private final Set<Gearbox> gearboxes;

    private final Set<Provider> providers;

    public static final VehicleDetailsSet allowingAll;

    static {
        Set<FuelType> fuelTypes = Sets.newHashSet(FuelType.values());
        Set<Gearbox> gearboxes = Sets.newHashSet(Gearbox.values());
        Set<Provider> providers = Sets.newHashSet(Provider.values());
        allowingAll = new VehicleDetailsSet(fuelTypes, gearboxes, providers);
    }

    private VehicleDetailsSet(Set<FuelType> fuelTypes, Set<Gearbox> gearboxes, Set<Provider> providers) {
        this.fuelTypes = fuelTypes;
        this.gearboxes = gearboxes;
        this.providers = providers;
    }

    public VehicleDetailsSet(List<FuelType> fuelTypes, List<Gearbox> gearboxes, List<Provider> providers) {
        this.fuelTypes = !fuelTypes.isEmpty() ? Sets.newHashSet(fuelTypes) : allowingAll.fuelTypes;
        this.gearboxes = !gearboxes.isEmpty() ? Sets.newHashSet(gearboxes) : allowingAll.gearboxes;
        this.providers = !providers.isEmpty() ? Sets.newHashSet(providers) : allowingAll.providers;
    }

    public boolean isRentable(VehicleDescription vehicleDescription) {
        return fuelTypes.contains(vehicleDescription.getFuelType()) &&
                gearboxes.contains(vehicleDescription.getGearbox()) &&
                providers.contains(vehicleDescription.getProvider());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleDetailsSet that = (VehicleDetailsSet) o;
        return Objects.equals(fuelTypes, that.fuelTypes) &&
                Objects.equals(gearboxes, that.gearboxes) &&
                Objects.equals(providers, that.providers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fuelTypes, gearboxes, providers);
    }
}
