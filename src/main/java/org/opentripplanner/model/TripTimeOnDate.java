package org.opentripplanner.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.StopTimeKey;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a Trip at a specific stop index and on a specific service day. This is a read-only
 * data transfer object used to pass information from the OTP internal model to the APIs.
 */
public class TripTimeOnDate {

  private static final Logger LOG = LoggerFactory.getLogger(TripTimeOnDate.class);

  public static final int UNDEFINED = -1;

  private final TripTimes tripTimes;
  private final int stopIndex;
  // This is only needed because TripTimes has no reference to TripPattern
  private final TripPattern tripPattern;
  private final LocalDate serviceDate;
  private final long midnight;

  public TripTimeOnDate(TripTimes tripTimes, int stopIndex, TripPattern tripPattern) {
    this.tripTimes = tripTimes;
    this.stopIndex = stopIndex;
    this.tripPattern = tripPattern;
    this.serviceDate = null;
    this.midnight = UNDEFINED;
  }

  public TripTimeOnDate(
    TripTimes tripTimes,
    int stopIndex,
    TripPattern tripPattern,
    LocalDate serviceDate,
    Instant midnight
  ) {
    this.tripTimes = tripTimes;
    this.stopIndex = stopIndex;
    this.tripPattern = tripPattern;
    this.serviceDate = serviceDate;
    this.midnight = midnight != null ? midnight.getEpochSecond() : UNDEFINED;
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   */
  public static List<TripTimeOnDate> fromTripTimes(Timetable table, Trip trip) {
    TripTimes times = table.getTripTimes(trip);
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern()));
    }
    return out;
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   *
   * @param serviceDate service day to set, if null none is set
   */
  public static List<TripTimeOnDate> fromTripTimes(
    Timetable table,
    Trip trip,
    LocalDate serviceDate,
    Instant midnight
  ) {
    TripTimes times = table.getTripTimes(trip);
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern(), serviceDate, midnight));
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

  /**
   * @return The GTFS stop sequence of the stop time.
   */
  public int getGtfsSequence() {
    return tripTimes.gtfsSequenceOfStopIndex(stopIndex);
  }

  public int getScheduledDeparture() {
    return tripTimes.getScheduledDepartureTime(stopIndex);
  }

  public int getRealtimeArrival() {
    return isCancelledStop() || isNoDataStop()
      ? tripTimes.getScheduledArrivalTime(stopIndex)
      : tripTimes.getArrivalTime(stopIndex);
  }

  public int getRealtimeDeparture() {
    return isCancelledStop() || isNoDataStop()
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
    return isCancelledStop() || isNoDataStop() ? 0 : tripTimes.getArrivalDelay(stopIndex);
  }

  public int getDepartureDelay() {
    return isCancelledStop() || isNoDataStop() ? 0 : tripTimes.getDepartureDelay(stopIndex);
  }

  public boolean isTimepoint() {
    return tripTimes.isTimepoint(stopIndex);
  }

  public boolean isRealtime() {
    return !tripTimes.isScheduled() && !isNoDataStop();
  }

  public boolean isCancelledStop() {
    return (
      tripTimes.isCancelledStop(stopIndex) ||
      tripPattern.isBoardAndAlightAt(stopIndex, PickDrop.CANCELLED)
    );
  }

  public boolean isPredictionInaccurate() {
    return tripTimes.isPredictionInaccurate(stopIndex);
  }

  /** Return {code true} if stop is cancelled, or trip is canceled/replaced */
  public boolean isCanceledEffectively() {
    return (
      isCancelledStop() ||
      tripTimes.isCanceledOrDeleted() ||
      tripTimes.getTrip().getNetexAlteration().isCanceledOrReplaced()
    );
  }

  public boolean isNoDataStop() {
    return tripTimes.isNoDataStop(stopIndex);
  }

  public RealTimeState getRealTimeState() {
    return tripTimes.isNoDataStop(stopIndex)
      ? RealTimeState.SCHEDULED
      : tripTimes.getRealTimeState();
  }

  public OccupancyStatus getOccupancyStatus() {
    return tripTimes.getOccupancyStatus(stopIndex);
  }

  public long getServiceDayMidnight() {
    return midnight;
  }

  public LocalDate getServiceDay() {
    return serviceDate;
  }

  public Trip getTrip() {
    return tripTimes.getTrip();
  }

  public String getBlockId() {
    return tripTimes.getTrip().getGtfsBlockId();
  }

  public I18NString getHeadsign() {
    return tripTimes.getHeadsign(stopIndex);
  }

  public List<String> getHeadsignVias() {
    return tripTimes.getHeadsignVias(stopIndex);
  }

  public PickDrop getPickupType() {
    if (tripTimes.isDeleted()) {
      LOG.warn(
        "Returning pickup type for a deleted trip {} on pattern {} on date {}. This indicates a bug.",
        tripTimes.getTrip().getId(),
        tripPattern.getId(),
        serviceDate
      );

      return tripPattern.getBoardType(stopIndex);
    }

    return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopIndex)
      ? PickDrop.CANCELLED
      : tripPattern.getBoardType(stopIndex);
  }

  public PickDrop getDropoffType() {
    if (tripTimes.isDeleted()) {
      LOG.warn(
        "Returning dropoff type for a deleted trip {} on pattern {} on date {}. This indicates a bug.",
        tripTimes.getTrip().getId(),
        tripPattern.getId(),
        serviceDate
      );

      return tripPattern.getAlightType(stopIndex);
    }

    return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopIndex)
      ? PickDrop.CANCELLED
      : tripPattern.getAlightType(stopIndex);
  }

  public StopTimeKey getStopTimeKey() {
    return StopTimeKey.of(tripTimes.getTrip().getId(), stopIndex).build();
  }

  public BookingInfo getPickupBookingInfo() {
    return tripTimes.getPickupBookingInfo(stopIndex);
  }

  public BookingInfo getDropOffBookingInfo() {
    return tripTimes.getDropOffBookingInfo(stopIndex);
  }
}
