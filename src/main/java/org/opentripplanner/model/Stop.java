/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Stop extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private static final int MISSING_VALUE = -999;

    private FeedScopedId id;

    private String name;

    private double lat;

    private double lon;

    private String code;

    private String desc;

    private String zoneId;

    private String url;

    private int locationType = 0;

    private String parentStation;

    private int wheelchairBoarding = 0;

    private String direction;

    private String timezone;

    private int vehicleType = MISSING_VALUE;

    private String platformCode;

    public Stop() {

    }

    public Stop(Stop obj) {
        this.id = obj.id;
        this.code = obj.code;
        this.name = obj.name;
        this.desc = obj.desc;
        this.lat = obj.lat;
        this.lon = obj.lon;
        this.zoneId = obj.zoneId;
        this.url = obj.url;
        this.locationType = obj.locationType;
        this.parentStation = obj.parentStation;
        this.wheelchairBoarding = obj.wheelchairBoarding;
        this.direction = obj.direction;
        this.timezone = obj.timezone;
        this.vehicleType = obj.vehicleType;
        this.platformCode = obj.platformCode;
    }

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getLocationType() {
        return locationType;
    }

    public void setLocationType(int locationType) {
        this.locationType = locationType;
    }

    public String getParentStation() {
        return parentStation;
    }

    public void setParentStation(String parentStation) {
        this.parentStation = parentStation;
    }

    @Override
    public String toString() {
        return "<Stop " + this.id + ">";
    }

    public void setWheelchairBoarding(int wheelchairBoarding) {
        this.wheelchairBoarding = wheelchairBoarding;
    }

    public int getWheelchairBoarding() {
        return wheelchairBoarding;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isVehicleTypeSet() {
        return vehicleType != MISSING_VALUE;
    }

    public int getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(int vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void clearVehicleType() {
        vehicleType = MISSING_VALUE;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }
}
