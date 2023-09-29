package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.ValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.ValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import jakarta.annotation.Nullable;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It is
 * carried along by States when routing to ensure that they have a consistent, fast view of the trip
 * when realtime updates have been applied. All times are expressed as seconds since midnight (as in
 * GTFS).
 */
public class TripTimes implements Serializable, Comparable<TripTimes> {

  private static final Logger LOG = LoggerFactory.getLogger(TripTimes.class);
  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private final Trip trip;
  private I18NString[] headsigns;
  /**
   * Implementation notes: This is 2D array since there can be more than
   * one via name/stop per each record in stop sequence). Outer array may be null if there are no
   * vias in stop sequence. Inner array may be null if there are no vias for particular stop. This
   * is done in order to save space.
   */
  private final String[][] headsignVias;
  private final int[] scheduledArrivalTimes;
  private final int[] scheduledDepartureTimes;
  private final List<BookingInfo> dropOffBookingInfos;
  private final List<BookingInfo> pickupBookingInfos;
  private final int[] originalGtfsStopSequence;
  private final BitSet timepoints;
  /**
   * This allows re-using the same scheduled arrival and departure time arrays for many
   * TripTimes. It is also used in materializing frequency-based TripTimes.
   */
  private int timeShift;

  /** Implementation notes: not final because these are set later, after TripTimes construction. */
  private int serviceCode = -1;
  /** Implementation notes: Non-final to allow updates. */
  private int[] arrivalTimes;
  /** Implementation notes: Non-final to allow updates. */
  private int[] departureTimes;

  /** Implementation notes:  Non-final to allow updates. */
  private StopRealTimeState[] stopRealTimeStates;

  /**
   *  Implementation notes: Non-final to allow updates.
   */
  private OccupancyStatus[] occupancyStatus;

  private RealTimeState realTimeState = RealTimeState.SCHEDULED;

  private Accessibility wheelchairAccessibility;

  /**
   * The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing. The
   * non-interpolated stoptimes should already be marked at timepoints by a previous filtering
   * step.
   */
  public TripTimes(
    final Trip trip,
    final Collection<StopTime> stopTimes,
    final Deduplicator deduplicator
  ) {
    this.trip = trip;
    final int nStops = stopTimes.size();
    final int[] departures = new int[nStops];
    final int[] arrivals = new int[nStops];
    final int[] sequences = new int[nStops];
    final BitSet timepoints = new BitSet(nStops);
    // Times are always shifted to zero. This is essential for frequencies and deduplication.
    this.timeShift = stopTimes.iterator().next().getArrivalTime();
    final List<BookingInfo> dropOffBookingInfos = new ArrayList<>();
    final List<BookingInfo> pickupBookingInfos = new ArrayList<>();
    int s = 0;
    for (final StopTime st : stopTimes) {
      departures[s] = st.getDepartureTime() - timeShift;
      arrivals[s] = st.getArrivalTime() - timeShift;
      sequences[s] = st.getStopSequence();
      timepoints.set(s, st.getTimepoint() == 1);

      dropOffBookingInfos.add(st.getDropOffBookingInfo());
      pickupBookingInfos.add(st.getPickupBookingInfo());
      s++;
    }
    this.scheduledDepartureTimes = deduplicator.deduplicateIntArray(departures);
    this.scheduledArrivalTimes = deduplicator.deduplicateIntArray(arrivals);
    this.originalGtfsStopSequence = deduplicator.deduplicateIntArray(sequences);
    this.headsigns =
      deduplicator.deduplicateObjectArray(I18NString.class, makeHeadsignsArray(stopTimes));
    this.headsignVias = deduplicator.deduplicateString2DArray(makeHeadsignViasArray(stopTimes));

    this.dropOffBookingInfos =
      deduplicator.deduplicateImmutableList(BookingInfo.class, dropOffBookingInfos);
    this.pickupBookingInfos =
      deduplicator.deduplicateImmutableList(BookingInfo.class, pickupBookingInfos);
    // We set these to null to indicate that this is a non-updated/scheduled TripTimes.
    // We cannot point to the scheduled times because they are shifted, and updated times are not.
    this.arrivalTimes = null;
    this.departureTimes = null;
    this.stopRealTimeStates = null;
    this.timepoints = deduplicator.deduplicateBitSet(timepoints);
    this.wheelchairAccessibility = trip.getWheelchairBoarding();
    LOG.trace("trip {} has timepoint at indexes {}", trip, timepoints);
  }

