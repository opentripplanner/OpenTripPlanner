package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.time.Duration;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It is
 * carried along by States when routing to ensure that they have a consistent, fast view of the trip
 * when realtime updates have been applied. All times are expressed as seconds since midnight (as in
 * GTFS).
 */

public final class RealTimeTripTimes implements TripTimes<RealTimeTripTimes> {

  private final ScheduledTripTimes scheduledTripTimes;

  private final int[] arrivalTimes;
  private final int[] departureTimes;
  private final RealTimeState realTimeState;
  private final StopRealTimeState[] stopRealTimeStates;
  private final BitSet extraCalls;
  private final BitSet hasArrived;
  private final BitSet hasDeparted;

  @Nullable
  private final I18NString tripHeadsign;

  private final I18NString[] stopHeadsigns;
  private final OccupancyStatus[] occupancyStatus;
  private final Accessibility wheelchairAccessibility;

  RealTimeTripTimes(RealTimeTripTimesBuilder builder) {
    scheduledTripTimes = builder.scheduledTripTimes();
    arrivalTimes = builder.arrivalTimes();
    departureTimes = builder.departureTimes();
    realTimeState = builder.realTimeState();
    stopRealTimeStates = builder.stopRealTimeStates();
    extraCalls = builder.extraCalls();
    tripHeadsign = builder.tripHeadsign();
    stopHeadsigns = builder.stopHeadsigns();
    occupancyStatus = builder.occupancyStatus();
    wheelchairAccessibility = builder.wheelchairAccessibility();
    hasArrived = builder.hasArrived();
    hasDeparted = builder.hasDeparted();
    validateNonIncreasingTimes();
  }

  /**
   * Replace the scheduled times, leaving everything else intact. Used to change service code.
   */
  private RealTimeTripTimes(RealTimeTripTimes original, ScheduledTripTimes scheduledTripTimes) {
    this.scheduledTripTimes = scheduledTripTimes;
    this.arrivalTimes = original.arrivalTimes;
    this.departureTimes = original.departureTimes;
    this.realTimeState = original.realTimeState;
    this.stopRealTimeStates = original.stopRealTimeStates;
    this.extraCalls = original.extraCalls;
    this.tripHeadsign = original.tripHeadsign;
    this.stopHeadsigns = original.stopHeadsigns;
    this.occupancyStatus = original.occupancyStatus;
    this.wheelchairAccessibility = original.wheelchairAccessibility;
    this.hasArrived = original.hasArrived;
    this.hasDeparted = original.hasDeparted;
  }

  /**
   * Time shift all the times, including scheduled and real times. Used to change time zone.
   */
  private RealTimeTripTimes(RealTimeTripTimes original, int timeShift) {
    this.scheduledTripTimes = original.scheduledTripTimes
      .copyOfNoDuplication()
      .plusTimeShift(timeShift)
      .build();
    this.arrivalTimes = IntUtils.shiftArray(timeShift, original.arrivalTimes);
    this.departureTimes = IntUtils.shiftArray(timeShift, original.departureTimes);
    this.realTimeState = original.realTimeState;
    this.stopRealTimeStates = original.stopRealTimeStates;
    this.extraCalls = original.extraCalls;
    this.tripHeadsign = original.tripHeadsign;
    this.stopHeadsigns = original.stopHeadsigns;
    this.occupancyStatus = original.occupancyStatus;
    this.wheelchairAccessibility = original.wheelchairAccessibility;
    this.hasArrived = original.hasArrived;
    this.hasDeparted = original.hasDeparted;
  }

  ScheduledTripTimes scheduledTripTimes() {
    return scheduledTripTimes;
  }

  @Override
  public RealTimeTripTimesBuilder createRealTimeWithoutScheduledTimes() {
    return new RealTimeTripTimesBuilder(scheduledTripTimes);
  }

  @Override
  public RealTimeTripTimesBuilder createRealTimeFromScheduledTimes() {
    return RealTimeTripTimesBuilder.fromScheduledTimes(scheduledTripTimes);
  }

  /**
   * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS fields.
   * A trip may not have a headsign, in which case we should fall back on a Timetable or
   * Pattern-level headsign. Such a string will be available when we give TripPatterns or
   * StopPatterns unique human readable route variant names, but a TripTimes currently does not have
   * a pointer to its enclosing timetable or pattern.
   */
  @Nullable
  public I18NString getHeadsign(final int stopPos) {
    return stopHeadsigns[stopPos] != null ? stopHeadsigns[stopPos] : tripHeadsign;
  }

