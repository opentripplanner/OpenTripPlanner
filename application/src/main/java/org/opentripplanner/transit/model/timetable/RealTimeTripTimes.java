package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It is
 * carried along by States when routing to ensure that they have a consistent, fast view of the trip
 * when realtime updates have been applied. All times are expressed as seconds since midnight (as in
 * GTFS).
 */
public final class RealTimeTripTimes implements TripTimes {

  private ScheduledTripTimes scheduledTripTimes;

  private int[] arrivalTimes;
  private int[] departureTimes;
  private RealTimeState realTimeState;
  private StopRealTimeState[] stopRealTimeStates;
  private I18NString[] headsigns;
  private OccupancyStatus[] occupancyStatus;
  private Accessibility wheelchairAccessibility;

  RealTimeTripTimes(ScheduledTripTimes scheduledTripTimes) {
    this(
      scheduledTripTimes,
      scheduledTripTimes.getRealTimeState(),
      null,
      null,
      null,
      scheduledTripTimes.getWheelchairAccessibility()
    );
  }

  private RealTimeTripTimes(RealTimeTripTimes original, ScheduledTripTimes scheduledTripTimes) {
    this(
      scheduledTripTimes,
      original.realTimeState,
      original.stopRealTimeStates,
      original.headsigns,
      original.occupancyStatus,
      original.wheelchairAccessibility
    );
  }

  private RealTimeTripTimes(
    ScheduledTripTimes scheduledTripTimes,
    RealTimeState realTimeState,
    StopRealTimeState[] stopRealTimeStates,
    I18NString[] headsigns,
    OccupancyStatus[] occupancyStatus,
    Accessibility wheelchairAccessibility
  ) {
    this.scheduledTripTimes = scheduledTripTimes;
    this.realTimeState = realTimeState;
    this.stopRealTimeStates = stopRealTimeStates;
    this.headsigns = headsigns;
    this.occupancyStatus = occupancyStatus;
    this.wheelchairAccessibility = wheelchairAccessibility;

    // We set these to null to indicate that this is a non-updated/scheduled TripTimes.
    // We cannot point to the scheduled times because we do not want to make an unnecessary copy.
    this.arrivalTimes = null;
    this.departureTimes = null;
  }

  public static RealTimeTripTimes of(ScheduledTripTimes scheduledTripTimes) {
    return new RealTimeTripTimes(scheduledTripTimes);
  }

  @Override
  public RealTimeTripTimes copyScheduledTimes() {
    return new RealTimeTripTimes(this, scheduledTripTimes);
  }

  /**
   * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS fields.
   * A trip may not have a headsign, in which case we should fall back on a Timetable or
   * Pattern-level headsign. Such a string will be available when we give TripPatterns or
   * StopPatterns unique human readable route variant names, but a TripTimes currently does not have
   * a pointer to its enclosing timetable or pattern.
   */
  @Nullable
  public I18NString getHeadsign(final int stop) {
    return (headsigns != null && headsigns[stop] != null)
      ? headsigns[stop]
      : scheduledTripTimes.getHeadsign(stop);
  }

  @Override
  public List<String> getHeadsignVias(final int stop) {
    return scheduledTripTimes.getHeadsignVias(stop);
  }

  /**
   * @return the whole trip's headsign. Individual stops can have different headsigns.
   */
  @Override
  public I18NString getTripHeadsign() {
    return scheduledTripTimes.getTripHeadsign();
  }

  /**
   * The time in seconds after midnight at which the vehicle should arrive at the given stop
   * according to the original schedule.
   */
  @Override
  public int getScheduledArrivalTime(final int stop) {
    return scheduledTripTimes.getScheduledArrivalTime(stop);
  }

  /**
   * The time in seconds after midnight at which the vehicle should leave the given stop according
   * to the original schedule.
   */
  @Override
  public int getScheduledDepartureTime(final int stop) {
    return scheduledTripTimes.getScheduledDepartureTime(stop);
  }

  /**
   * The time in seconds after midnight at which the vehicle arrives at each stop, accounting for
   * any real-time updates.
   */
  @Override
  public int getArrivalTime(final int stop) {
    return getOrElse(stop, arrivalTimes, scheduledTripTimes::getScheduledArrivalTime);
  }

  /**
   * The time in seconds after midnight at which the vehicle leaves each stop, accounting for any
   * real-time updates.
   */
  @Override
  public int getDepartureTime(final int stop) {
    return getOrElse(stop, departureTimes, scheduledTripTimes::getScheduledDepartureTime);
  }

  /** @return the difference between the scheduled and actual arrival times at this stop. */
  @Override
  public int getArrivalDelay(final int stop) {
    return getArrivalTime(stop) - scheduledTripTimes.getScheduledArrivalTime(stop);
  }

