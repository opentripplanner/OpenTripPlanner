package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingEntrance;

/**
 * A vertex for a vehicle parking entrance.
 * 
 * Connected to streets by {@link org.opentripplanner.routing.edgetype.StreetVehicleParkingLink}.
 * Transition for parking the bike is handled by {@link org.opentripplanner.routing.edgetype.VehicleParkingEdge}.
 */
public class VehicleParkingEntranceVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    private final VehicleParkingEntrance parkingEntrance;

    public VehicleParkingEntranceVertex(
            Graph g,
            VehicleParkingEntrance parkingEntrance
    ) {
        super(g, "Vehicle parking " + parkingEntrance.getEntranceId(), parkingEntrance.getX(), parkingEntrance.getY(), parkingEntrance.getName());
        this.parkingEntrance = parkingEntrance;
    }

    public VehicleParkingEntrance getParkingEntrance() {
        return parkingEntrance;
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
