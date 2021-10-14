/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import javax.validation.constraints.NotNull;

public final class Trip extends TransitEntity {

    private static final long serialVersionUID = 1L;

    private Route route;

    private Operator operator;

    private FeedScopedId serviceId;

    private String tripShortName;

    private String internalPlanningCode;

    private String tripHeadsign;

    private String routeShortName;

    @NotNull
    private Direction direction = Direction.UNKNOWN;

    private String blockId;

    private FeedScopedId shapeId;

    private int wheelchairAccessible = 0;

    /**
     * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
     */
    private BikeAccess bikesAllowed = BikeAccess.UNKNOWN;

    /** Custom extension for KCM to specify a fare per-trip */
    private String fareId;

    /**
     * Default alteration for a trip. // TODO Implement alterations for DSJ
     *
     * This is planned, by default (e.g. GTFS and if not set explicit).
     */
    private TripAlteration alteration = TripAlteration.PLANNED;

    public Trip(FeedScopedId id) {
        super(id);
    }

    public Trip(Trip obj) {
        this(obj.getId());
        this.route = obj.route;
        this.operator = obj.operator;
        this.serviceId = obj.serviceId;
        this.tripShortName = obj.tripShortName;
        this.tripHeadsign = obj.tripHeadsign;
        this.routeShortName = obj.routeShortName;
        this.direction = obj.direction;
        this.blockId = obj.blockId;
        this.shapeId = obj.shapeId;
        this.wheelchairAccessible = obj.wheelchairAccessible;
        this.bikesAllowed = obj.bikesAllowed;
        this.fareId = obj.fareId;
    }

    /**
     * Operator running the trip. Returns operator of this trip, if it exist, or else the route operator.
     */
    public Operator getOperator() {
        return operator != null ? operator : route.getOperator();
    }

    /**
     * This method return the operator associated with the trip. If the Trip have no Operator set {@code null} is
     * returned. Note! this method do not consider the {@link Route} that the trip is part of.
     * @see #getOperator()
     */
    public Operator getTripOperator() {
        return operator;
    }

    public void setTripOperator(Operator operator) {
        this.operator = operator;
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

    /**
     * Public code or identifier for the journey. Equal to NeTEx PublicCode. GTFS and NeTEx have
     * additional constraints on this fields that are not enforced in OTP.
     */
    public String getTripShortName() {
        return tripShortName;
    }

    public void setTripShortName(String tripShortName) {
        this.tripShortName = tripShortName;
    }

    /**
     * Return human friendly short info to identify the trip when mode, from/to stop and
     * times are known. This method is meant for logging, and should not be exposed in any API.
     */
    public String logInfo() {
        if(hasValue(tripShortName)) { return tripShortName; }
        if(hasValue(routeShortName)) { return routeShortName; }
        if(route != null && hasValue(route.getName())) { return route.getName(); }
        if(hasValue(tripHeadsign)) { return tripHeadsign; }
        return getId().getId();
    }

    /**
     * Internal code (non-public identifier) for the journey (e.g. train- or trip number from
     * the planners' tool). This is kept to ensure compatibility with legacy planning systems.
     * In NeTEx this maps to privateCode, there is no GTFS equivalent.
     */
    public String getInternalPlanningCode() { return internalPlanningCode; }

    public void setInternalPlanningCode(String internalPlanningCode) {
        this.internalPlanningCode = internalPlanningCode;
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

    // TODO Consider moving this to the TripPattern class once we have refactored the transit model
    /**
     * The direction for this Trip (and all other Trips in this TripPattern).
     */
    @NotNull
    public Direction getDirection() {
        return direction;
    }

    public String getGtfsDirectionIdAsString(String unknownValue) {
        return direction.equals(Direction.UNKNOWN)
            ? unknownValue
            : Integer.toString(direction.gtfsCode);
    }

    public void setDirection(Direction direction) {
        // Enforce non-null
        this.direction = direction != null ? direction : Direction.UNKNOWN;
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

    public BikeAccess getBikesAllowed() {
        return bikesAllowed;
    }

    public void setBikesAllowed(BikeAccess bikesAllowed) {
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

    public TripAlteration getTripAlteration() {
        return alteration;
    }

    public void setAlteration(TripAlteration tripAlteration) {
        if (tripAlteration != null) {
            this.alteration = tripAlteration;
        }
    }

    private boolean hasValue(String text) {
        return text != null && !text.isBlank();
    }
}
