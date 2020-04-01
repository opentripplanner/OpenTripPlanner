package org.opentripplanner.routing.core.vehicle_sharing;


import org.opentripplanner.routing.core.TraverseMode;

public abstract class VehicleDescription {

    private final double longitude;
    private final double latitude;
    private final FuelType fuelType;
    private final Gearbox gearbox;
    private final Provider provider;

    public double getSpeedInMetersPerSecond() {
        return speedInMetersPerSecond;
    }

    private final double speedInMetersPerSecond=40;

    public VehicleDescription(double longitude, double latitude, FuelType fuelType, Gearbox gearbox, Provider provider) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.fuelType = fuelType;
        this.gearbox = gearbox;
        this.provider = provider;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    FuelType getFuelType() {
        return fuelType;
    }

    Gearbox getGearbox() {
        return gearbox;
    }

    Provider getProvider() {
        return provider;
    }

    public abstract TraverseMode getTraverseMode();

    public abstract int getRentTimeInSeconds();

    public abstract int getDropoffTimeInSeconds();
}
