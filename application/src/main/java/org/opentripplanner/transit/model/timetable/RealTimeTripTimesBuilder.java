package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.MISSING_ARRIVAL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.MISSING_DEPARTURE_TIME;

import java.util.BitSet;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.transit.model.framework.DataValidationException;

public class RealTimeTripTimesBuilder {

  private ScheduledTripTimes scheduledTripTimes;
  private final Integer[] arrivalTimes;
  private final Integer[] departureTimes;

  private final BitSet extraCalls;
  private final BitSet hasArrived;
  private final BitSet hasDeparted;

  @Nullable
  private I18NString tripHeadsign;

  private final I18NString[] stopHeadsigns;

  @Nullable
  private Accessibility wheelchairAccessibility;

  private final TripRealTimeMetadata.TripRealTimeMetadataBuilder tripRealTimeMetadataBuilder;

  /**
   * This constructor takes a ScheduledTripTimes (not base TripTimes) to enforce creating a new
   * RealTimeTripTimes based on the scheduled info. RT updates are  NOT cumulative and this
   * enforces copying the scheduled information, not the previous real-time update.
   * <p>
   * The arrival and departure times are left uninitialized by this constructor, and they need to
   * be set explicitly.
   */
  RealTimeTripTimesBuilder(ScheduledTripTimes tripTimes) {
    this.scheduledTripTimes = tripTimes;
    var numStops = tripTimes.getNumStops();
    arrivalTimes = new Integer[numStops];
    departureTimes = new Integer[numStops];
    extraCalls = new BitSet(numStops);
    stopHeadsigns = new I18NString[numStops];
    hasArrived = new BitSet(numStops);
    hasDeparted = new BitSet(numStops);
    tripRealTimeMetadataBuilder = TripRealTimeMetadata.builder(numStops);
  }

  /**
   * Does this stop have any real-time update on the departure or arrival time?
   */
  public boolean containsNoRealTimeTimes(int i) {
    return getArrivalDelay(i) == null && getDepartureDelay(i) == null;
  }

  static RealTimeTripTimesBuilder fromScheduledTimes(ScheduledTripTimes tripTimes) {
    var instance = new RealTimeTripTimesBuilder(tripTimes);
    instance.copyMissingTimesFromScheduledTimetable();
    return instance;
  }

  public ScheduledTripTimes scheduledTripTimes() {
    return scheduledTripTimes;
  }

  public Trip getTrip() {
    return scheduledTripTimes.getTrip();
  }

  public int numberOfStops() {
    return scheduledTripTimes().getNumStops();
  }

  /**
   * Returns a stream of the positions of the stops in these trip times (starting at 0). Useful
   * for iterating over them.
   */
  public IntStream listStopPositions() {
    return IntStream.range(0, numberOfStops());
  }

  public int[] arrivalTimes() {
    var result = new int[arrivalTimes.length];
    for (int i = 0; i < arrivalTimes.length; i++) {
      if (arrivalTimes[i] == null) {
        throw new DataValidationException(
          new TimetableValidationError(MISSING_ARRIVAL_TIME, i, getTrip())
        );
      }
      result[i] = arrivalTimes[i];
    }
    return result;
  }

  @Nullable
  public Integer getArrivalTime(int stop) {
    return arrivalTimes[stop];
  }

  public int getScheduledArrivalTime(int stop) {
    return scheduledTripTimes().getScheduledArrivalTime(stop);
  }

  /** @return the difference between the scheduled and actual arrival times at this stop. */
  @Nullable
  public Integer getArrivalDelay(int stop) {
    if (arrivalTimes[stop] == null) {
      return null;
    }
    return arrivalTimes[stop] - getScheduledArrivalTime(stop);
  }

  public RealTimeTripTimesBuilder withArrivalTime(int stop, int time) {
    arrivalTimes[stop] = time;
    return this;
  }

  public RealTimeTripTimesBuilder withArrivalDelay(int stop, int delay) {
    arrivalTimes[stop] = getScheduledArrivalTime(stop) + delay;
    return this;
  }

  @Nullable
  public Integer getDepartureTime(int stop) {
    return departureTimes[stop];
  }

  public int getScheduledDepartureTime(int stop) {
    return scheduledTripTimes().getScheduledDepartureTime(stop);
  }

