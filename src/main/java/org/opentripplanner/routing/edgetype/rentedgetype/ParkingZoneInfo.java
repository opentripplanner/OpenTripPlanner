package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;
import org.opentripplanner.updater.vehicle_sharing.parking_zones.GeometryParkingZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParkingZoneInfo {

    private final List<SingleParkingZone> parkingZones = new ArrayList<>();

    public void updateParkingZones(List<SingleParkingZone> parkingZones) {
        this.parkingZones.clear();
        this.parkingZones.addAll(parkingZones);
    }

    public boolean appliesToVehicle(VehicleDescription vehicle) {
        return parkingZones.stream().anyMatch(pz -> pz.appliesToThisVehicle(vehicle));
    }

    public static class SingleParkingZone {

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
            return vehicle.getProviderId() == providerId && vehicle.getVehicleType().equals(vehicleType);
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
