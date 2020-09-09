package org.opentripplanner.hasura_client.hasura_objects;

import org.opentripplanner.updater.vehicle_sharing.vehicles_positions.VehicleProvider;

public class Vehicle extends HasuraObject {

    private String providerVehicleId;
    private double longitude;
    private double latitude;
    private String fuelType;
    private String gearbox;
    private String type;
    private VehicleProvider provider;
    private Double range;

    public Double getRangeInMeters() {
        if (range != null)
            return range * 1000;
        else
            return null;
    }

    public String getProviderVehicleId() {
        return providerVehicleId;
    }

    public void setProviderVehicleId(String providerVehicleId) {
        this.providerVehicleId = providerVehicleId;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public String getGearbox() {
        return gearbox;
    }

    public void setGearbox(String gearbox) {
        this.gearbox = gearbox;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public VehicleProvider getProvider() {
        return provider;
    }

    public void setProvider(VehicleProvider provider) {
        this.provider = provider;
    }

    public void setRange(Double range) {
        this.range = range;
    }
}
