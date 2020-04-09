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

        private long id;
        private double longitude;
        private double latitude;
        private String fuelType;
        private String gearbox;
        private String type;
        private int providerId;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
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

        public int getProviderId() {
            return providerId;
        }

        public void setProviderId(int providerId) {
            this.providerId = providerId;
        }
    }
}
