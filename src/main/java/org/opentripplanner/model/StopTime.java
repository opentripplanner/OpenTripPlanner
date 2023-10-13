/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

import java.util.List;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.StopTimeKey;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * This class is TEMPORALLY used during mapping of GTFS and Netex into the internal Model, it is not
 * kept as part of the Graph.
 * <p/>
 * TODO OTP2 - Refactor the mapping so it do not create these objecs, but map directly into the target
 *           - object structure.
 */
public final class StopTime implements Comparable<StopTime> {

  public static final int MISSING_VALUE = -999;

  private Trip trip;

  private StopLocation stop;

  private int arrivalTime = MISSING_VALUE;

  private int departureTime = MISSING_VALUE;

  private int timepoint = MISSING_VALUE;

  private int stopSequence;

  private I18NString stopHeadsign;

  private List<String> headsignVias;

  private String routeShortName;

  private PickDrop pickupType = PickDrop.SCHEDULED;

  private PickDrop dropOffType = PickDrop.SCHEDULED;

  private double shapeDistTraveled = MISSING_VALUE;

  /** This is a Conveyal extension to the GTFS spec to support Seattle on/off peak fares. */
  private String farePeriodId;

  private int flexWindowStart = MISSING_VALUE;

  private int flexWindowEnd = MISSING_VALUE;

  // Disabled by default
  private PickDrop flexContinuousPickup = PickDrop.NONE;

  // Disabled by default
  private PickDrop flexContinuousDropOff = PickDrop.NONE;

  private BookingInfo dropOffBookingInfo;

  private BookingInfo pickupBookingInfo;

  public StopTime() {}

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
    this.flexWindowStart = st.flexWindowStart;
    this.flexWindowEnd = st.flexWindowEnd;
    this.flexContinuousPickup = st.flexContinuousPickup;
    this.flexContinuousDropOff = st.flexContinuousDropOff;
    this.dropOffBookingInfo = st.dropOffBookingInfo;
    this.pickupBookingInfo = st.pickupBookingInfo;
    this.headsignVias = st.headsignVias;
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
    return StopTimeKey.of(trip.getId(), stopSequence).build();
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

  public I18NString getStopHeadsign() {
    return stopHeadsign;
  }

  public void setStopHeadsign(I18NString headSign) {
    this.stopHeadsign = headSign;
  }

  public String getRouteShortName() {
    return routeShortName;
  }

  public void setRouteShortName(String routeShortName) {
    this.routeShortName = routeShortName;
  }

  public PickDrop getPickupType() {
    return pickupType;
  }

  public void setPickupType(PickDrop pickupType) {
    this.pickupType = pickupType;
  }

  public PickDrop getDropOffType() {
    return dropOffType;
  }

  public void setDropOffType(PickDrop dropOffType) {
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

  public String getFarePeriodId() {
    return farePeriodId;
  }

  public void setFarePeriodId(String farePeriodId) {
    this.farePeriodId = farePeriodId;
  }

  public int getFlexWindowStart() {
    return flexWindowStart;
  }

  public void setFlexWindowStart(int flexWindowStart) {
    this.flexWindowStart = flexWindowStart;
  }

  public int getFlexWindowEnd() {
    return flexWindowEnd;
  }

  public void setFlexWindowEnd(int flexWindowEnd) {
    this.flexWindowEnd = flexWindowEnd;
  }

  /** Get either the start of the flex window or the departure time, whichever is set */
  public int getEarliestPossibleDepartureTime() {
    return getAvailableTime(getFlexWindowStart(), getDepartureTime());
  }

  /** Get either the end of the flex window or the arrival time, whichever is set */
  public int getLatestPossibleArrivalTime() {
    return getAvailableTime(getFlexWindowEnd(), getArrivalTime());
  }

  public PickDrop getFlexContinuousPickup() {
    return flexContinuousPickup;
  }

  public void setFlexContinuousPickup(PickDrop flexContinuousPickup) {
    this.flexContinuousPickup = flexContinuousPickup;
  }

  public PickDrop getFlexContinuousDropOff() {
    return flexContinuousDropOff;
  }

  public void setFlexContinuousDropOff(PickDrop flexContinuousDropOff) {
    this.flexContinuousDropOff = flexContinuousDropOff;
  }

  public BookingInfo getDropOffBookingInfo() {
    return dropOffBookingInfo;
  }

  public void setDropOffBookingInfo(BookingInfo dropOffBookingInfo) {
    this.dropOffBookingInfo = dropOffBookingInfo;
  }

  public BookingInfo getPickupBookingInfo() {
    return pickupBookingInfo;
  }

  public void setPickupBookingInfo(BookingInfo pickupBookingInfo) {
    this.pickupBookingInfo = pickupBookingInfo;
  }

  public List<String> getHeadsignVias() {
    return headsignVias;
  }

  public void setHeadsignVias(List<String> headsignVias) {
    this.headsignVias = headsignVias;
  }

  public int compareTo(StopTime o) {
    return this.getStopSequence() - o.getStopSequence();
  }

  public void cancel() {
    pickupType = PickDrop.CANCELLED;
    dropOffType = PickDrop.CANCELLED;
  }

  @Override
  public String toString() {
    return (
      "StopTime(seq=" +
      getStopSequence() +
      " stop=" +
      getStop().getId() +
      " trip=" +
      getTrip().getId() +
      " times=" +
      TimeUtils.timeToStrLong(getArrivalTime()) +
      "-" +
      TimeUtils.timeToStrLong(getDepartureTime()) +
      ")"
    );
  }

  private static int getAvailableTime(int... times) {
    for (var time : times) {
      if (time != MISSING_VALUE) {
        return time;
      }
    }

    return MISSING_VALUE;
  }
}