  /** This copy constructor does not copy the actual times, only the scheduled times. */
  public TripTimes(final TripTimes object) {
    this.timeShift = object.timeShift;
    this.trip = object.trip;
    this.serviceCode = object.serviceCode;
    this.headsigns = object.headsigns;
    this.headsignVias = object.headsignVias;
    this.scheduledArrivalTimes = object.scheduledArrivalTimes;
    this.scheduledDepartureTimes = object.scheduledDepartureTimes;
    this.arrivalTimes = null;
    this.departureTimes = null;
    this.stopRealTimeStates = object.stopRealTimeStates;
    this.pickupBookingInfos = object.pickupBookingInfos;
    this.dropOffBookingInfos = object.dropOffBookingInfos;
    this.originalGtfsStopSequence = object.originalGtfsStopSequence;
    this.realTimeState = object.realTimeState;
    this.timepoints = object.timepoints;
    this.wheelchairAccessibility = object.wheelchairAccessibility;
    this.occupancyStatus = object.occupancyStatus;
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
    I18NString tripHeadsign = getTrip().getHeadsign();
    if (headsigns == null) {
      return tripHeadsign;
    } else {
      I18NString stopHeadsign = headsigns[stop];
      return stopHeadsign != null ? stopHeadsign : tripHeadsign;
    }
  }

  /**
   * Return list of via names per particular stop. This field provides info about intermediate stops
   * between current stop and final trip destination. Mapped from NeTEx DestinationDisplay.vias. No
   * GTFS mapping at the moment.
   *
   * @return Empty list if there are no vias registered for a stop.
   */
  public List<String> getHeadsignVias(final int stop) {
    if (headsignVias == null || headsignVias[stop] == null) {
      return List.of();
    }
    return List.of(headsignVias[stop]);
  }

  /**
   * @return the whole trip's headsign. Individual stops can have different headsigns.
   */
  public I18NString getTripHeadsign() {
    return trip.getHeadsign();
  }

  /**
   * The time in seconds after midnight at which the vehicle should arrive at the given stop
   * according to the original schedule.
   */
  public int getScheduledArrivalTime(final int stop) {
    return scheduledArrivalTimes[stop] + timeShift;
  }

  /**
   * The time in seconds after midnight at which the vehicle should leave the given stop according
   * to the original schedule.
   */
  public int getScheduledDepartureTime(final int stop) {
    return scheduledDepartureTimes[stop] + timeShift;
  }

  /**
   * Return an integer which can be used to sort TripTimes in order of departure/arrivals.
   * <p>
   * This sorted trip times is used to search for trips. OTP assume one trip do NOT pass another
   * trip down the line.
   */
  public int sortIndex() {
    return getDepartureTime(0);
  }

  /**
   * The time in seconds after midnight at which the vehicle arrives at each stop, accounting for
   * any real-time updates.
   */
  public int getArrivalTime(final int stop) {
    if (arrivalTimes == null) {
      return getScheduledArrivalTime(stop);
    } else return arrivalTimes[stop]; // updated times are not time shifted.
  }

  /**
   * The time in seconds after midnight at which the vehicle leaves each stop, accounting for any
   * real-time updates.
   */
  public int getDepartureTime(final int stop) {
    if (departureTimes == null) {
      return getScheduledDepartureTime(stop);
    } else return departureTimes[stop]; // updated times are not time shifted.
  }

  /** @return the difference between the scheduled and actual arrival times at this stop. */
  public int getArrivalDelay(final int stop) {
    return getArrivalTime(stop) - (scheduledArrivalTimes[stop] + timeShift);
  }

