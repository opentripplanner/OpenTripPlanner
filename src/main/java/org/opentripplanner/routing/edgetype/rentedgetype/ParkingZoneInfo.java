package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;

import java.io.Serializable;
import java.util.List;

/**
 * This class enables disallowing dropping off vehicles outside of their parking zones.
 * For each pair <{@link Provider}, {@link VehicleType}> there can be different parking zones. If a given pair has
 * parking zones feature enabled, then we will have it in `parkingZonesEnabled` field. If we can park a vehicle of some
 * type and some provider in a given location, then we will have this pair of <provider, vehicleType>
 * in `parkingZones` field;
 */
public class ParkingZoneInfo implements Serializable {

    /**
     * Are we inside a parking zone for given provider and vehicleType
     */
    private final List<SingleParkingZone> parkingZones;

    /**
     * Does this provider and vehicleType have parking zones feature enabled?
     */
    private final List<SingleParkingZone> parkingZonesEnabled;

    public ParkingZoneInfo(List<SingleParkingZone> parkingZones, List<SingleParkingZone> parkingZonesEnabled) {
        this.parkingZones = parkingZones;
        this.parkingZonesEnabled = parkingZonesEnabled;
    }

    /**
     * Checks if we can dropoff given vehicle at this location. We allow to dropoff a vehicle if
     * 1. Their provider and vehicle type have disabled parking zones feature
     * 2. We are inside parking zone for that provider and vehicleType
     */
    public boolean canDropoffVehicleHere(VehicleDescription vehicle) {
        return hasProviderAndVehicleTypeDisabledParkingZonesFeature(vehicle)
                || areWeInsideParkingZoneForProviderAndVehicleType(vehicle);
    }

    private boolean hasProviderAndVehicleTypeDisabledParkingZonesFeature(VehicleDescription vehicle) {
        if (vehicle.isHubbable())
            return false;
        return parkingZonesEnabled.stream().noneMatch(pz -> pz.appliesToThisVehicle(vehicle));
    }

    private boolean areWeInsideParkingZoneForProviderAndVehicleType(VehicleDescription vehicle) {
        return parkingZones.stream().anyMatch(pz -> pz.appliesToThisVehicle(vehicle));
    }

}
