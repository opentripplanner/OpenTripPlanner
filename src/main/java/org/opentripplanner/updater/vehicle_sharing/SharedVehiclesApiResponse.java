package org.opentripplanner.updater.vehicle_sharing;

import java.util.List;

public class SharedVehiclesApiResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private List<Vehicle> vehicles;

        public List<Vehicle> getVehicles() {
            return vehicles;
        }

        public void setVehicles(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
        }
    }

    public static class Vehicle {

        private String providerVehicleId;
        private double longitude;
        private double latitude;
        private String fuelType;
        private String gearbox;
        private String type;
        private VehicleProvider provider;

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
    }

    public static class VehicleProvider {

        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