  @Override
  public List<String> getHeadsignVias(final int stopPos) {
    return scheduledTripTimes.getHeadsignVias(stopPos);
  }

  /**
   * @return the whole trip's headsign. Individual stops can have different headsigns.
   */
  @Nullable
  @Override
  public I18NString getTripHeadsign() {
    return tripHeadsign;
  }

  /**
   * The time in seconds after midnight at which the vehicle should arrive at the given stop
   * according to the original schedule.
   */
  @Override
  public int getScheduledArrivalTime(final int stopPos) {
    return scheduledTripTimes.getScheduledArrivalTime(stopPos);
  }

  /**
   * The time in seconds after midnight at which the vehicle should leave the given stop according
   * to the original schedule.
   */
  @Override
  public int getScheduledDepartureTime(final int stopPos) {
    return scheduledTripTimes.getScheduledDepartureTime(stopPos);
  }

  /**
   * The time in seconds after midnight at which the vehicle arrives at each stop, accounting for
   * any real-time updates.
   */
  @Override
  public int getArrivalTime(final int stop) {
    return arrivalTimes[stop];
  }

  /**
   * The time in seconds after midnight at which the vehicle leaves each stop, accounting for any
   * real-time updates.
   */
  @Override
  public int getDepartureTime(final int stopPos) {
    return departureTimes[stopPos];
  }

  /** @return the difference between the scheduled and actual arrival times at this stop. */
  @Override
  public int getArrivalDelay(final int stopPos) {
    return getArrivalTime(stopPos) - scheduledTripTimes.getScheduledArrivalTime(stopPos);
  }

  /** @return the difference between the scheduled and actual departure times at this stop. */
  @Override
  public int getDepartureDelay(final int stopPos) {
    return getDepartureTime(stopPos) - scheduledTripTimes.getScheduledDepartureTime(stopPos);
  }

  public boolean isCancelledStop(int stopPos) {
    return isStopRealTimeStates(stopPos, StopRealTimeState.CANCELLED);
  }

  @Override
  public boolean hasArrived(int stopPos) {
    return hasArrived.get(stopPos);
  }

  @Override
  public boolean hasDeparted(int stopPos) {
    return hasDeparted.get(stopPos);
  }

  public boolean isNoDataStop(int stopPos) {
    return isStopRealTimeStates(stopPos, StopRealTimeState.NO_DATA);
  }

  public boolean isPredictionInaccurate(int stopPos) {
    return isStopRealTimeStates(stopPos, StopRealTimeState.INACCURATE_PREDICTIONS);
  }

  public boolean isExtraCall(int stopPos) {
    return extraCalls.get(stopPos);
  }

  public boolean isRealTimeUpdated(int stopPos) {
    return (
      realTimeState != RealTimeState.SCHEDULED &&
      !isStopRealTimeStates(stopPos, StopRealTimeState.NO_DATA)
    );
  }

  /**
   * This is only for API-purposes (does not affect routing).
   */
  @Override
  public OccupancyStatus getOccupancyStatus(int stopPos) {
    if (this.occupancyStatus == null) {
      return OccupancyStatus.NO_DATA_AVAILABLE;
    }
    return this.occupancyStatus[stopPos];
  }

  OccupancyStatus[] copyOccupancyStatus() {
    return occupancyStatus.clone();
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int stopPos) {
    return scheduledTripTimes.getDropOffBookingInfo(stopPos);
  }

  @Override
  public BookingInfo getPickupBookingInfo(int stopPos) {
    return scheduledTripTimes.getPickupBookingInfo(stopPos);
  }

  @Override
  public boolean isScheduled() {
    return realTimeState == RealTimeState.SCHEDULED;
  }

  @Override
  public boolean isCanceledOrDeleted() {
    return isCanceled() || isDeleted();
  }

  @Override
  public boolean isCanceled() {
    return realTimeState == RealTimeState.CANCELED;
  }

  @Override
  public boolean isDeleted() {
    return realTimeState == RealTimeState.DELETED;
  }

  @Override
  public RealTimeState getRealTimeState() {
    return realTimeState;
  }