  /** @return the difference between the scheduled and actual departure times at this stop. */
  public int getDepartureDelay(final int stop) {
    return getDepartureTime(stop) - (scheduledDepartureTimes[stop] + timeShift);
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

  public void setOccupancyStatus(int stop, OccupancyStatus occupancyStatus) {
    prepareForRealTimeUpdates();
    this.occupancyStatus[stop] = occupancyStatus;
  }

  /**
   * This is only for API-purposes (does not affect routing).
   */
  public OccupancyStatus getOccupancyStatus(int stop) {
    if (this.occupancyStatus == null) {
      return OccupancyStatus.NO_DATA_AVAILABLE;
    }
    return this.occupancyStatus[stop];
  }

  public BookingInfo getDropOffBookingInfo(int stop) {
    return dropOffBookingInfos.get(stop);
  }

  public BookingInfo getPickupBookingInfo(int stop) {
    return pickupBookingInfos.get(stop);
  }

  /**
   * @return true if this TripTimes represents an unmodified, scheduled trip from a published
   * timetable or false if it is a updated, cancelled, or otherwise modified one. This method
   * differs from {@link #getRealTimeState()} in that it checks whether real-time information is
   * actually available in this TripTimes.
   */
  public boolean isScheduled() {
    return realTimeState == RealTimeState.SCHEDULED;
  }

  /**
   * @return true if this TripTimes is canceled or soft-deleted
   */
  public boolean isCanceledOrDeleted() {
    return isCanceled() || isDeleted();
  }

  /**
   * @return true if this TripTimes is canceled
   */
  public boolean isCanceled() {
    return realTimeState == RealTimeState.CANCELED;
  }

  /**
   * @return true if this TripTimes is soft-deleted, and should not be visible to the user
   */
  public boolean isDeleted() {
    return realTimeState == RealTimeState.DELETED;
  }

  /**
   * @return the real-time state of this TripTimes
   */
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
   * @return empty if times were found to be increasing, stop index of the first error otherwise
   */
  public Optional<ValidationError> validateNonIncreasingTimes() {
    final int nStops = scheduledArrivalTimes.length;
    int prevDep = -9_999_999;
    for (int s = 0; s < nStops; s++) {
      final int arr = getArrivalTime(s);
      final int dep = getDepartureTime(s);

      if (dep < arr) {
        return Optional.of(new ValidationError(NEGATIVE_DWELL_TIME, s));
      }
      if (prevDep > arr) {
        return Optional.of(new ValidationError(NEGATIVE_HOP_TIME, s));
      }
      prevDep = dep;
    }
    return Optional.empty();
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
    departureTimes[stop] = scheduledDepartureTimes[stop] + timeShift + delay;
  }

  public void updateArrivalTime(final int stop, final int time) {
    prepareForRealTimeUpdates();
    arrivalTimes[stop] = time;
  }

  public void updateArrivalDelay(final int stop, final int delay) {
    prepareForRealTimeUpdates();
    arrivalTimes[stop] = scheduledArrivalTimes[stop] + timeShift + delay;
  }

  public Accessibility getWheelchairAccessibility() {
    return wheelchairAccessibility;
  }

  public void updateWheelchairAccessibility(Accessibility wheelchairAccessibility) {
    this.wheelchairAccessibility = wheelchairAccessibility;
  }

  public int getNumStops() {
    return scheduledArrivalTimes.length;
  }

  /** Sort TripTimes based on first departure time. */
  @Override
  public int compareTo(final TripTimes other) {
    return this.getDepartureTime(0) - other.getDepartureTime(0);
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
    final TripTimes shifted = new TripTimes(this);
    // Adjust 0-based times to match desired stoptime.
    final int shift = time - (depart ? getDepartureTime(stop) : getArrivalTime(stop));
    // existing shift should usually (always?) be 0 on freqs
    shifted.timeShift = shifted.timeShift + shift;
    return shifted;
  }

  // Time-shift all times on this trip. This is used when updating the time zone for the trip.
  public void timeShift(Duration duration) {
    timeShift += duration.toSeconds();
  }

  /**
   * Returns the GTFS sequence number of the given 0-based stop position.
   *
   * These are the GTFS stop sequence numbers, which show the order in which the vehicle visits the
   * stops. Despite the face that the StopPattern or TripPattern enclosing this TripTimes provides
   * an ordered list of Stops, the original stop sequence numbers may still be needed for matching
   * with GTFS-RT update messages. Unfortunately, each individual trip can have totally different
   * sequence numbers for the same stops, so we need to store them at the individual trip level. An
   * effort is made to re-use the sequence number arrays when they are the same across different
   * trips in the same pattern.
   */
  public int gtfsSequenceOfStopIndex(final int stop) {
    return originalGtfsStopSequence[stop];
  }

  /**
   * Returns the 0-based stop index of the given GTFS sequence number.
   */
  public OptionalInt stopIndexOfGtfsSequence(int stopSequence) {
    if (originalGtfsStopSequence == null) {
      return OptionalInt.empty();
    }
    for (int i = 0; i < originalGtfsStopSequence.length; i++) {
      var sequence = originalGtfsStopSequence[i];
      if (sequence == stopSequence) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  /**
   * Whether or not stopIndex is considered a GTFS timepoint.
   */
  public boolean isTimepoint(final int stopIndex) {
    return timepoints.get(stopIndex);
  }

  /** The code for the service on which this trip runs. For departure search optimizations. */
  public int getServiceCode() {
    return serviceCode;
  }

  public void setServiceCode(int serviceCode) {
    this.serviceCode = serviceCode;
  }

  /** The trips whose arrivals and departures are represented by this TripTimes */
  public Trip getTrip() {
    return trip;
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

  /**
   * @return either an array of headsigns (one for each stop on this trip) or null if the headsign
   * is the same at all stops (including null) and can be found in the Trip object.
   */
  private I18NString[] makeHeadsignsArray(final Collection<StopTime> stopTimes) {
    final I18NString tripHeadsign = trip.getHeadsign();
    boolean useStopHeadsigns = false;
    if (tripHeadsign == null) {
      useStopHeadsigns = true;
    } else {
      for (final StopTime st : stopTimes) {
        if (!(tripHeadsign.equals(st.getStopHeadsign()))) {
          useStopHeadsigns = true;
          break;
        }
      }
    }
    if (!useStopHeadsigns) {
      return null; //defer to trip_headsign
    }
    boolean allNull = true;
    int i = 0;
    final I18NString[] hs = new I18NString[stopTimes.size()];
    for (final StopTime st : stopTimes) {
      final I18NString headsign = st.getStopHeadsign();
      hs[i++] = headsign;
      if (headsign != null) allNull = false;
    }
    if (allNull) {
      return null;
    } else {
      return hs;
    }
  }

  public void setHeadsign(int index, I18NString headsign) {
    if (headsigns == null) {
      if (headsign.equals(trip.getHeadsign())) {
        return;
      }

      this.headsigns = new I18NString[scheduledArrivalTimes.length];
      this.headsigns[index] = headsign;
      return;
    }

    prepareForRealTimeUpdates();
    headsigns[index] = headsign;
  }

  /**
   * Create 2D String array for via names for each stop in sequence.
   *
   * @return May be null if no vias are present in stop sequence.
   */
  private String[][] makeHeadsignViasArray(final Collection<StopTime> stopTimes) {
    if (
      stopTimes
        .stream()
        .allMatch(st -> st.getHeadsignVias() == null || st.getHeadsignVias().isEmpty())
    ) {
      return null;
    }

    String[][] vias = new String[stopTimes.size()][];

    int i = 0;
    for (final StopTime st : stopTimes) {
      if (st.getHeadsignVias() == null) {
        vias[i] = EMPTY_STRING_ARRAY;
        i++;
        continue;
      }

      vias[i] = st.getHeadsignVias().toArray(EMPTY_STRING_ARRAY);
      i++;
    }

    return vias;
  }

  /**
   * If they don't already exist, create arrays for updated arrival and departure times that are
   * just time-shifted copies of the zero-based scheduled departure times.
   * <p>
   * Also sets the realtime state to UPDATED.
   */
  private void prepareForRealTimeUpdates() {
    if (arrivalTimes == null) {
      this.arrivalTimes = Arrays.copyOf(scheduledArrivalTimes, scheduledArrivalTimes.length);
      this.departureTimes = Arrays.copyOf(scheduledDepartureTimes, scheduledDepartureTimes.length);
      this.stopRealTimeStates = new StopRealTimeState[arrivalTimes.length];
      this.occupancyStatus = new OccupancyStatus[arrivalTimes.length];
      if (headsigns != null) {
        headsigns = Arrays.copyOf(headsigns, headsigns.length);
      }

      for (int i = 0; i < arrivalTimes.length; i++) {
        arrivalTimes[i] += timeShift;
        departureTimes[i] += timeShift;
        stopRealTimeStates[i] = StopRealTimeState.DEFAULT;
        occupancyStatus[i] = OccupancyStatus.NO_DATA_AVAILABLE;
      }

      // Update the real-time state
      realTimeState = RealTimeState.UPDATED;
    }
  }
}
