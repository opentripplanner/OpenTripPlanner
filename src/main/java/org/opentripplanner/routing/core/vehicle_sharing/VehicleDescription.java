package org.opentripplanner.routing.core.vehicle_sharing;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.routing.core.TraverseMode;

public abstract class VehicleDescription {

    private final long id;
    private final double longitude;
    private final double latitude;

    @JsonSerialize
    private final FuelType fuelType;

    @JsonSerialize
    private final Gearbox gearbox;

    @JsonSerialize
    private final Provider provider;

    public VehicleDescription(long id, double longitude, double latitude, FuelType fuelType, Gearbox gearbox, Provider provider) {
        this.id = id;
        this.longitude = longitude;
        this.latitude = latitude;
        this.fuelType = fuelType;
        this.gearbox = gearbox;
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "VehicleDescription{" +
                "id=" + id +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", fuelType=" + fuelType +
                ", gearbox=" + gearbox +
                ", provider=" + provider +
                '}';
    }

    public long getId() {
        return id;
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

    @JsonSerialize
    public int getProviderId() {
        return Provider.toId(provider);
    }

    @JsonIgnore
    public abstract double getMaxSpeedInMetersPerSecond();

    @JsonIgnore
    public abstract TraverseMode getTraverseMode();

    @JsonIgnore
    public abstract int getRentTimeInSeconds();

    @JsonIgnore
    public abstract int getDropoffTimeInSeconds();

    @JsonSerialize
    public abstract VehicleType getVehicleType();
}
