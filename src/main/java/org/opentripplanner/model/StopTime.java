/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import org.opentripplanner.util.TimeToStringConverter;

import java.io.Serializable;
import java.util.Objects;

public final class StopTime extends TransitEntity<StopTimeId> implements Serializable, Comparable<StopTime> {

    private static final long serialVersionUID = 1L;

    public static final int MISSING_VALUE = -999;

    private Trip trip;

    private Stop stop;

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
    }

    /**
     * The id is used to navigate/link StopTime to other entities (Map from StopTime.id -> Entity.id).
     * There is no need to navigate in the opposite direction. The StopTime id is NOT stored in a
     * StopTime field. This save memory, since there is a lot of StopTimes - and very few references
     * to StopTimes.
     * <p/>
     * New ids should only be created when a travel search result is mapped to an itinerary, so even
     * if creating new objects are expensive, the few extra objects created during the mapping process
     * is ok.
     * <p/>
     * THE *ID* SHOULD NOT BE USED DURING THE ROUTING SEARCH.
     */
    @Override
    public StopTimeId getId() {
        return new StopTimeId(trip.getId(), stopSequence);
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

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
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

    public int compareTo(StopTime o) {
        return this.getStopSequence() - o.getStopSequence();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StopTime stopTime = (StopTime) o;
        return stopSequence == stopTime.stopSequence && trip.equals(stopTime.trip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trip, stopSequence);
    }

    @Override
    public String toString() {
        return "StopTime(seq=" + getStopSequence() + " stop=" + getStop().getId() + " trip="
                + getTrip().getId() + " times=" + TimeToStringConverter.toHH_MM_SS(getArrivalTime())
                + "-" + TimeToStringConverter.toHH_MM_SS(getDepartureTime()) + ")";
    }
}
