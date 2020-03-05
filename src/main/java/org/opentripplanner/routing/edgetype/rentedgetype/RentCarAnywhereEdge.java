package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Vertex;

public class RentCarAnywhereEdge extends RentVehicleAnywhereEdge {
    int rentTimeInMinutes;
    int dropoffTimeInMinutes;

    protected RentCarAnywhereEdge(Vertex v1, Vertex v2) {
        super(v1, v2);
    }

    public RentCarAnywhereEdge(Vertex v1, Vertex v2, int rentTimeInMinutes, int dropoffTimeInMinutes) {
        super(v1, v2);
        this.rentTimeInMinutes = rentTimeInMinutes;
        this.dropoffTimeInMinutes = dropoffTimeInMinutes;
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

}
