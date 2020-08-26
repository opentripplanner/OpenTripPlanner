package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import java.util.List;

public abstract class EdgeWithParkingZones extends Edge {
//    TODO comments needed!!!!
    private final ParkingZoneInfo parkingZones = new ParkingZoneInfo();
    //    TODO comments needed!!!!
    private final ParkingZoneInfo parkingZonesEnabled = new ParkingZoneInfo();

    protected EdgeWithParkingZones(Vertex v1, Vertex v2) {
        super(v1, v2);
    }

    protected boolean canDropoffVehicleHere(VehicleDescription vehicle) {
        return !parkingZonesEnabled.appliesToVehicle(vehicle) || parkingZones.appliesToVehicle(vehicle);
    }

    public void updateParkingZones(List<SingleParkingZone> parkingZonesEnabled,
                                   List<SingleParkingZone> parkingZones) {
        this.parkingZonesEnabled.updateParkingZones(parkingZonesEnabled);
        this.parkingZones.updateParkingZones(parkingZones);
    }
}
