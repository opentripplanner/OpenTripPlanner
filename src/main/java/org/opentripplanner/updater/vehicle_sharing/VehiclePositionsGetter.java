package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;

public class VehiclePositionsGetter {
    long lastUpdate;
    TraverseMode MODE;

    public VehiclePositionsGetter(TraverseMode MODE) {
        this.MODE = MODE;
        lastUpdate=-1;
    }

    VehiclePositionsDiff getVehiclePositionsDiff() {
        // TODO
        return null;
    }
}
