package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.time.Duration;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.error.OtpError;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.DeduplicatorService;

/**
 * Regular/planed/scheduled read-only version of {@link TripTimes}. The set of static
 * trip-times are build during graph-build and can not be changed using real-time updates.
 *
 * @see RealTimeTripTimes for real-time version
 */
public final class ScheduledTripTimes implements TripTimes {

  /**
   * When time-shifting from one time-zone to another negative times may occur.
   */
  private static final int MIN_TIME = DurationUtils.durationInSeconds("-12h");
  /**
   * We allow a trip to last for maximum 20 days. In Norway the longest trip is 6 days.
   */
  private static final int MAX_TIME = DurationUtils.durationInSeconds("20d");

  /**
   * Implementation notes: This allows re-using the same scheduled arrival and departure time
   * arrays for many ScheduledTripTimes. It is also used in materializing frequency-based
   * ScheduledTripTimes.
   */
  private final int timeShift;
  private final int serviceCode;
  private final int[] arrivalTimes;
  private final int[] departureTimes;
  private final BitSet timepoints;
  private final Trip trip;
  private final List<BookingInfo> dropOffBookingInfos;
  private final List<BookingInfo> pickupBookingInfos;

  @Nullable
  private final I18NString[] headsigns;

  /**
   * Implementation notes: This is 2D array since there can be more than one via name/stop per each
   * record in stop sequence). Outer array may be null if there are no vias in stop sequence. Inner
   * array may be null if there are no vias for particular stop. This is done in order to save
   * space.
   */
  @Nullable
  private final String[][] headsignVias;

  private final int[] originalGtfsStopSequence;

  ScheduledTripTimes(ScheduledTripTimesBuilder builder) {
    this.timeShift = builder.timeShift();
    this.serviceCode = builder.serviceCode();
    this.arrivalTimes = Objects.requireNonNull(builder.arrivalTimes());
    this.departureTimes = Objects.requireNonNull(builder.departureTimes());
    this.timepoints = Objects.requireNonNull(builder.timepoints());
    this.trip = Objects.requireNonNull(builder.trip());
    this.pickupBookingInfos = Objects.requireNonNull(builder.pickupBookingInfos());
    this.dropOffBookingInfos = Objects.requireNonNull(builder.dropOffBookingInfos());
    this.headsigns = builder.headsigns();
    this.headsignVias = builder.headsignVias();
    this.originalGtfsStopSequence = builder.originalGtfsStopSequence();
    validate();
  }

  /**
   * Always provide a deduplicator when building the graph. No deduplication is ok when changing
   * simple fields like {@code timeShift} and {@code serviceCode} or even the prefered way in an
   * unittest.
   */
  public static ScheduledTripTimesBuilder of() {
    return new ScheduledTripTimesBuilder(null);
  }

  public static ScheduledTripTimesBuilder of(DeduplicatorService deduplicator) {
    return new ScheduledTripTimesBuilder(deduplicator);
  }

  public ScheduledTripTimesBuilder copyOf(Deduplicator deduplicator) {
    return new ScheduledTripTimesBuilder(
      timeShift,
      serviceCode,
      arrivalTimes,
      departureTimes,
      timepoints,
      trip,
      dropOffBookingInfos,
      pickupBookingInfos,
      headsigns,
      headsignVias,
      originalGtfsStopSequence,
      deduplicator
    );
  }

  /**
   * @see #copyOf(Deduplicator) copyOf(null)
   */
  public ScheduledTripTimesBuilder copyOfNoDuplication() {
    return copyOf(null);
  }

  @Override
  public RealTimeTripTimes copyScheduledTimes() {
    return RealTimeTripTimes.of(this);
  }

  @Override
  public TripTimes adjustTimesToGraphTimeZone(Duration shiftDelta) {
    return copyOfNoDuplication().plusTimeShift((int) shiftDelta.toSeconds()).build();
  }

  @Override
  public int getServiceCode() {
    return serviceCode;
  }

  @Override
  public int getScheduledArrivalTime(final int stop) {
    return timeShifted(arrivalTimes[stop]);
  }

  @Override
  public int getArrivalTime(final int stop) {
    return getScheduledArrivalTime(stop);
  }

  @Override
  public int getArrivalDelay(final int stop) {
    return getArrivalTime(stop) - timeShifted(arrivalTimes[stop]);
  }

  @Override
  public int getScheduledDepartureTime(final int stop) {
    return timeShifted(departureTimes[stop]);
  }

  @Override
  public int getDepartureTime(final int stop) {
    return getScheduledDepartureTime(stop);
  }

  @Override
  public int getDepartureDelay(final int stop) {
    return getDepartureTime(stop) - timeShifted(departureTimes[stop]);
  }

  @Override
  public boolean isTimepoint(final int stopIndex) {
    return timepoints.get(stopIndex);
  }

