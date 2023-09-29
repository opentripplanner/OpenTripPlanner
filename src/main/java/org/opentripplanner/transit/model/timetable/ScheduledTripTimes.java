package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.ValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.ValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.Deduplicator;

final class ScheduledTripTimes implements Serializable, Comparable<ScheduledTripTimes> {

  private static final String[] EMPTY_STRING_ARRAY = new String[0];
  private static final int NOT_SET = -1;

  private final int[] scheduledArrivalTimes;
  private final int[] scheduledDepartureTimes;
  /**
   * This allows re-using the same scheduled arrival and departure time arrays for many
   * ScheduledTripTimes. It is also used in materializing frequency-based ScheduledTripTimes.
   */
  private final int timeShift;

  /** Implementation notes: not final because these are set after construction. */
  private int serviceCode = NOT_SET;
  private final BitSet timepoints;
  private final List<BookingInfo> dropOffBookingInfos;
  private final List<BookingInfo> pickupBookingInfos;
  private final Trip trip;

  @Nullable
  private final I18NString[] headsigns;

  /**
   * Implementation notes: This is 2D array since there can be more than
   * one via name/stop per each record in stop sequence). Outer array may be null if there are no
   * vias in stop sequence. Inner array may be null if there are no vias for particular stop. This
   * is done in order to save space.
   */
  @Nullable
  private final String[][] headsignVias;

  private final int[] originalGtfsStopSequence;

  /**
   * The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing. The
   * non-interpolated stoptimes should already be marked at timepoints by a previous filtering
   * step.
   */
  public ScheduledTripTimes(
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
    this.timepoints = deduplicator.deduplicateBitSet(timepoints);
  }

  /** This copy constructor does not copy the actual times, only the scheduled times. */
  public ScheduledTripTimes(final ScheduledTripTimes object) {
    this.timeShift = object.timeShift;
    this.trip = object.trip;
    this.serviceCode = object.serviceCode;
    this.headsigns = object.headsigns;
    this.headsignVias = object.headsignVias;
    this.scheduledArrivalTimes = object.scheduledArrivalTimes;
    this.scheduledDepartureTimes = object.scheduledDepartureTimes;
    this.pickupBookingInfos = object.pickupBookingInfos;
    this.dropOffBookingInfos = object.dropOffBookingInfos;
    this.originalGtfsStopSequence = object.originalGtfsStopSequence;
    this.timepoints = object.timepoints;
  }

  /**
   * This is a temporary constructor to allow timeShifting.
   * TODO TT - It should be replaced by a builder.
   */
  ScheduledTripTimes(final ScheduledTripTimes object, int timeShiftDelta) {
    this.timeShift = object.timeShift + timeShiftDelta;
    this.trip = object.trip;
    this.serviceCode = object.serviceCode;
    this.headsigns = object.headsigns;
    this.headsignVias = object.headsignVias;
    this.scheduledArrivalTimes = object.scheduledArrivalTimes;
    this.scheduledDepartureTimes = object.scheduledDepartureTimes;
    this.pickupBookingInfos = object.pickupBookingInfos;
    this.dropOffBookingInfos = object.dropOffBookingInfos;
    this.originalGtfsStopSequence = object.originalGtfsStopSequence;
    this.timepoints = object.timepoints;
  }

  /**
   * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS fields.
   * A trip may not have a headsign, in which case we should fall back on a Timetable or
   * Pattern-level headsign. Such a string will be available when we give TripPatterns or
   * StopPatterns unique human readable route variant names, but a ScheduledTripTimes currently
   * does not have a pointer to its enclosing timetable or pattern.
   */
  @Nullable
  public I18NString getHeadsign(final int stop) {
    return (headsigns != null && headsigns[stop] != null)
      ? headsigns[stop]
      : getTrip().getHeadsign();
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
    return getScheduledArrivalTime(stop);
  }

  /**
   * The time in seconds after midnight at which the vehicle leaves each stop, accounting for any
   * real-time updates.
   */
  public int getDepartureTime(final int stop) {
    return getScheduledDepartureTime(stop);
  }

  /** @return the difference between the scheduled and actual arrival times at this stop. */
  public int getArrivalDelay(final int stop) {
    return getArrivalTime(stop) - (scheduledArrivalTimes[stop] + timeShift);
  }

  /** @return the difference between the scheduled and actual departure times at this stop. */
  public int getDepartureDelay(final int stop) {
    return getDepartureTime(stop) - (scheduledDepartureTimes[stop] + timeShift);
  }

  /**
   * This is only for API-purposes (does not affect routing).
   */
  public OccupancyStatus getOccupancyStatus(int stop) {
    return OccupancyStatus.NO_DATA_AVAILABLE;
  }

  public BookingInfo getDropOffBookingInfo(int stop) {
    return dropOffBookingInfos.get(stop);
  }

  public BookingInfo getPickupBookingInfo(int stop) {
    return pickupBookingInfos.get(stop);
  }

  /**
   * Return {@code true} if the trip is unmodified, a scheduled trip from a published timetable.
   * Return {@code false} if the trip is an updated, cancelled, or otherwise modified one. This
   * method differs from {@link #getRealTimeState()} in that it checks whether real-time
   * information is actually available.
   */
  public boolean isScheduled() {
    return true;
  }

  /**
   * Return {@code true} if canceled or soft-deleted
   */
  public boolean isCanceledOrDeleted() {
    return false;
  }

  /**
   * Return {@code true} if canceled
   */
  public boolean isCanceled() {
    return false;
  }

  /**
   * Return true if trip is soft-deleted, and should not be visible to the user
   */
  public boolean isDeleted() {
    return false;
  }

  public RealTimeState getRealTimeState() {
    return RealTimeState.SCHEDULED;
  }

  /**
   * When creating ScheduledTripTimes or wrapping it in updates, we could potentially imply
   * negative running or dwell times. We really don't want those being used in routing. This method
   * checks that all times are increasing.
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

  public Accessibility getWheelchairAccessibility() {
    return trip.getWheelchairBoarding();
  }

  public int getNumStops() {
    return scheduledArrivalTimes.length;
  }

  /** Sort trips based on first departure time. */
  @Override
  public int compareTo(final ScheduledTripTimes other) {
    return this.getDepartureTime(0) - other.getDepartureTime(0);
  }

  /**
   * Returns the GTFS sequence number of the given 0-based stop position.
   *
   * These are the GTFS stop sequence numbers, which show the order in which the vehicle visits the
   * stops. Despite the face that the StopPattern or TripPattern enclosing this class provides
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

  /** The trips whose arrivals and departures are represented by this class */
  public Trip getTrip() {
    return trip;
  }

  /* package local - only visible to timetable classes */

  int[] copyArrivalTimes() {
    return IntUtils.shiftArray(timeShift, scheduledArrivalTimes);
  }

  int[] copyDepartureTimes() {
    return IntUtils.shiftArray(timeShift, scheduledDepartureTimes);
  }

  I18NString[] copyHeadsigns(Supplier<I18NString[]> defaultValue) {
    return headsigns == null ? defaultValue.get() : Arrays.copyOf(headsigns, headsigns.length);
  }

  /* private member methods */

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
}
