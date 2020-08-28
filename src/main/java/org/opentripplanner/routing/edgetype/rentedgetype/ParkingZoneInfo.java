package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.Provider;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.GeometryParkingZone;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

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
        return parkingZonesEnabled.stream().noneMatch(pz -> pz.appliesToThisVehicle(vehicle));
    }

    private boolean areWeInsideParkingZoneForProviderAndVehicleType(VehicleDescription vehicle) {
        return parkingZones.stream().anyMatch(pz -> pz.appliesToThisVehicle(vehicle));
    }

    public static class SingleParkingZone implements Serializable {

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

        private boolean appliesToThisVehicle(VehicleDescription vehicle) {
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
}
