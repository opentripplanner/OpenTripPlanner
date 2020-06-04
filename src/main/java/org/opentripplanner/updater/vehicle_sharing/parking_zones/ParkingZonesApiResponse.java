package org.opentripplanner.updater.vehicle_sharing.parking_zones;

import com.google.gson.JsonObject;

import java.util.List;

public class ParkingZonesApiResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public static class Data {

        private List<ParkingZone> parking_zones;

        public List<ParkingZone> getParking_zones() {
            return parking_zones;
        }

        public void setParking_zones(List<ParkingZone> parking_zones) {
            this.parking_zones = parking_zones;
        }
    }

    public static class ParkingZone {

        private int providerId;
        private String vehicleType;
        private boolean isAllowed;
        private List<Feature> polygons;

        public int getProviderId() {
            return providerId;
        }

        public void setProviderId(int providerId) {
            this.providerId = providerId;
        }

        public String getVehicleType() {
            return vehicleType;
        }

        public void setVehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
        }

        public boolean isAllowed() {
            return isAllowed;
        }

        public void setAllowed(boolean allowed) {
            isAllowed = allowed;
        }

        public List<Feature> getPolygons() {
            return polygons;
        }

        public void setPolygons(List<Feature> polygons) {
            this.polygons = polygons;
        }
    }

    public static class Feature {

        private String type;
        private Object properties;
        private JsonObject geometry;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Object getProperties() {
            return properties;
        }

        public void setProperties(Object properties) {
            this.properties = properties;
        }

        public JsonObject getGeometry() {
            return geometry;
        }

        public void setGeometry(JsonObject geometry) {
            this.geometry = geometry;
        }
    }
}
