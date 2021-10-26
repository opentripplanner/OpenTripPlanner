package org.opentripplanner.routing.vertextype;

import lombok.Getter;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParking.VehicleParkingEntrance;

/**
 * A vertex for a bike park.
 * 
 * Connected to streets by StreetBikeParkLink. Transition for parking the bike is handled by
 * BikeParkEdge.
 */
public class VehicleParkingEntranceVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    @Getter
    private final VehicleParking.VehicleParkingEntrance parkingEntrance;

    public VehicleParkingEntranceVertex(
            Graph g,
            VehicleParkingEntrance parkingEntrance
    ) {
        super(g, "Vehicle parking " + parkingEntrance.getEntranceId(), parkingEntrance.getX(), parkingEntrance.getY(), parkingEntrance.getName());
        this.parkingEntrance = parkingEntrance;
    }

    public VehicleParking getVehicleParking() {
        return parkingEntrance.getVehicleParking();
    }

    public boolean isCarAccessible() {
        return parkingEntrance.isCarAccessible();
    }

    public boolean isWalkAccessible() {
        return parkingEntrance.isWalkAccessible();
    }
}
