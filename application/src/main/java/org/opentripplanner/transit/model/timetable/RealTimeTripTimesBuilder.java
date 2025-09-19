package org.opentripplanner.transit.model.timetable;

import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.MISSING_ARRIVAL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.MISSING_DEPARTURE_TIME;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DataValidationException;

public class RealTimeTripTimesBuilder {

  private ScheduledTripTimes scheduledTripTimes;
  private final Integer[] arrivalTimes;
  private final Integer[] departureTimes;

  @Nullable
  private RealTimeState realTimeState;

  private final StopRealTimeState[] stopRealTimeStates;

  @Nullable
  private I18NString tripHeadsign;

  private final I18NString[] stopHeadsigns;
  private final OccupancyStatus[] occupancyStatus;

  @Nullable
  private Accessibility wheelchairAccessibility;

  private boolean updated;

  /**
   * This constructor take a ScheduledTripTimes (not base TripTimes) to enforce creating a new
   * RealTimeTripTimes based on the scheduled info. RT updates are  NOT cumulative and this
   * enforce copying the scheduled information, not the previous real-time update.
   * <p>
   * The arrival and departure times are left uninitialized by this constructor, and they need to
   * be set explicitly.
   */
  RealTimeTripTimesBuilder(ScheduledTripTimes tripTimes) {
    this.scheduledTripTimes = tripTimes;
    var numStops = tripTimes.getNumStops();
    arrivalTimes = new Integer[numStops];
    departureTimes = new Integer[numStops];
    stopRealTimeStates = new StopRealTimeState[numStops];
    Arrays.fill(stopRealTimeStates, StopRealTimeState.DEFAULT);
    stopHeadsigns = new I18NString[numStops];
    occupancyStatus = new OccupancyStatus[numStops];
    Arrays.fill(occupancyStatus, OccupancyStatus.NO_DATA_AVAILABLE);
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
    updated = true;
    arrivalTimes[stop] = time;
    return this;
  }

  public RealTimeTripTimesBuilder withArrivalDelay(int stop, int delay) {
    updated = true;
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
    updated = true;
    departureTimes[stop] = time;
    return this;
  }

  public RealTimeTripTimesBuilder withDepartureDelay(int stop, int delay) {
    updated = true;
    departureTimes[stop] = getScheduledDepartureTime(stop) + delay;
    return this;
  }

  public RealTimeState realTimeState() {
    if (realTimeState == null) {
      return updated ? RealTimeState.UPDATED : RealTimeState.SCHEDULED;
    }
    return realTimeState;
  }

  public RealTimeTripTimesBuilder withRealTimeState(RealTimeState realTimeState) {
    this.realTimeState = realTimeState;
    return this;
  }

  public RealTimeTripTimesBuilder cancelTrip() {
    return withRealTimeState(RealTimeState.CANCELED);
  }

  public RealTimeTripTimesBuilder deleteTrip() {
    return withRealTimeState(RealTimeState.DELETED);
  }

  public StopRealTimeState getStopRealTimeState(int stop) {
    return stopRealTimeStates[stop];
  }

  public StopRealTimeState[] stopRealTimeStates() {
    return stopRealTimeStates.clone();
  }

  public RealTimeTripTimesBuilder withRecorded(int stop) {
    return withStopRealTimeState(stop, StopRealTimeState.RECORDED);
  }

  public RealTimeTripTimesBuilder withCanceled(int stop) {
    return withStopRealTimeState(stop, StopRealTimeState.CANCELLED);
  }

  public RealTimeTripTimesBuilder withNoData(int stop) {
    return withStopRealTimeState(stop, StopRealTimeState.NO_DATA);
  }

  public RealTimeTripTimesBuilder withInaccuratePredictions(int stop) {
    return withStopRealTimeState(stop, StopRealTimeState.INACCURATE_PREDICTIONS);
  }

  public RealTimeTripTimesBuilder withStopRealTimeState(int stop, StopRealTimeState state) {
    this.stopRealTimeStates[stop] = state;
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

  public OccupancyStatus[] occupancyStatus() {
    return occupancyStatus.clone();
  }

  public RealTimeTripTimesBuilder withOccupancyStatus(int stop, OccupancyStatus occupancyStatus) {
    this.occupancyStatus[stop] = occupancyStatus;
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

  public RealTimeTripTimes build() {
    return new RealTimeTripTimes(this);
  }
}
