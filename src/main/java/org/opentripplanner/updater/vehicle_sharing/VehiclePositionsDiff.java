package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.vehicle_sharing.VehicleDescription;

import java.util.List;

public class VehiclePositionsDiff {

    public final List<VehicleDescription> appeared;
    public final Long previousUpdateTime;
    public final Long currentUpdateTime;

    public VehiclePositionsDiff(List<VehicleDescription> appeared, Long previousUpdateTime, Long currentUpdateTime) {
        this.appeared = appeared;
        this.previousUpdateTime = previousUpdateTime;
        this.currentUpdateTime = currentUpdateTime;
    }
}