  /** @return the difference between the scheduled and actual departure times at this stop. */
  @Override
  public int getDepartureDelay(final int stop) {
    return getDepartureTime(stop) - scheduledTripTimes.getScheduledDepartureTime(stop);
  }

  public void setRecorded(int stop) {
    setStopRealTimeStates(stop, StopRealTimeState.RECORDED);
  }

  public void setCancelled(int stop) {
    setStopRealTimeStates(stop, StopRealTimeState.CANCELLED);
  }

  public void setNoData(int stop) {
    setStopRealTimeStates(stop, StopRealTimeState.NO_DATA);
  }

  public void setPredictionInaccurate(int stop) {
    setStopRealTimeStates(stop, StopRealTimeState.INACCURATE_PREDICTIONS);
  }

  public boolean isCancelledStop(int stop) {
    return isStopRealTimeStates(stop, StopRealTimeState.CANCELLED);
  }

  public boolean isRecordedStop(int stop) {
    return isStopRealTimeStates(stop, StopRealTimeState.RECORDED);
  }

  public boolean isNoDataStop(int stop) {
    return isStopRealTimeStates(stop, StopRealTimeState.NO_DATA);
  }

  public boolean isPredictionInaccurate(int stop) {
    return isStopRealTimeStates(stop, StopRealTimeState.INACCURATE_PREDICTIONS);
  }

  public boolean isRealTimeUpdated(int stop) {
    return (
      realTimeState != RealTimeState.SCHEDULED &&
      !isStopRealTimeStates(stop, StopRealTimeState.NO_DATA)
    );
  }

  public void setOccupancyStatus(int stop, OccupancyStatus occupancyStatus) {
    prepareForRealTimeUpdates();
    this.occupancyStatus[stop] = occupancyStatus;
  }

  /**
   * This is only for API-purposes (does not affect routing).
   */
  @Override
  public OccupancyStatus getOccupancyStatus(int stop) {
    if (this.occupancyStatus == null) {
      return OccupancyStatus.NO_DATA_AVAILABLE;
    }
    return this.occupancyStatus[stop];
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int stop) {
    return scheduledTripTimes.getDropOffBookingInfo(stop);
  }

