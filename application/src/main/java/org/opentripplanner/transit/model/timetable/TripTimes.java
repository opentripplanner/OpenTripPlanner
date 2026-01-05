package org.opentripplanner.transit.model.timetable;

import java.io.Serializable;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * A TripTimes represents the arrival and departure times for a single trip in a timetable. It is
 * one of the core class used for transit routing. This interface allows different kind of trip
 * to implement their own trip times. Scheduled/planned trips should be immutable, real-time
 * trip times should allow updates and more info, frequency-based trips can use a more compact
 * implementation, and Flex may expose part of the trip as a "scheduled/regular" stop-to-stop
 * trip using this interface. All times are expressed as seconds since midnight (as in
 * GTFS). Unless stated otherwise, accessor methods which take an integer stop parameter refer to
 * the position within the trip's TripPattern, not its GTFS stop sequence for example or Raptor
 * stop index. The stop position is 0(zero) based.
 */
public interface TripTimes<T extends TripTimes> extends Serializable, Comparable<TripTimes> {
  /**
   * Create a RealTimeTripTimesBuilder using the information, but not the times, from this
   * TripTimes.
   */
  RealTimeTripTimesBuilder createRealTimeWithoutScheduledTimes();

  /**
   * Create a RealTimeTripTimesBuilder using the information from this TripTimes, with the actual
   * times pre-filled from scheduled times.
   */
  RealTimeTripTimesBuilder createRealTimeFromScheduledTimes();

  /** The code for the service on which this trip runs. For departure search optimizations. */
  int getServiceCode();

  /** Make a copy of the TripTimes with the new service code, for use while adding trips to Timetable */
  T withServiceCode(int serviceCode);

  /**
   * The time in seconds after midnight at which the vehicle should arrive at the given stop
   * position according to the original schedule.
   */
  int getScheduledArrivalTime(int stopPos);

  /**
   * The time in seconds after midnight at which the vehicle arrives at each stop position,
   * accounting for any real-time updates.
   */
  int getArrivalTime(int stopPos);

  /**
   * @return the difference between the scheduled and actual arrival times at this stop position.
   */
  int getArrivalDelay(int stopPos);

  /**
   * The time in seconds after midnight at which the vehicle should leave the given stop position
   * according to the original schedule.
   */
  int getScheduledDepartureTime(int stopPos);

  /**
   * The time in seconds after midnight at which the vehicle leaves each stop position, accounting
   * for any real-time updates.
   */
  int getDepartureTime(int stopPos);

  /**
   * @return the difference between the scheduled and actual departure times at this stop position.
   */
  int getDepartureDelay(int stopPos);

  /**
   * Whether stop position is considered a GTFS timepoint.
   */
  boolean isTimepoint(int stopPos);

  /** The trips whose arrivals and departures are represented by this class */
  Trip getTrip();

  /**
   * Return an integer which can be used to sort TripTimes in order of departure/arrivals.
   * <p>
   * This sorted trip times is used to search for trips. OTP assume one trip do NOT pass another
   * trip down the line.
   */
  default int sortIndex() {
    return getDepartureTime(0);
  }

  /** Sort trips based on first departure time. */
  default Comparator<TripTimes> compare() {
    return Comparator.comparingInt(TripTimes::sortIndex);
  }

  /** Sort trips based on first departure time. */
  default int compareTo(TripTimes other) {
    return sortIndex() - other.sortIndex();
  }

  BookingInfo getDropOffBookingInfo(int stopPos);

  BookingInfo getPickupBookingInfo(int stopPos);

  /**
   * Return {@code true} if the trip is unmodified, a scheduled trip from a published timetable.
   * Return {@code false} if the trip is an updated, cancelled, or otherwise modified one. This
   * method differs from {@link #getRealTimeState()} in that it checks whether real-time information
   * is actually available.
   */
  boolean isScheduled();

  /**
   * Return {@code true} if canceled or soft-deleted
   */
  boolean isCanceledOrDeleted();

  /**
   * Return {@code true} if canceled
   */
  boolean isCanceled();

  /**
   * Return true if trip is soft-deleted, and should not be visible to the user
   */
  boolean isDeleted();

  RealTimeState getRealTimeState();

  boolean isCancelledStop(int stopPos);

  boolean isRecordedStop(int stopPos);

  boolean isNoDataStop(int stopPos);

  boolean isPredictionInaccurate(int stopPos);

  /**
   * Return if trip has been updated and stop position has not been given a NO_DATA update.
   */
  boolean isRealTimeUpdated(int stopPos);

  /**
   * @return the whole trip's headsign. Individual stop positions can have different headsigns.
   */
  @Nullable
  I18NString getTripHeadsign();

  /**
   * The headsign displayed by the vehicle, which may change at each stop position along the trip.
   * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS fields.
   * A trip may not have a headsign, in which case we should fall back on a Timetable or
   * Pattern-level headsign. Such a string will be available when we give TripPatterns or
   * StopPatterns unique human-readable route variant names, but a ScheduledTripTimes currently does
   * not have a pointer to its enclosing timetable or pattern.
   */
  @Nullable
  I18NString getHeadsign(int stopPos);

  /**
   * Vias are an additional intermediate destinations between the given stop position and the
   * terminus, which are displayed alongside the terminus headsign. Vias often change or are
   * displayed only at certain stops along the way. While the concept of Headsigns exists in both
   * GTFS (Headsign) and Netex (DestinationDisplay), the Via concept is only present in Transmodel.
   * @return a list of via names visible at the given stop position, or an empty list if there are
   * no vias.
   */
  List<String> getHeadsignVias(int stopPos);

  int getNumStops();

  Accessibility getWheelchairAccessibility();

  /**
   * This is only for API-purposes (does not affect routing).
   */
  OccupancyStatus getOccupancyStatus(int stopPos);

  /**
   * Returns the GTFS sequence number of the given 0-based stop position within the pattern.
   * <p>
   * These are the GTFS stop sequence numbers, which show the order in which the vehicle visits the
   * stops. Despite the fact that the StopPattern or TripPattern enclosing this class provides an
   * ordered list of Stops, the original stop sequence numbers may still be needed for matching
   * with GTFS-RT update messages. Unfortunately, each individual trip can have totally different
   * sequence numbers for the same stops, so we need to store them at the individual trip level. An
   * effort is made to re-use the sequence number arrays when they are the same across different
   * trips in the same pattern.
   */
  int gtfsSequenceOfStopIndex(int stopPos);

  /**
   * Returns the 0-based stop index of the given GTFS sequence number.
   */
  OptionalInt stopPositionForGtfsSequence(int stopSequence);

  /**
   * Time-shift all times on this trip. This is used when updating the time zone for the trip.
   */
  T withAdjustedTimes(Duration shiftDelta);
}
