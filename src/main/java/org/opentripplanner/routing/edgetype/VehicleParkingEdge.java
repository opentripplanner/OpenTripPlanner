package org.opentripplanner.routing.edgetype;

import lombok.Getter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;

import org.locationtech.jts.geom.LineString;
import java.util.Locale;

/**
 * Parking a vehicle edge.
 */
public class VehicleParkingEdge extends Edge {

    private static final long serialVersionUID = 1L;

    @Getter
    private final VehicleParking vehicleParking;

    public VehicleParkingEdge(VehicleParkingEntranceVertex vehicleParkingEntranceVertex) {
        this(vehicleParkingEntranceVertex, vehicleParkingEntranceVertex);
    }

    public VehicleParkingEdge(VehicleParkingEntranceVertex fromVehicleParkingEntranceVertex, VehicleParkingEntranceVertex toVehicleParkingEntranceVertex) {
        super(fromVehicleParkingEntranceVertex, toVehicleParkingEntranceVertex);
        this.vehicleParking = fromVehicleParkingEntranceVertex.getVehicleParking();
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();

        if (!options.parkAndRide) {
            return null;
        }

        if (options.arriveBy) {
            return traverseUnPark(s0);
        } else {
            return traversePark(s0);
        }
    }

    protected State traverseUnPark(State s0) {
        RoutingRequest options = s0.getOptions();

        if (s0.getNonTransitMode() != TraverseMode.WALK || !s0.isVehicleParked()) {
            return null;
        }

        if (options.streetSubRequestModes.getBicycle()) {
            return traverseUnPark(s0, options.bikeParkCost, options.bikeParkTime, TraverseMode.BICYCLE);
        } else if (options.streetSubRequestModes.getCar()) {
            return traverseUnPark(s0, options.carParkCost, options.carParkTime, TraverseMode.CAR);
        } else {
            return null;
        }
    }

    private State traverseUnPark(State s0, int parkingCost, int parkingTime, TraverseMode mode) {
        RoutingRequest options = s0.getOptions();
        if (!isSpacesAvailable(mode, options.wheelchairAccessible)) {
            return null;
        }

        StateEditor s0e = s0.edit(this);
        s0e.incrementWeight(parkingCost);
        s0e.incrementTimeInSeconds(parkingTime);
        s0e.setVehicleParked(false, mode);
        return s0e.makeState();
    }

    private State traversePark(State s0) {
        RoutingRequest options = s0.getOptions();

        if (!options.streetSubRequestModes.getWalk() || s0.isVehicleParked()) {
            return null;
        }

        if (options.streetSubRequestModes.getBicycle()) {
            // Parking a rented bike is not allowed
            if (s0.isBikeRenting()) {
                return null;
            }

            return traversePark(s0, options.bikeParkCost, options.bikeParkTime);
        } else if (options.streetSubRequestModes.getCar()) {
            return traversePark(s0, options.carParkCost, options.carParkTime);
        } else {
            return null;
        }
    }

    private State traversePark(State s0, int parkingCost, int parkingTime) {
        RoutingRequest options = s0.getOptions();

        if (!isSpacesAvailable(s0.getNonTransitMode(), options.wheelchairAccessible)) {
            return null;
        }

        StateEditor s0e = s0.edit(this);
        s0e.incrementWeight(parkingCost);
        s0e.incrementTimeInSeconds(parkingTime);
        s0e.setVehicleParked(true, TraverseMode.WALK);
        return s0e.makeState();
    }

    private boolean isSpacesAvailable(TraverseMode traverseMode, boolean wheelchairAccessible) {
        switch (traverseMode) {
            case BICYCLE:
                return vehicleParking.hasBicyclePlaces();
            case CAR:
                return wheelchairAccessible ? vehicleParking.hasWheelchairAccessibleCarPlaces() : vehicleParking.hasCarPlaces();
            default:
                return false;
        }
    }

    @Override
    public double getDistanceMeters() {
        return 0;
    }

    @Override
    public LineString getGeometry() {
        return null;
    }

    @Override
    public String getName() {
        return getToVertex().getName();
    }

    @Override
    public String getName(Locale locale) {
        return getToVertex().getName(locale);
    }

    @Override
    public boolean hasBogusName() {
        return false;
    }

    public boolean equals(Object o) {
        if (o instanceof VehicleParkingEdge) {
            VehicleParkingEdge other = (VehicleParkingEdge) o;
            return other.getFromVertex().equals(fromv) && other.getToVertex().equals(tov);
        }
        return false;
    }

    public String toString() {
        return "VehicleParkingEdge(" + fromv + " -> " + tov + ")";
    }
}