  /**
   * When creating a scheduled TripTimes or wrapping it in updates, we could potentially imply
   * negative running or dwell times. We really don't want those being used in routing. This method
   * checks that all internal times are increasing. Thus, this check should be used at the end of
   * updating trip times, after any propagating or interpolating delay operations.
   *
   * @throws org.opentripplanner.transit.model.framework.DataValidationException of the first error
   * found.
   *
   * Note! This is a duplicate (almost) of the same method in ScheduledTripTimes.
   * We should aim for just one implementation. We need to decide how to do this.
   * A common abstract base class would simplify it, but may lead to other problems and performance
   * overhead. We should look back on this after refactoring
   * the rest of the timetable classes (calendar/patterns).
   */
  private void validateNonIncreasingTimes() {
    final int nStops = scheduledTripTimes.getNumStops();
    int prevDep = -9_999_999;
    for (int s = 0; s < nStops; s++) {
      final int arr = getArrivalTime(s);
      final int dep = getDepartureTime(s);

      if (dep < arr) {
        throw new DataValidationException(
          new TimetableValidationError(NEGATIVE_DWELL_TIME, s, getTrip())
        );
      }
      if (prevDep > arr) {
        throw new DataValidationException(
          new TimetableValidationError(NEGATIVE_HOP_TIME, s, getTrip())
        );
      }
      prevDep = dep;
    }
  }

  @Nullable
  public Accessibility getWheelchairAccessibility() {
    // No need to fall back to scheduled state, since it is copied over in the constructor
    return wheelchairAccessibility;
  }

  public int getNumStops() {
    return scheduledTripTimes.getNumStops();
  }

  /**
   * Time-shift all times on this trip. This is used when updating the time zone for the trip.
   */
  @Override
  public RealTimeTripTimes withAdjustedTimes(Duration shiftDelta) {
    return new RealTimeTripTimes(this, (int) shiftDelta.toSeconds());
  }

  @Override
  public int gtfsSequenceOfStopIndex(final int stopPos) {
    return scheduledTripTimes.gtfsSequenceOfStopIndex(stopPos);
  }

  @Override
  public OptionalInt stopPositionForGtfsSequence(int stopSequence) {
    return scheduledTripTimes.stopPositionForGtfsSequence(stopSequence);
  }

  @Override
  public boolean isTimepoint(final int stopPos) {
    return scheduledTripTimes.isTimepoint(stopPos);
  }

  @Override
  public int getServiceCode() {
    return scheduledTripTimes.getServiceCode();
  }

  public RealTimeTripTimes withServiceCode(int serviceCode) {
    return new RealTimeTripTimes(
      this,
      scheduledTripTimes.copyOfNoDuplication().withServiceCode(serviceCode).build()
    );
  }

  @Override
  public Trip getTrip() {
    return scheduledTripTimes.getTrip();
  }

  StopRealTimeState[] copyStopRealTimeStates() {
    return stopRealTimeStates.clone();
  }

  /**
   * The real-time states for a given stops. If the state is DEFAULT for a stop,
   * the {@link #getRealTimeState()} should determine the realtime state of the stop.
   * <p>
   * This is only for API-purposes (does not affect routing).
   */
  private boolean isStopRealTimeStates(int stopPos, StopRealTimeState state) {
    return stopRealTimeStates != null && stopRealTimeStates[stopPos] == state;
  }

  I18NString[] copyStopHeadsigns() {
    return stopHeadsigns.clone();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RealTimeTripTimes that = (RealTimeTripTimes) o;
    return (
      Objects.equals(scheduledTripTimes, that.scheduledTripTimes) &&
      Objects.deepEquals(arrivalTimes, that.arrivalTimes) &&
      Objects.deepEquals(departureTimes, that.departureTimes) &&
      realTimeState == that.realTimeState &&
      Objects.deepEquals(stopRealTimeStates, that.stopRealTimeStates) &&
      Objects.equals(tripHeadsign, that.tripHeadsign) &&
      Objects.deepEquals(stopHeadsigns, that.stopHeadsigns) &&
      Objects.deepEquals(occupancyStatus, that.occupancyStatus) &&
      wheelchairAccessibility == that.wheelchairAccessibility
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      scheduledTripTimes,
      Arrays.hashCode(arrivalTimes),
      Arrays.hashCode(departureTimes),
      realTimeState,
      Arrays.hashCode(stopRealTimeStates),
      tripHeadsign,
      Arrays.hashCode(stopHeadsigns),
      Arrays.hashCode(occupancyStatus),
      wheelchairAccessibility
    );
  }
}
