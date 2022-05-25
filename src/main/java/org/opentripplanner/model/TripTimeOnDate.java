package org.opentripplanner.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * Represents a Trip at a specific stop index and on a specific service day. This is a read-only
 * data transfer object used to pass information from the OTP internal model to the APIs.
 */
public class TripTimeOnDate {

  public static final int UNDEFINED = -1;

  private final TripTimes tripTimes;
  private final int stopIndex;
  // This is only needed because TripTimes has no reference to TripPattern
  private final TripPattern tripPattern;
  private final ServiceDate serviceDate;
  private final long midnight;

  public TripTimeOnDate(
    TripTimes tripTimes,
    int stopIndex,
    TripPattern tripPattern,
    ServiceDay serviceDay
  ) {
    this.tripTimes = tripTimes;
    this.stopIndex = stopIndex;
    this.tripPattern = tripPattern;
    this.serviceDate = serviceDay != null ? serviceDay.getServiceDate() : null;
    this.midnight = serviceDay != null ? serviceDay.time(0) : UNDEFINED;
  }

  public TripTimeOnDate(
    TripTimes tripTimes,
    int stopIndex,
    TripPattern tripPattern,
    ServiceDate serviceDate,
    Instant midnight
  ) {
    this.tripTimes = tripTimes;
    this.stopIndex = stopIndex;
    this.tripPattern = tripPattern;
    this.serviceDate = serviceDate;
    this.midnight = midnight.getEpochSecond();
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   */
  public static List<TripTimeOnDate> fromTripTimes(Timetable table, Trip trip) {
    TripTimes times = table.getTripTimes(trip);
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern(), (ServiceDay) null));
    }
    return out;
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   *
   * @param serviceDay service day to set, if null none is set
   */
  public static List<TripTimeOnDate> fromTripTimes(
    Timetable table,
    Trip trip,
    ServiceDay serviceDay
  ) {
    TripTimes times = table.getTripTimes(trip);
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern(), serviceDay));
    }
    return out;
  }

  public static Comparator<TripTimeOnDate> compareByDeparture() {
    return Comparator.comparing(t -> t.getServiceDayMidnight() + t.getRealtimeDeparture());
  }

  public StopLocation getStop() {
    return tripPattern.getStop(stopIndex);
  }

  public int getStopIndex() {
    return stopIndex;
  }

  public TripTimes getTripTimes() {
    return tripTimes;
  }

  public int getStopCount() {
    return tripTimes.getNumStops();
  }

  public int getScheduledArrival() {
    return tripTimes.getScheduledArrivalTime(stopIndex);
  }

  public int getScheduledDeparture() {
    return tripTimes.getScheduledDepartureTime(stopIndex);
  }

  public int getRealtimeArrival() {
    return isRealtime() && isCancelledStop()
      ? tripTimes.getScheduledArrivalTime(stopIndex)
      : tripTimes.getArrivalTime(stopIndex);
  }

  public int getRealtimeDeparture() {
    return isRealtime() && isCancelledStop()
      ? tripTimes.getScheduledDepartureTime(stopIndex)
      : tripTimes.getDepartureTime(stopIndex);
  }

  /**
   * Returns the actual arrival time if available. Otherwise -1 is returned.
   */
  public int getActualArrival() {
    return tripTimes.isRecordedStop(stopIndex) ? tripTimes.getArrivalTime(stopIndex) : UNDEFINED;
  }

  /**
   * Returns the actual departure time if available. Otherwise -1 is returned.
   */
  public int getActualDeparture() {
    return tripTimes.isRecordedStop(stopIndex) ? tripTimes.getDepartureTime(stopIndex) : UNDEFINED;
  }

  public int getArrivalDelay() {
    return tripTimes.getArrivalDelay(stopIndex);
  }

  public int getDepartureDelay() {
    return tripTimes.getDepartureDelay(stopIndex);
  }

  public boolean isTimepoint() {
    return tripTimes.isTimepoint(stopIndex);
  }

  public boolean isRealtime() {
    return !tripTimes.isScheduled() && !tripTimes.isNoDataStop(stopIndex);
  }

  public boolean isCancelledStop() {
    return (
      tripTimes.isCancelledStop(stopIndex) ||
      tripPattern.isBoardAndAlightAt(stopIndex, PickDrop.CANCELLED)
    );
  }

  /** Return {code true} if stop is cancelled, or trip is canceled/replaced */
  public boolean isCanceledEffectively() {
    return (
      isCancelledStop() ||
      tripTimes.isCanceled() ||
      tripTimes.getTrip().getTripAlteration().isCanceledOrReplaced()
    );
  }

  public RealTimeState getRealtimeState() {
    return tripTimes.isNoDataStop(stopIndex)
      ? RealTimeState.SCHEDULED
      : tripTimes.getRealTimeState();
  }

  public long getServiceDayMidnight() {
    return midnight;
  }

  public ServiceDate getServiceDay() {
    return serviceDate;
  }

  public Trip getTrip() {
    return tripTimes.getTrip();
  }

  public String getBlockId() {
    return tripTimes.getTrip().getBlockId();
  }

  public String getHeadsign() {
    return tripTimes.getHeadsign(stopIndex);
  }

  public List<String> getHeadsignVias() {
    return tripTimes.getHeadsignVias(stopIndex);
  }

  public PickDrop getPickupType() {
    return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopIndex)
      ? PickDrop.CANCELLED
      : tripPattern.getBoardType(stopIndex);
  }

  public PickDrop getDropoffType() {
    return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopIndex)
      ? PickDrop.CANCELLED
      : tripPattern.getAlightType(stopIndex);
  }

  public StopTimeKey getStopTimeKey() {
    return new StopTimeKey(tripTimes.getTrip().getId(), stopIndex);
  }

  public BookingInfo getPickupBookingInfo() {
    return tripTimes.getPickupBookingInfo(stopIndex);
  }

  public BookingInfo getDropOffBookingInfo() {
    return tripTimes.getDropOffBookingInfo(stopIndex);
  }
}
