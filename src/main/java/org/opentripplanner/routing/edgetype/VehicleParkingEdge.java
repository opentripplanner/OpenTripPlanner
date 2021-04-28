package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;

import org.locationtech.jts.geom.LineString;
import java.util.Locale;

/**
 * Parking a bike edge.
 * 
 * Note: There is an edge only in the "park" direction. We do not handle (yet) unparking a bike, as
 * you would need to know where you have parked your car, and is probably better handled by the
 * client by issuing two requests (first one from your origin to your bike, second one from your
 * bike to your destination).
 * 
 * Cost is the time to park a bike, estimated.
 */
public class VehicleParkingEdge extends Edge {

    private static final long serialVersionUID = 1L;

    public VehicleParkingEdge(VehicleParkingVertex vehicleParkingVertex) {
        super(vehicleParkingVertex, vehicleParkingVertex);
    }

    @Override
    public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        if (options.arriveBy) {
            return traverseUnPark(s0);
        } else {
            return traversePark(s0);
        }
    }

    protected State traverseUnPark(State s0) {
        RoutingRequest options = s0.getOptions();

        if (s0.getNonTransitMode() != TraverseMode.WALK) {
            return null;
        }

        if (options.streetSubRequestModes.getBicycle()) {
            return traverseUnParkWithBicycle(s0);
        } else if (options.streetSubRequestModes.getCar()) {
            return traverseUnParkWithCar(s0);
        } else {
            return null;
        }
    }

    private State traverseUnParkWithBicycle(State s0) {
        RoutingRequest options = s0.getOptions();

        if (!options.bikeParkAndRide) {
            return null;
        }

        StateEditor s0e = s0.edit(this);
        s0e.incrementWeight(options.bikeParkCost);
        s0e.incrementTimeInSeconds(options.bikeParkTime);
        s0e.setBikeParked(false);
        return s0e.makeState();
    }

    private State traverseUnParkWithCar(State s0) {
        RoutingRequest options = s0.getOptions();

        if (!options.parkAndRide) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(options.carDropoffTime);
        s1.incrementTimeInSeconds(options.carDropoffTime);
        s1.setCarParked(false);
        return s1.makeState();
    }

    private State traversePark(State s0) {
        switch (s0.getNonTransitMode()) {
            case BICYCLE:
                return traverseParkWithBicycle(s0);
            case CAR:
                return traverseParkWithCar(s0);
            default:
                return null;
        }
    }

    private State traverseParkWithBicycle(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * To park a bike, we need to be riding one, (not rented) and be allowed to walk and to park
         * it.
         */
        if (s0.getNonTransitMode() != TraverseMode.BICYCLE || !options.streetSubRequestModes.getWalk()
            || s0.isBikeRenting() || s0.isBikeParked() || !options.bikeParkAndRide
            || !spacesAvailableForMode(TraverseMode.BICYCLE, false)) {
            return null;
        }

        StateEditor s0e = s0.edit(this);
        s0e.incrementWeight(options.bikeParkCost);
        s0e.incrementTimeInSeconds(options.bikeParkTime);
        s0e.setBikeParked(true);
        return s0e.makeState();
    }

    private boolean spacesAvailableForMode(TraverseMode traverseMode, boolean wheelchairAccessible) {
        VehicleParkingVertex vehicleParkingVertex = (VehicleParkingVertex) tov;
        return vehicleParkingVertex.isSpacesAvailable(traverseMode, wheelchairAccessible);
    }

    private State traverseParkWithCar(State s0) {
        RoutingRequest options = s0.getOptions();
        /*
         * To park a car, we need to be in one and have allowed walk modes.
         */
        if (s0.getNonTransitMode() != TraverseMode.CAR || !options.streetSubRequestModes.getWalk()
            || s0.isCarParked() || !options.parkAndRide
            || !spacesAvailableForMode(TraverseMode.CAR, options.wheelchairAccessible)) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(options.carDropoffTime);
        s1.incrementTimeInSeconds(options.carDropoffTime);
        s1.setCarParked(true);
        return s1.makeState();
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
