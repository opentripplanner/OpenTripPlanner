package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

import static java.util.Collections.emptyList;

public abstract class EdgeWithParkingZones extends Edge {

    private ParkingZoneInfo parkingZones = new ParkingZoneInfo(emptyList(), emptyList());

    protected EdgeWithParkingZones(Vertex v) {
        super(v, v);
    }

    protected boolean canDropoffVehicleHere(VehicleDescription vehicle) {
        return parkingZones.canDropoffVehicleHere(vehicle);
    }

    public void setParkingZones(ParkingZoneInfo parkingZones) {
        this.parkingZones = parkingZones;
    }
}