  @Override
  public Trip getTrip() {
    return trip;
  }

  @Override
  public int sortIndex() {
    return getDepartureTime(0);
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int stop) {
    return dropOffBookingInfos.get(stop);
  }

  @Override
  public BookingInfo getPickupBookingInfo(int stop) {
    return pickupBookingInfos.get(stop);
  }

  @Override
  public boolean isScheduled() {
    return true;
  }

  @Override
  public boolean isCanceledOrDeleted() {
    return false;
  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public boolean isDeleted() {
    return false;
  }

  @Override
  public RealTimeState getRealTimeState() {
    return RealTimeState.SCHEDULED;
  }

  @Override
  public boolean isCancelledStop(int stop) {
    return false;
  }

  @Override
  public boolean isRecordedStop(int stop) {
    return false;
  }

  @Override
  public boolean isNoDataStop(int stop) {
    return false;
  }

  @Override
  public boolean isPredictionInaccurate(int stop) {
    return false;
  }

  @Override
  public I18NString getTripHeadsign() {
    return trip.getHeadsign();
  }

  @Override
  @Nullable
  public I18NString getHeadsign(final int stop) {
    return (headsigns != null && headsigns[stop] != null)
      ? headsigns[stop]
      : getTrip().getHeadsign();
  }

  @Override
  public List<String> getHeadsignVias(final int stop) {
    if (headsignVias == null || headsignVias[stop] == null) {
      return List.of();
    }
    return List.of(headsignVias[stop]);
  }

  @Override
  public int getNumStops() {
    return arrivalTimes.length;
  }

  @Override
  public Accessibility getWheelchairAccessibility() {
    return trip.getWheelchairBoarding();
  }

  @Override
  public OccupancyStatus getOccupancyStatus(int ignore) {
    return OccupancyStatus.NO_DATA_AVAILABLE;
  }

  @Override
  public int gtfsSequenceOfStopIndex(final int stop) {
    return originalGtfsStopSequence[stop];
  }

  @Override
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

  @Override
  public boolean equals(Object o) {
    throw new UnsupportedOperationException("Not implemented, implement if needed!");
  }

  @Override
  public int hashCode() {
    throw new UnsupportedOperationException("Not implemented, implement if needed!");
  }

  /* package local - only visible to timetable classes */

  int[] copyArrivalTimes() {
    return IntUtils.shiftArray(timeShift, arrivalTimes);
  }

  int[] copyDepartureTimes() {
    return IntUtils.shiftArray(timeShift, departureTimes);
  }

  I18NString[] copyHeadsigns(Supplier<I18NString[]> defaultValue) {
    return headsigns == null ? defaultValue.get() : Arrays.copyOf(headsigns, headsigns.length);
  }

  /* private methods */

  private void validate() {
    // Validate first departure time and last arrival time
    validateTimeInRange("departureTime", departureTimes, 0);
    validateTimeInRange("arrivalTime", arrivalTimes, arrivalTimes.length - 1);
    // TODO: This class is used by FLEX, so we can not validate increasing TripTimes
    // validateNonIncreasingTimes();
  }

  /**
   * When creating scheduled trip times we could potentially imply negative running or dwell times.
   * We really don't want those being used in routing. This method checks that all times are
   * increasing. The first stop arrival time and the last stops departure time is NOT checked -
   * these should be ignored by raptor.
   */
  private void validateNonIncreasingTimes() {
    final int lastStop = arrivalTimes.length - 1;

    // This check is currently used since Flex trips may have only one stop. This class should
    // not be used to represent FLEX, so remove this check and create new data classes for FLEX
    // trips.
    if (lastStop < 1) {
      return;
    }
    int prevDep = getDepartureTime(0);

    for (int i = 1; true; ++i) {
      final int arr = getArrivalTime(i);
      final int dep = getDepartureTime(i);

      if (prevDep > arr) {
        throw new DataValidationException(new TimetableValidationError(NEGATIVE_HOP_TIME, i, trip));
      }
      if (i == lastStop) {
        return;
      }
      if (dep < arr) {
        throw new DataValidationException(
          new TimetableValidationError(NEGATIVE_DWELL_TIME, i, trip)
        );
      }
      prevDep = dep;
    }
  }

  private int timeShifted(int time) {
    return timeShift + time;
  }

  private void validateTimeInRange(String field, int[] times, int stopPos) {
    int t = timeShifted(times[stopPos]);

    if (t < MIN_TIME || t > MAX_TIME) {
      throw new DataValidationException(
        OtpError.of(
          "TripTimeOutOfRange",
          "The %s is not in range[%s, %s]. Time: %s, stop-pos: %d, trip: %s.",
          field,
          DurationUtils.durationToStr(MIN_TIME),
          DurationUtils.durationToStr(MAX_TIME),
          TimeUtils.timeToStrLong(t),
          stopPos,
          trip.getId()
        )
      );
    }
  }
}
