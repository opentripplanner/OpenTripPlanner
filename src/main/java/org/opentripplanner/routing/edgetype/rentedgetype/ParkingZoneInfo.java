package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;

import java.util.ArrayList;
import java.util.List;

public class ParkingZoneInfo {

    private final List<SingleParkingZone> parkingZones = new ArrayList<>();

    public void updateParkingZones(List<SingleParkingZone> parkingZones) {
        this.parkingZones.clear();
        this.parkingZones.addAll(parkingZones);
    }

    public boolean appliesToVehicle(VehicleDescription vehicle) {
        return parkingZones.stream().anyMatch(pz -> pz.appliesToThisVehicle(vehicle));
    }

}
