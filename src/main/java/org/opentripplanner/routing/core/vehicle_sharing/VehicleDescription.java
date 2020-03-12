package org.opentripplanner.routing.core.vehicle_sharing;


import org.opentripplanner.routing.core.TraverseMode;

public class VehicleDescription  {
    private TraverseMode traverseMode;
    private int maxSpeedInMPerSec;
    private int rangeInM;
    private boolean automaticGear;
    private boolean isRented;
    float longitude;
    float latitude;

    public TraverseMode getTraverseMode() {
        return traverseMode;
    }

    public void setTraverseMode(TraverseMode traverseMode) {
        this.traverseMode = traverseMode;
    }

    public int getMaxSpeedInMPerSec() {
        return maxSpeedInMPerSec;
    }

    public void setMaxSpeedInMPerSec(int maxSpeedInMPerSec) {
        this.maxSpeedInMPerSec = maxSpeedInMPerSec;
    }

    public int getRangeInM() {
        return rangeInM;
    }

    public void setRangeInM(int rangeInM) {
        this.rangeInM = rangeInM;
    }

    public boolean isAutomaticGear() {
        return automaticGear;
    }

    public void setAutomaticGear(boolean automaticGear) {
        this.automaticGear = automaticGear;
    }

    public boolean isRented() {
        return isRented;
    }

    public void setRented(boolean rented) {
        isRented = rented;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public VehicleDescription(TraverseMode traverseMode, float longitude, float latitude) {
        this.traverseMode = traverseMode;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
