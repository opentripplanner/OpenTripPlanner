package org.opentripplanner.updater.vehicle_sharing;

import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

public class VehiclePostionsDiff{
    List<VehiclePosition> appeared;
    List<VehiclePosition> disappeared;
    Long previousUpdateTime;
    Long currentUpdateTime;

    public VehiclePostionsDiff(List<VehiclePosition> appeared, List<VehiclePosition> disappeared, Long previousUpdateTime, Long currentUpdateTime) {
        this.appeared = appeared;
        this.disappeared = disappeared;
        this.previousUpdateTime = previousUpdateTime;
        this.currentUpdateTime = currentUpdateTime;
    }

}
