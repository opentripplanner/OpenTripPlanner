/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Trip extends IdentityBean<FeedScopedId> {

    private static final long serialVersionUID = 1L;

    private FeedScopedId id;

    private Route route;

    private FeedScopedId serviceId;

    private String tripShortName;

    private String tripHeadsign;

    private String routeShortName;

    private String directionId;

    private String blockId;

    private FeedScopedId shapeId;

    private int wheelchairAccessible = 0;

    @Deprecated private int tripBikesAllowed = 0;

    /**
     * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    private int bikesAllowed = 0;

    /** Custom extension for KCM to specify a fare per-trip */
    private String fareId;

    private String drtMaxTravelTime;

    private String drtAvgTravelTime;

    private double drtAdvanceBookMin;

    private String drtPickupMessage;

    private String drtDropOffMessage;

    private String continuousPickupMessage;

    private String continuousDropOffMessage;

    public Trip() {
    }

    public Trip(Trip obj) {
        this.id = obj.id;
        this.route = obj.route;
        this.serviceId = obj.serviceId;
        this.tripShortName = obj.tripShortName;
        this.tripHeadsign = obj.tripHeadsign;
        this.routeShortName = obj.routeShortName;
        this.directionId = obj.directionId;
        this.blockId = obj.blockId;
        this.shapeId = obj.shapeId;
        this.wheelchairAccessible = obj.wheelchairAccessible;
        this.tripBikesAllowed = obj.tripBikesAllowed;
        this.bikesAllowed = obj.bikesAllowed;
        this.fareId = obj.fareId;
        this.drtMaxTravelTime = obj.drtMaxTravelTime;
        this.drtAvgTravelTime = obj.drtAvgTravelTime;
        this.drtAdvanceBookMin = obj.drtAdvanceBookMin;
        this.drtPickupMessage = obj.drtPickupMessage;
        this.drtDropOffMessage = obj.drtDropOffMessage;
        this.continuousPickupMessage = obj.continuousPickupMessage;
        this.continuousDropOffMessage = obj.continuousDropOffMessage;
    }

    public FeedScopedId getId() {
        return id;
    }

    public void setId(FeedScopedId id) {
        this.id = id;
    }

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
    }

    public FeedScopedId getServiceId() {
        return serviceId;
    }

    public void setServiceId(FeedScopedId serviceId) {
        this.serviceId = serviceId;
    }

    public String getTripShortName() {
        return tripShortName;
    }

    public void setTripShortName(String tripShortName) {
        this.tripShortName = tripShortName;
    }

    public String getTripHeadsign() {
        return tripHeadsign;
    }

    public void setTripHeadsign(String tripHeadsign) {
        this.tripHeadsign = tripHeadsign;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public String getDirectionId() {
        return directionId;
    }

    public void setDirectionId(String directionId) {
        this.directionId = directionId;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public FeedScopedId getShapeId() {
        return shapeId;
    }

    public void setShapeId(FeedScopedId shapeId) {
        this.shapeId = shapeId;
    }

    public void setWheelchairAccessible(int wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public int getWheelchairAccessible() {
        return wheelchairAccessible;
    }

    @Deprecated
    public void setTripBikesAllowed(int tripBikesAllowed) {
        this.tripBikesAllowed = tripBikesAllowed;
    }

    @Deprecated
    public int getTripBikesAllowed() {
        return tripBikesAllowed;
    }

    /**
     * @return 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public int getBikesAllowed() {
        return bikesAllowed;
    }

    /**
     * @param bikesAllowed 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    public void setBikesAllowed(int bikesAllowed) {
        this.bikesAllowed = bikesAllowed;
    }

    public String toString() {
        return "<Trip " + getId() + ">";
    }

    public String getFareId() {
        return fareId;
    }

    public void setFareId(String fareId) {
        this.fareId = fareId;
    }

    public String getDrtMaxTravelTime() {
        return drtMaxTravelTime;
    }

    public void setDrtMaxTravelTime(String drtMaxTravelTime) {
        this.drtMaxTravelTime = drtMaxTravelTime;
    }

    public String getDrtAvgTravelTime() {
        return drtAvgTravelTime;
    }

    public void setDrtAvgTravelTime(String drtAvgTravelTime) {
        this.drtAvgTravelTime = drtAvgTravelTime;
    }

    public double getDrtAdvanceBookMin() {
        return drtAdvanceBookMin;
    }

    public void setDrtAdvanceBookMin(double drtAdvanceBookMin) {
        this.drtAdvanceBookMin = drtAdvanceBookMin;
    }

    public String getDrtPickupMessage() {
        return drtPickupMessage;
    }

    public void setDrtPickupMessage(String drtPickupMessage) {
        this.drtPickupMessage = drtPickupMessage;
    }

    public String getDrtDropOffMessage() {
        return drtDropOffMessage;
    }

    public void setDrtDropOffMessage(String drtDropOffMessage) {
        this.drtDropOffMessage = drtDropOffMessage;
    }

    public String getContinuousPickupMessage() {
        return continuousPickupMessage;
    }

    public void setContinuousPickupMessage(String continuousPickupMessage) {
        this.continuousPickupMessage = continuousPickupMessage;
    }

    public String getContinuousDropOffMessage() {
        return continuousDropOffMessage;
    }

    public void setContinuousDropOffMessage(String continuousDropOffMessage) {
        this.continuousDropOffMessage = continuousDropOffMessage;
    }

}
