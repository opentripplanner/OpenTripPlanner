/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.opentripplanner.util.TimeToStringConverter;


/**
 * This class is TEMPORALLY used during mapping of GTFS and Netex into the internal Model,
 * it is not kept as part of the Graph.
 * <p/>
 * TODO OTP2 - Refactor the mapping so it do not create these objecs, but map directly into the target
 *           - object structure.
 */
public final class StopTime implements Comparable<StopTime> {

    private static final long serialVersionUID = 1L;

    public static final int MISSING_VALUE = -999;

    private Trip trip;

    private StopLocation stop;

    private int arrivalTime = MISSING_VALUE;

    private int departureTime = MISSING_VALUE;

    private int timepoint = MISSING_VALUE;

    private int stopSequence;

    private String stopHeadsign;

    private String routeShortName;

    private int pickupType;

    private int dropOffType;

    private double shapeDistTraveled = MISSING_VALUE;

    /** This is a Conveyal extension to the GTFS spec to support Seattle on/off peak fares. */
    private String farePeriodId;

    private int minArrivalTime = MISSING_VALUE;

    private int maxDepartureTime = MISSING_VALUE;

    private int continuousPickup;

    private int continuousDropOff;

    public StopTime() { }

    public StopTime(StopTime st) {
        this.trip = st.trip;
        this.stop = st.stop;
        this.arrivalTime = st.arrivalTime;
        this.departureTime = st.departureTime;
        this.timepoint = st.timepoint;
        this.stopSequence = st.stopSequence;
        this.stopHeadsign = st.stopHeadsign;
        this.routeShortName = st.routeShortName;
        this.pickupType = st.pickupType;
        this.dropOffType = st.dropOffType;
        this.shapeDistTraveled = st.shapeDistTraveled;
        this.farePeriodId = st.farePeriodId;
        this.minArrivalTime = st.minArrivalTime;
        this.maxDepartureTime  = st.maxDepartureTime;
        this.continuousPickup = st.continuousPickup;
        this.continuousDropOff = st.continuousDropOff;
    }

    /**
     * The id is used to navigate/link StopTime to other entities (Map from StopTime.id -> Entity.id).
     * There is no need to navigate in the opposite direction. The StopTime id is NOT stored in a
     * StopTime field.
     * <p/>
     * New ids should only be created when a travel search result is mapped to an itinerary, so even
     * if creating new objects are expensive, the few extra objects created during the mapping process
     * is ok.
     */
    public StopTimeKey getId() {
        return new StopTimeKey(trip.getId(), stopSequence);
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public int getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(int stopSequence) {
        this.stopSequence = stopSequence;
    }

    public StopLocation getStop() {
        return stop;
    }

    public void setStop(StopLocation stop) {
        this.stop = stop;
    }

    public boolean isArrivalTimeSet() {
        return arrivalTime != MISSING_VALUE;
    }

    /**
     * @return arrival time, in seconds since midnight
     */
    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public void clearArrivalTime() {
        this.arrivalTime = MISSING_VALUE;
    }

    public boolean isDepartureTimeSet() {
        return departureTime != MISSING_VALUE;
    }

    /**
     * @return departure time, in seconds since midnight
     */
    public int getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(int departureTime) {
        this.departureTime = departureTime;
    }

    public void clearDepartureTime() {
        this.departureTime = MISSING_VALUE;
    }

    public boolean isTimepointSet() {
        return timepoint != MISSING_VALUE;
    }

    /**
     * @return 1 if the stop-time is a timepoint location
     */
    public int getTimepoint() {
        return timepoint;
    }

    public void setTimepoint(int timepoint) {
        this.timepoint = timepoint;
    }

    public void clearTimepoint() {
        this.timepoint = MISSING_VALUE;
    }

    public String getStopHeadsign() {
        return stopHeadsign;
    }

    public void setStopHeadsign(String headSign) {
        this.stopHeadsign = headSign;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public void setRouteShortName(String routeShortName) {
        this.routeShortName = routeShortName;
    }

    public int getPickupType() {
        return pickupType;
    }

    public void setPickupType(int pickupType) {
        this.pickupType = pickupType;
    }

    public int getDropOffType() {
        return dropOffType;
    }

    public void setDropOffType(int dropOffType) {
        this.dropOffType = dropOffType;
    }

    public boolean isShapeDistTraveledSet() {
        return shapeDistTraveled != MISSING_VALUE;
    }

    public double getShapeDistTraveled() {
        return shapeDistTraveled;
    }

    public void setShapeDistTraveled(double shapeDistTraveled) {
        this.shapeDistTraveled = shapeDistTraveled;
    }

    public void clearShapeDistTraveled() {
        this.shapeDistTraveled = MISSING_VALUE;
    }

    public String getFarePeriodId() {
        return farePeriodId;
    }

    public void setFarePeriodId(String farePeriodId) {
        this.farePeriodId = farePeriodId;
    }

    public void setMinArrivalTime(int minArrivalTime) {
        this.minArrivalTime = minArrivalTime;
    }

    public int getMinArrivalTime() {
        return minArrivalTime;
    }

    public void setMaxDepartureTime(int maxDepartureTime) {
        this.maxDepartureTime = maxDepartureTime;
    }

    public int getMaxDepartureTime() {
        return maxDepartureTime;
    }

    public int getContinuousPickup() {
        return continuousPickup;
    }

    public void setContinuousPickup(int continuousPickup) {
        this.continuousPickup = continuousPickup;
    }

    public int getContinuousDropOff() {
        return continuousDropOff;
    }

    public void setContinuousDropOff(int continuousDropOff) {
        this.continuousDropOff = continuousDropOff;
    }

    public int compareTo(StopTime o) {
        return this.getStopSequence() - o.getStopSequence();
    }

    @Override
    public String toString() {
        return "StopTime(seq=" + getStopSequence() + " stop=" + getStop().getId() + " trip="
                + getTrip().getId() + " times=" + TimeToStringConverter.toHH_MM_SS(getArrivalTime())
                + "-" + TimeToStringConverter.toHH_MM_SS(getDepartureTime()) + ")";
    }
}
