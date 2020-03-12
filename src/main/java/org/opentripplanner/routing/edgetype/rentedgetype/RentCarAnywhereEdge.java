package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;
import org.opentripplanner.routing.graph.Vertex;

import java.util.LinkedList;
import java.util.List;

public class RentCarAnywhereEdge extends RentVehicleAnywhereEdge {
    int rentTimeInMinutes;
    int dropoffTimeInMinutes;



    public RentCarAnywhereEdge(Vertex v, int rentTimeInMinutes, int dropoffTimeInMinutes) {
        super(v);
        this.rentTimeInMinutes = rentTimeInMinutes;
        this.dropoffTimeInMinutes = dropoffTimeInMinutes;
        avaiableVehicles = new LinkedList<>();
    }

    @Override
    public TraverseMode traverseMode() {
        return TraverseMode.CAR;
    }

    @Override
    protected int getRentTimeInSeconds() {
        return rentTimeInMinutes * 60;
    }

    @Override
    protected int getDropoffTimeInSeconds() {
        return dropoffTimeInMinutes * 60;
    }

    @Override
    boolean isAvailable(long time) {
        return !avaiableVehicles.isEmpty();
    }

    @Override
    VehicleDescription getVehicle(long time) {
        return avaiableVehicles.get(0);
    }

}
