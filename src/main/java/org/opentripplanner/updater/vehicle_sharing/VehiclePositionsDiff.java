package org.opentripplanner.updater.vehicle_sharing;

import java.util.List;

public class VehiclePositionsDiff {

    List<VehiclePosition> appeared;
    List<VehiclePosition> disappeared;
    Long previousUpdateTime;
    Long currentUpdateTime;

    public VehiclePositionsDiff(List<VehiclePosition> appeared, List<VehiclePosition> disappeared, Long previousUpdateTime, Long currentUpdateTime) {
        this.appeared = appeared;
        this.disappeared = disappeared;
        this.previousUpdateTime = previousUpdateTime;
        this.currentUpdateTime = currentUpdateTime;
    }
}
