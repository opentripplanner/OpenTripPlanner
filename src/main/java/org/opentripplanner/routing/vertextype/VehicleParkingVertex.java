package org.opentripplanner.routing.vertextype;

import lombok.Getter;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

/**
 * A vertex for a bike park.
 * 
 * Connected to streets by StreetBikeParkLink. Transition for parking the bike is handled by
 * BikeParkEdge.
 * 
 * Bike park-and-ride and "OV-fiets mode" development has been funded by GoAbout
 * (https://goabout.com/).
 * 
 * @author laurent
 * @author GoAbout
 * 
 */
public class VehicleParkingVertex extends Vertex {

    private static final long serialVersionUID = 1L;

    @Getter
    private final VehicleParking vehicleParking;

    public VehicleParkingVertex(Graph g, VehicleParking vehicleParking) {
        //TODO: localize vehicle parking
        super(g, "Vehicle parking " + vehicleParking.getId(), vehicleParking.getX(), vehicleParking.getY(), vehicleParking.getName());
        this.vehicleParking = vehicleParking;
    }

    public boolean isSpacesAvailable(TraverseMode traverseMode, boolean wheelchairAccessible) {
        switch (traverseMode) {
            case BICYCLE:
                return vehicleParking.hasBicyclePlaces();
            case CAR:
                return wheelchairAccessible ? vehicleParking.hasWheelchairAccessibleCarPlaces() : vehicleParking.hasCarPlaces();
            default:
                return false;
        }
    }
}