  @Override
  public BookingInfo getPickupBookingInfo(int stop) {
    return scheduledTripTimes.getPickupBookingInfo(stop);
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

  public void setRealTimeState(final RealTimeState realTimeState) {
    this.realTimeState = realTimeState;
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
  public void validateNonIncreasingTimes() {
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

  /** Cancel this entire trip */
  public void cancelTrip() {
    realTimeState = RealTimeState.CANCELED;
  }

  /** Soft delete the entire trip */
  public void deleteTrip() {
    realTimeState = RealTimeState.DELETED;
  }

  public void updateDepartureTime(final int stop, final int time) {
    prepareForRealTimeUpdates();
    departureTimes[stop] = time;
  }

  public void updateDepartureDelay(final int stop, final int delay) {
    prepareForRealTimeUpdates();
    departureTimes[stop] = scheduledTripTimes.getScheduledDepartureTime(stop) + delay;
  }

  public void updateArrivalTime(final int stop, final int time) {
    prepareForRealTimeUpdates();
    arrivalTimes[stop] = time;
  }

  public void updateArrivalDelay(final int stop, final int delay) {
    prepareForRealTimeUpdates();
    arrivalTimes[stop] = scheduledTripTimes.getScheduledArrivalTime(stop) + delay;
  }

  @Nullable
  public Accessibility getWheelchairAccessibility() {
    // No need to fall back to scheduled state, since it is copied over in the constructor
    return wheelchairAccessibility;
  }

  public void updateWheelchairAccessibility(Accessibility wheelchairAccessibility) {
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public int getNumStops() {
    return scheduledTripTimes.getNumStops();
  }

  /**
   * Returns a time-shifted copy of this TripTimes in which the vehicle passes the given stop index
   * (not stop sequence number) at the given time. We only have a mechanism to shift the scheduled
   * stoptimes, not the real-time stoptimes. Therefore, this only works on trips without updates for
   * now (frequency trips don't have updates).
   */
  public TripTimes timeShift(final int stop, final int time, final boolean depart) {
    if (arrivalTimes != null || departureTimes != null) {
      return null;
    }
    // Adjust 0-based times to match desired stoptime.
    final int shift = time - (depart ? getDepartureTime(stop) : getArrivalTime(stop));

    return new RealTimeTripTimes(
      this,
      scheduledTripTimes.copyOfNoDuplication().plusTimeShift(shift).build()
    );
  }

  /**
   * Time-shift all times on this trip. This is used when updating the time zone for the trip.
   */
  @Override
  public TripTimes adjustTimesToGraphTimeZone(Duration shiftDelta) {
    return new RealTimeTripTimes(
      this,
      scheduledTripTimes.copyOfNoDuplication().plusTimeShift((int) shiftDelta.toSeconds()).build()
    );
  }

  @Override
  public int gtfsSequenceOfStopIndex(final int stop) {
    return scheduledTripTimes.gtfsSequenceOfStopIndex(stop);
  }

  @Override
  public OptionalInt stopIndexOfGtfsSequence(int stopSequence) {
    return scheduledTripTimes.stopIndexOfGtfsSequence(stopSequence);
  }

  @Override
  public boolean isTimepoint(final int stopIndex) {
    return scheduledTripTimes.isTimepoint(stopIndex);
  }

  @Override
  public int getServiceCode() {
    return scheduledTripTimes.getServiceCode();
  }

  public void setServiceCode(int serviceCode) {
    this.scheduledTripTimes = scheduledTripTimes
      .copyOfNoDuplication()
      .withServiceCode(serviceCode)
      .build();
  }

  @Override
  public Trip getTrip() {
    return scheduledTripTimes.getTrip();
  }

  /**
   * Note: This method only applies for GTFS, not SIRI!
   * This method interpolates the times for SKIPPED stops in between regular stops since GTFS-RT
   * does not require arrival and departure times for these stops. This method ensures the internal
   * time representations in OTP for SKIPPED stops are between the regular stop times immediately
   * before and after the cancellation in GTFS-RT. This is to meet the OTP requirement that stop
   * times should be increasing and to support the trip search flag `includeRealtimeCancellations`.
   * Terminal stop cancellations can be handled by backward and forward propagations, and are
   * outside the scope of this method.
   *
   * @return true if there is interpolated times, false if there is no interpolation.
   */
  public boolean interpolateMissingTimes() {
    boolean hasInterpolatedTimes = false;
    final int numStops = getNumStops();
    boolean startInterpolate = false;
    boolean hasPrevTimes = false;
    int prevDeparture = 0;
    int prevScheduledDeparture = 0;
    int prevStopIndex = -1;

    // Loop through all stops
    for (int s = 0; s < numStops; s++) {
      final boolean isCancelledStop = isCancelledStop(s);
      final int scheduledArrival = getScheduledArrivalTime(s);
      final int scheduledDeparture = getScheduledDepartureTime(s);
      final int arrival = getArrivalTime(s);
      final int departure = getDepartureTime(s);

      if (!isCancelledStop && !startInterpolate) {
        // Regular stop, could be used for interpolation for future cancellation, keep track.
        prevDeparture = departure;
        prevScheduledDeparture = scheduledDeparture;
        prevStopIndex = s;
        hasPrevTimes = true;
      } else if (isCancelledStop && !startInterpolate && hasPrevTimes) {
        // First cancelled stop, keep track.
        startInterpolate = true;
      } else if (!isCancelledStop && startInterpolate && hasPrevTimes) {
        // First regular stop after cancelled stops, interpolate.
        // Calculate necessary info for interpolation.
        int numCancelledStops = s - prevStopIndex - 1;
        int scheduledTravelTime = scheduledArrival - prevScheduledDeparture;
        int realTimeTravelTime = arrival - prevDeparture;
        double travelTimeRatio = (double) realTimeTravelTime / scheduledTravelTime;

        // Fill out interpolated time for cancelled stops, using the calculated ratio.
        for (int cancelledIndex = prevStopIndex + 1; cancelledIndex < s; cancelledIndex++) {
          final int scheduledArrivalCancelled = getScheduledArrivalTime(cancelledIndex);
          final int scheduledDepartureCancelled = getScheduledDepartureTime(cancelledIndex);

          // Interpolate
          int scheduledArrivalDiff = scheduledArrivalCancelled - prevScheduledDeparture;
          double interpolatedArrival = prevDeparture + travelTimeRatio * scheduledArrivalDiff;
          int scheduledDepartureDiff = scheduledDepartureCancelled - prevScheduledDeparture;
          double interpolatedDeparture = prevDeparture + travelTimeRatio * scheduledDepartureDiff;

          // Set Interpolated Times
          updateArrivalTime(cancelledIndex, (int) interpolatedArrival);
          updateDepartureTime(cancelledIndex, (int) interpolatedDeparture);
        }

        // Set tracking variables
        prevDeparture = departure;
        prevScheduledDeparture = scheduledDeparture;
        prevStopIndex = s;
        startInterpolate = false;
        hasPrevTimes = true;

        // Set return variable
        hasInterpolatedTimes = true;
      }
    }

    return hasInterpolatedTimes;
  }

  /**
   * Adjusts arrival time for the stop at the firstUpdatedIndex if no update was given for it and
   * arrival/departure times for the stops before that stop. Returns {@code true} if times have been
   * adjusted.
   */
  public boolean adjustTimesBeforeAlways(int firstUpdatedIndex) {
    boolean hasAdjustedTimes = false;
    int delay = getDepartureDelay(firstUpdatedIndex);
    if (getArrivalDelay(firstUpdatedIndex) == 0) {
      updateArrivalDelay(firstUpdatedIndex, delay);
      hasAdjustedTimes = true;
    }
    delay = getArrivalDelay(firstUpdatedIndex);
    if (delay == 0) {
      return false;
    }
    for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
      hasAdjustedTimes = true;
      updateDepartureDelay(i, delay);
      updateArrivalDelay(i, delay);
    }
    return hasAdjustedTimes;
  }

  /**
   * Adjusts arrival and departure times for the stops before the stop at firstUpdatedIndex when
   * required to ensure that the times are increasing. Can set NO_DATA flag on the updated previous
   * stops. Returns {@code true} if times have been adjusted.
   */
  public boolean adjustTimesBeforeWhenRequired(int firstUpdatedIndex, boolean setNoData) {
    if (getArrivalTime(firstUpdatedIndex) > getDepartureTime(firstUpdatedIndex)) {
      // The given trip update has arrival time after departure time for the first updated stop.
      // This method doesn't try to fix issues in the given data, only for the missing part
      return false;
    }
    int nextStopArrivalTime = getArrivalTime(firstUpdatedIndex);
    int delay = getArrivalDelay(firstUpdatedIndex);
    boolean hasAdjustedTimes = false;
    boolean adjustTimes = true;
    for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
      if (setNoData && !isCancelledStop(i)) {
        setNoData(i);
      }
      if (adjustTimes) {
        if (getDepartureTime(i) < nextStopArrivalTime) {
          adjustTimes = false;
          continue;
        } else {
          hasAdjustedTimes = true;
          updateDepartureDelay(i, delay);
        }
        if (getArrivalTime(i) < getDepartureTime(i)) {
          adjustTimes = false;
        } else {
          updateArrivalDelay(i, delay);
          nextStopArrivalTime = getArrivalTime(i);
        }
      }
    }
    return hasAdjustedTimes;
  }

  /* private member methods */

  private void setStopRealTimeStates(int stop, StopRealTimeState state) {
    prepareForRealTimeUpdates();
    this.stopRealTimeStates[stop] = state;
  }

  /**
   * The real-time states for a given stops. If the state is DEFAULT for a stop,
   * the {@link #getRealTimeState()} should determine the realtime state of the stop.
   * <p>
   * This is only for API-purposes (does not affect routing).
   */
  private boolean isStopRealTimeStates(int stop, StopRealTimeState state) {
    return stopRealTimeStates != null && stopRealTimeStates[stop] == state;
  }

  public void setHeadsign(int index, I18NString headsign) {
    if (headsigns == null) {
      if (headsign.equals(getTrip().getHeadsign())) {
        return;
      }
      this.headsigns = scheduledTripTimes.copyHeadsigns(() -> new I18NString[getNumStops()]);
      this.headsigns[index] = headsign;
      return;
    }

    prepareForRealTimeUpdates();
    headsigns[index] = headsign;
  }

  private static int getOrElse(int index, int[] array, IntUnaryOperator defaultValue) {
    return array != null ? array[index] : defaultValue.applyAsInt(index);
  }

  /**
   * If they don't already exist, create arrays for updated arrival and departure times that are
   * just time-shifted copies of the zero-based scheduled departure times.
   * <p>
   * Also sets the realtime state to UPDATED.
   */
  private void prepareForRealTimeUpdates() {
    if (arrivalTimes == null) {
      this.arrivalTimes = scheduledTripTimes.copyArrivalTimes();
      this.departureTimes = scheduledTripTimes.copyDepartureTimes();
      // Update the real-time state
      this.realTimeState = RealTimeState.UPDATED;
      this.stopRealTimeStates = new StopRealTimeState[arrivalTimes.length];
      Arrays.fill(stopRealTimeStates, StopRealTimeState.DEFAULT);
      this.headsigns = scheduledTripTimes.copyHeadsigns(() -> null);
      this.occupancyStatus = new OccupancyStatus[arrivalTimes.length];
      Arrays.fill(occupancyStatus, OccupancyStatus.NO_DATA_AVAILABLE);
      // skip immutable types: scheduledTripTimes & wheelchairAccessibility
    }
  }
}
