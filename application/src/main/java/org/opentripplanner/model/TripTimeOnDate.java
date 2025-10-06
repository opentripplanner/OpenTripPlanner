package org.opentripplanner.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.StopTimeKey;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.lang.IntUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;
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
  private final int stopPosition;
  // This is only needed because TripTimes has no reference to TripPattern
  private final TripPattern tripPattern;

  @Nullable
  private final LocalDate serviceDate;

  private final long midnight;

  public TripTimeOnDate(TripTimes tripTimes, int stopPosition, TripPattern tripPattern) {
    this.tripTimes = tripTimes;
    this.stopPosition = stopPosition;
    this.tripPattern = tripPattern;
    this.serviceDate = null;
    this.midnight = UNDEFINED;
  }

  public TripTimeOnDate(
    TripTimes tripTimes,
    int stopPosition,
    TripPattern tripPattern,
    @Nullable LocalDate serviceDate,
    @Nullable Instant midnight
  ) {
    this.tripTimes = tripTimes;
    this.stopPosition = stopPosition;
    this.tripPattern = tripPattern;
    this.serviceDate = serviceDate;
    this.midnight = midnight != null ? midnight.getEpochSecond() : UNDEFINED;
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   *
   * @return null if the trip does not exist in the timetable
   */
  @Nullable
  public static List<TripTimeOnDate> fromTripTimes(Timetable table, Trip trip) {
    TripTimes times = table.getTripTimes(trip);
    if (times == null) {
      return null;
    }
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern()));
    }
    return out;
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   * <br>
   * If the timetable does not contain the trip, scheduledTimetable is used instead.
   *
   * @param table the timetable for the service day
   * @param serviceDate service day to set
   */
  public static List<TripTimeOnDate> fromTripTimesWithScheduleFallback(
    Timetable table,
    Trip trip,
    LocalDate serviceDate,
    Instant midnight
  ) {
    TripTimes times = table.getTripTimes(trip);
    if (times == null) {
      Timetable scheduledTimetable = table.getPattern().getScheduledTimetable();
      return fromTripTimes(scheduledTimetable, trip);
    }
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern(), serviceDate, midnight));
    }
    return out;
  }

  /**
   * Must pass in both Timetable and Trip, because TripTimes do not have a reference to
   * StopPatterns.
   * <br>
   * The timetable given must correspond to the service day so that it must contain the trip.
   *
   * @param table the timetable for the service day
   * @param serviceDate service day to set
   */
  public static List<TripTimeOnDate> fromTripTimes(
    Timetable table,
    Trip trip,
    LocalDate serviceDate,
    Instant midnight
  ) {
    // The timetable given should always contain the trip.
    // if the trip doesn't run on the date, the scheduled timetable should be given.
    TripTimes times = Objects.requireNonNull(table.getTripTimes(trip));
    List<TripTimeOnDate> out = new ArrayList<>();
    for (int i = 0; i < times.getNumStops(); ++i) {
      out.add(new TripTimeOnDate(times, i, table.getPattern(), serviceDate, midnight));
    }
    return out;
  }

  /**
   * Get first stop TripTimeOnDate from Timetable.
   */
  public static TripTimeOnDate firstFromTripTimes(
    Timetable table,
    Trip trip,
    LocalDate serviceDate,
    Instant midnight
  ) {
    TripTimes times = table.getTripTimes(trip);
    return new TripTimeOnDate(times, 0, table.getPattern(), serviceDate, midnight);
  }

  /**
   * Get last stop TripTimeOnDate from Timetable.
   */
  public static TripTimeOnDate lastFromTripTimes(
    Timetable table,
    Trip trip,
    LocalDate serviceDate,
    Instant midnight
  ) {
    TripTimes times = table.getTripTimes(trip);
    return new TripTimeOnDate(
      times,
      times.getNumStops() - 1,
      table.getPattern(),
      serviceDate,
      midnight
    );
  }

  /**
   * A comparator that uses real time departure if it is available, otherwise the scheduled departure.
   */
  public static Comparator<TripTimeOnDate> compareByDeparture() {
    return Comparator.comparing(t -> t.getServiceDayMidnight() + t.getRealtimeDeparture());
  }

  /**
   * A comparator that uses the scheduled departure time only.
   */
  public static Comparator<TripTimeOnDate> compareByScheduledDeparture() {
    return Comparator.comparing(t -> t.getServiceDayMidnight() + t.getScheduledDeparture());
  }

  public StopLocation getStop() {
    return tripPattern.getStop(stopPosition);
  }

  public int getStopPosition() {
    return stopPosition;
  }

  public TripTimes getTripTimes() {
    return tripTimes;
  }

  /**
   * Returns true if there is either a scheduled arrival or a scheduled departure time.
   * If neither is the case, this indicates that it's a flexible time window instead.
   */
  public boolean hasScheduledTimes() {
    return (
      tripTimes.getScheduledArrivalTime(stopPosition) != StopTime.MISSING_VALUE &&
      tripTimes.getScheduledDepartureTime(stopPosition) != StopTime.MISSING_VALUE
    );
  }

  public int getScheduledArrival() {
    return tripTimes.getScheduledArrivalTime(stopPosition);
  }

  /**
   * Returns the scheduled arrival as an Instant.
   */
  public Instant scheduledArrival() {
    return toInstant(getScheduledArrival());
  }

  /**
   * @return The GTFS stop sequence of the stop time.
   */
  public int getGtfsSequence() {
    return tripTimes.gtfsSequenceOfStopIndex(stopPosition);
  }

  public int getScheduledDeparture() {
    return tripTimes.getScheduledDepartureTime(stopPosition);
  }

  /**
   * Returns the scheduled departure as an Instant.
   */
  public Instant scheduledDeparture() {
    return toInstant(getScheduledDeparture());
  }

  public int getRealtimeArrival() {
    return isCancelledStop() || isNoDataStop()
      ? tripTimes.getScheduledArrivalTime(stopPosition)
      : tripTimes.getArrivalTime(stopPosition);
  }

  public int getRealtimeDeparture() {
    return isCancelledStop() || isNoDataStop()
      ? tripTimes.getScheduledDepartureTime(stopPosition)
      : tripTimes.getDepartureTime(stopPosition);
  }

  /**
   * Returns the actual arrival time if available. Otherwise -1 is returned.
   */
  public int getActualArrival() {
    return isRecordedStop() ? tripTimes.getArrivalTime(stopPosition) : UNDEFINED;
  }

  /**
   * Returns the actual departure time if available. Otherwise -1 is returned.
   */
  public int getActualDeparture() {
    return isRecordedStop() ? tripTimes.getDepartureTime(stopPosition) : UNDEFINED;
  }

  public int getArrivalDelay() {
    return isCancelledStop() || isNoDataStop() ? 0 : tripTimes.getArrivalDelay(stopPosition);
  }

  public int getDepartureDelay() {
    return isCancelledStop() || isNoDataStop() ? 0 : tripTimes.getDepartureDelay(stopPosition);
  }

  public boolean isTimepoint() {
    return tripTimes.isTimepoint(stopPosition);
  }

  public boolean isRealtime() {
    return !tripTimes.isScheduled() && !isNoDataStop();
  }

  public boolean isCancelledStop() {
    return (
      tripTimes.isCancelledStop(stopPosition) ||
      tripPattern.isBoardAndAlightAt(stopPosition, PickDrop.CANCELLED)
    );
  }

  public boolean isPredictionInaccurate() {
    return tripTimes.isPredictionInaccurate(stopPosition);
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
    return tripTimes.isNoDataStop(stopPosition);
  }

  /**
   * Is the real-time time a recorded time (i.e. has the vehicle already passed the stop).
   * This information is currently only available from SIRI feeds.
   */
  public boolean isRecordedStop() {
    return tripTimes.isRecordedStop(stopPosition);
  }

  public RealTimeState getRealTimeState() {
    return tripTimes.isNoDataStop(stopPosition)
      ? RealTimeState.SCHEDULED
      : tripTimes.getRealTimeState();
  }

  public OccupancyStatus getOccupancyStatus() {
    return tripTimes.getOccupancyStatus(stopPosition);
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

  public I18NString getHeadsign() {
    return tripTimes.getHeadsign(stopPosition);
  }

  /** @return a list of via names visible at this stop, or an empty list if there are no vias. */
  public List<String> getHeadsignVias() {
    return tripTimes.getHeadsignVias(stopPosition);
  }

  public PickDrop getPickupType() {
    if (tripTimes.isDeleted()) {
      LOG.warn(
        "Returning pickup type for a deleted trip {} on pattern {} on date {}. This indicates a bug.",
        tripTimes.getTrip().getId(),
        tripPattern.getId(),
        serviceDate
      );

      return tripPattern.getBoardType(stopPosition);
    }

    return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopPosition)
      ? PickDrop.CANCELLED
      : tripPattern.getBoardType(stopPosition);
  }

  public PickDrop getDropoffType() {
    if (tripTimes.isDeleted()) {
      LOG.warn(
        "Returning dropoff type for a deleted trip {} on pattern {} on date {}. This indicates a bug.",
        tripTimes.getTrip().getId(),
        tripPattern.getId(),
        serviceDate
      );

      return tripPattern.getAlightType(stopPosition);
    }

    return tripTimes.isCanceled() || tripTimes.isCancelledStop(stopPosition)
      ? PickDrop.CANCELLED
      : tripPattern.getAlightType(stopPosition);
  }

  public StopTimeKey getStopTimeKey() {
    return StopTimeKey.of(tripTimes.getTrip().getId(), stopPosition).build();
  }

  public BookingInfo getPickupBookingInfo() {
    return tripTimes.getPickupBookingInfo(stopPosition);
  }

  public BookingInfo getDropOffBookingInfo() {
    return tripTimes.getDropOffBookingInfo(stopPosition);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    TripTimeOnDate that = (TripTimeOnDate) o;
    return (
      stopPosition == that.stopPosition &&
      midnight == that.midnight &&
      Objects.equals(tripTimes, that.tripTimes) &&
      Objects.equals(tripPattern, that.tripPattern) &&
      Objects.equals(serviceDate, that.serviceDate)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(tripTimes, stopPosition, tripPattern, serviceDate, midnight);
  }

  public TripPattern pattern() {
    return tripPattern;
  }

  /**
   * Returns the previous stop times in the trip. If it's the first stop in the trip, it returns an
   * empty list.
   */
  public List<TripTimeOnDate> previousTimes() {
    if (stopPosition == 0) {
      return List.of();
    } else {
      // IntStream.range is (inclusive, exclusive)
      return IntStream.range(0, stopPosition).mapToObj(this::atStopPosition).toList();
    }
  }

  /**
   * Returns the next stop times in the trip. If it's the stop in the trip it returns an empty list.
   */
  public List<TripTimeOnDate> nextTimes() {
    if (stopPosition == tripTimes.getNumStops() - 1) {
      return List.of();
    } else {
      // IntStream.range is (inclusive, exclusive)
      return IntStream.range(stopPosition + 1, tripTimes.getNumStops())
        .mapToObj(this::atStopPosition)
        .toList();
    }
  }

  /**
   * Returns the real-time arrival, if available.
   */
  public Optional<Instant> realtimeArrival() {
    return optionalInstant(getRealtimeArrival());
  }

  /**
   * Returns the real-time departure, if available.
   */
  public Optional<Instant> realtimeDeparture() {
    return optionalInstant(getRealtimeDeparture());
  }

  private TripTimeOnDate atStopPosition(int stopPosition) {
    IntUtils.requireInRange(stopPosition, 0, tripTimes.getNumStops() - 1, "stopPosition");
    return new TripTimeOnDate(
      tripTimes,
      stopPosition,
      tripPattern,
      serviceDate,
      Instant.ofEpochSecond(midnight)
    );
  }

  /**
   * If real time data is available for this stop (call) then it is returned or an empty Optional
   * otherwise.
   */
  private Optional<Instant> optionalInstant(int secondsSinceMidnight) {
    if (isCancelledStop() || isRealtime()) {
      return Optional.of(toInstant(secondsSinceMidnight));
    } else {
      return Optional.empty();
    }
  }

  private Instant toInstant(int secondsSinceMidnight) {
    return Instant.ofEpochSecond(midnight).plusSeconds(secondsSinceMidnight);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripTimeOnDate.class)
      .addObj("trip", tripTimes.getTrip())
      .addNum("stopPosition", stopPosition)
      .addServiceTime("arrival", getScheduledArrival())
      .addServiceTime("departure", getScheduledArrival())
      .addDate("serviceDate", serviceDate)
      .toString();
  }
}
