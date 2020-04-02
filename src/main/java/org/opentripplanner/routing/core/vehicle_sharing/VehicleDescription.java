package org.opentripplanner.routing.core.vehicle_sharing;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.routing.core.TraverseMode;

public abstract class VehicleDescription {

    private final double longitude;
    private final double latitude;

    @JsonSerialize
    private final FuelType fuelType;

    @JsonSerialize
    private final Gearbox gearbox;

    @JsonSerialize
    private final Provider provider;

    public VehicleDescription(double longitude, double latitude, FuelType fuelType, Gearbox gearbox, Provider provider) {
        this.longitude = longitude;
        this.latitude = latitude;
        this.fuelType = fuelType;
        this.gearbox = gearbox;
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "VehicleDescription{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", fuelType=" + fuelType +
                ", gearbox=" + gearbox +
                ", provider=" + provider +
                '}';
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

    public abstract double getMaxSpeedInMetersPerSecond();

    public abstract TraverseMode getTraverseMode();

    public abstract int getRentTimeInSeconds();

    public abstract int getDropoffTimeInSeconds();

    @JsonSerialize
    public abstract VehicleType getVehicleType();
}
