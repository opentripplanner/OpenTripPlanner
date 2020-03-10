package org.opentripplanner.updater.vehicle_sharing;

public class VehiclePosition {
    float longitude;

    public VehiclePosition(float longitude, float latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    float latitude;
}