  public int[] departureTimes() {
    var result = new int[departureTimes.length];
    for (int i = 0; i < departureTimes.length; i++) {
      if (departureTimes[i] == null) {
        throw new DataValidationException(
          new TimetableValidationError(MISSING_DEPARTURE_TIME, i, getTrip())
        );
      }
      result[i] = departureTimes[i];
    }
    return result;
  }

  /** @return the difference between the scheduled and actual departure times at this stop. */
  @Nullable
  public Integer getDepartureDelay(int stop) {
    if (departureTimes[stop] == null) {
      return null;
    }
    return departureTimes[stop] - getScheduledDepartureTime(stop);
  }

  public RealTimeTripTimesBuilder withDepartureTime(int stop, int time) {
    departureTimes[stop] = time;
    return this;
  }

  public RealTimeTripTimesBuilder withDepartureDelay(int stop, int delay) {
    departureTimes[stop] = getScheduledDepartureTime(stop) + delay;
    return this;
  }

  public BitSet extraCalls() {
    return (BitSet) extraCalls.clone();
  }

  public BitSet hasArrived() {
    return (BitSet) hasArrived.clone();
  }

  public BitSet hasDeparted() {
    return (BitSet) hasDeparted.clone();
  }

  public RealTimeTripTimesBuilder withExtraCall(int stop, boolean extraCall) {
    this.extraCalls.set(stop, extraCall);
    return this;
  }

  public RealTimeTripTimesBuilder withHasArrived(int stop, boolean arrived) {
    if (stop > numberOfStops()) {
      throw new IllegalArgumentException("Stop index out of range");
    }
    this.hasArrived.set(stop, arrived);
    return this;
  }

  public RealTimeTripTimesBuilder withHasDeparted(int stop, boolean departed) {
    if (stop > numberOfStops()) {
      throw new IllegalArgumentException("Stop index out of range");
    }
    this.hasDeparted.set(stop, departed);
    return this;
  }

  @Nullable
  public I18NString tripHeadsign() {
    if (tripHeadsign == null) {
      return scheduledTripTimes.getTripHeadsign();
    }
    return tripHeadsign;
  }

  public RealTimeTripTimesBuilder withTripHeadsign(I18NString headsign) {
    tripHeadsign = headsign;
    return this;
  }

  public @Nullable I18NString[] stopHeadsigns() {
    var result = scheduledTripTimes.copyHeadsigns(() ->
      new I18NString[scheduledTripTimes.getNumStops()]
    );
    for (var i = 0; i < result.length; i++) {
      if (stopHeadsigns[i] != null) {
        result[i] = stopHeadsigns[i];
      }
    }
    return result;
  }

  public RealTimeTripTimesBuilder withStopHeadsign(int stop, I18NString headsign) {
    stopHeadsigns[stop] = headsign;
    return this;
  }

  public Accessibility wheelchairAccessibility() {
    if (wheelchairAccessibility == null) {
      return scheduledTripTimes.getWheelchairAccessibility();
    }
    return wheelchairAccessibility;
  }

  public RealTimeTripTimesBuilder withWheelchairAccessibility(
    Accessibility wheelchairAccessibility
  ) {
    this.wheelchairAccessibility = wheelchairAccessibility;
    return this;
  }

  public RealTimeTripTimesBuilder withServiceCode(int serviceCode) {
    this.scheduledTripTimes = scheduledTripTimes
      .copyOfNoDuplication()
      .withServiceCode(serviceCode)
      .build();
    return this;
  }

  /**
   * Fill in all the missing real times from the scheduled timetable.
   * <p>
   * This does not check for data consistency between the scheduled and real times.
   */
  public boolean copyMissingTimesFromScheduledTimetable() {
    var hasCopiedTimes = false;
    for (var i = 0; i < scheduledTripTimes.getNumStops(); i++) {
      if (arrivalTimes[i] == null) {
        arrivalTimes[i] = getScheduledArrivalTime(i);
        hasCopiedTimes = true;
      }
      if (departureTimes[i] == null) {
        departureTimes[i] = getScheduledDepartureTime(i);
        hasCopiedTimes = true;
      }
    }
    return hasCopiedTimes;
  }

  public TripRealTimeMetadata.TripRealTimeMetadataBuilder realTimeMetadataBuilder() {
    return tripRealTimeMetadataBuilder;
  }

  public RealTimeTripTimes build() {
    return new RealTimeTripTimes(this);
  }
}
