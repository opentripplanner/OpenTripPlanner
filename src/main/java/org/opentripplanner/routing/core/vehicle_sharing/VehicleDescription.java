package org.opentripplanner.routing.core.vehicle_sharing;


import org.opentripplanner.routing.core.TraverseMode;

public abstract class VehicleDescription {

    private final double longitude;
    private final double latitude;

    public VehicleDescription(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public abstract TraverseMode getTraverseMode();

    public abstract int getRentTimeInSeconds();

    public abstract int getDropoffTimeInSeconds();
}
