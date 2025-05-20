package org.opentripplanner.transit.model.timetable;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.basic.Accessibility;

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

  RealTimeTripTimesBuilder(ScheduledTripTimes scheduledTripTimes) {
    this.scheduledTripTimes = scheduledTripTimes;
    var numStops = scheduledTripTimes.getNumStops();
    arrivalTimes = new Integer[numStops];
    departureTimes = new Integer[numStops];
    stopRealTimeStates = new StopRealTimeState[numStops];
    Arrays.fill(stopRealTimeStates, StopRealTimeState.DEFAULT);
    stopHeadsigns = new I18NString[numStops];
    occupancyStatus = new OccupancyStatus[numStops];
    Arrays.fill(occupancyStatus, OccupancyStatus.NO_DATA_AVAILABLE);
  }

  RealTimeTripTimesBuilder(RealTimeTripTimes original) {
    scheduledTripTimes = original.scheduledTripTimes();
    var numStops = scheduledTripTimes.getNumStops();
    arrivalTimes = new Integer[numStops];
    departureTimes = new Integer[numStops];
    realTimeState = original.getRealTimeState();
    stopRealTimeStates = original.copyStopRealTimeStates();
    tripHeadsign = original.getTripHeadsign();
    stopHeadsigns = original.copyStopHeadsigns();
    occupancyStatus = original.copyOccupancyStatus();
    wheelchairAccessibility = original.getWheelchairAccessibility();
  }

  public ScheduledTripTimes scheduledTripTimes() {
    return scheduledTripTimes;
  }

  public int[] arrivalTimes() {
    // TODO: add propagation
    var result = scheduledTripTimes.copyArrivalTimes();
    for (int i = 0; i < arrivalTimes.length; i++) {
      if (arrivalTimes[i] != null) {
        result[i] = arrivalTimes[i];
      }
    }
    return result;
  }

  public Integer getArrivalTime(int stop) {
    return arrivalTimes[stop];
  }

  /** @return the difference between the scheduled and actual arrival times at this stop. */
  // TODO: handle null arrivalTime
  public int getArrivalDelay(int stop) {
    return arrivalTimes[stop] - scheduledTripTimes.getScheduledArrivalTime(stop);
  }

  public RealTimeTripTimesBuilder withArrivalTime(int stop, int time) {
    updated = true;
    arrivalTimes[stop] = time;
    return this;
  }

  public RealTimeTripTimesBuilder withArrivalDelay(int stop, int delay) {
    updated = true;
    arrivalTimes[stop] = scheduledTripTimes.getScheduledArrivalTime(stop) + delay;
    return this;
  }

  public Integer getDepartureTime(int stop) {
    return departureTimes[stop];
  }

  public int[] departureTimes() {
    // TODO: add propagation
    var result = scheduledTripTimes.copyDepartureTimes();
    for (int i = 0; i < departureTimes.length; i++) {
      if (departureTimes[i] != null) {
        result[i] = departureTimes[i];
      }
    }
    return result;
  }

  /** @return the difference between the scheduled and actual departure times at this stop. */
  public int getDepartureDelay(int stop) {
    return departureTimes[stop] - scheduledTripTimes.getScheduledDepartureTime(stop);
  }

  public RealTimeTripTimesBuilder withDepartureTime(int stop, int time) {
    updated = true;
    departureTimes[stop] = time;
    return this;
  }

  public RealTimeTripTimesBuilder withDepartureDelay(int stop, int delay) {
    updated = true;
    departureTimes[stop] = scheduledTripTimes.getScheduledDepartureTime(stop) + delay;
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

  private RealTimeTripTimesBuilder withStopRealTimeState(int stop, StopRealTimeState state) {
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
   * Note: This method only applies for GTFS, not SIRI!
   * This method interpolates the times for SKIPPED stops in between regular stops since GTFS-RT
   * does not require arrival and departure times for these stops. This method ensures the internal
   * time representations in OTP for SKIPPED stops are between the regular stop times immediately
   * before and after the cancellation in GTFS-RT. This is to meet the OTP requirement that stop
   * times should be increasing and to support the trip search flag `includeRealtimeCancellations`.
   * Terminal stop cancellations can be handled by backward and forward propagations, and are
   * outside the scope of this method.
   *
   * TODO: this interpolation logic is problematic, need to discard and rewrite later
   *
   * @return true if there is interpolated times, false if there is no interpolation.
   */
  public boolean interpolateMissingTimes() {
    copyMissingTimesFromScheduledTimetable();

    boolean hasInterpolatedTimes = false;
    final int numStops = scheduledTripTimes.getNumStops();
    boolean startInterpolate = false;
    boolean hasPrevTimes = false;
    int prevDeparture = 0;
    int prevScheduledDeparture = 0;
    int prevStopIndex = -1;

    // Loop through all stops
    for (int s = 0; s < numStops; s++) {
      final boolean isCancelledStop = stopRealTimeStates[s] == StopRealTimeState.CANCELLED;
      final int scheduledArrival = scheduledTripTimes.getScheduledArrivalTime(s);
      final int scheduledDeparture = scheduledTripTimes.getScheduledDepartureTime(s);
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
          final int scheduledArrivalCancelled = scheduledTripTimes.getScheduledArrivalTime(
            cancelledIndex
          );
          final int scheduledDepartureCancelled = scheduledTripTimes.getScheduledDepartureTime(
            cancelledIndex
          );

          // Interpolate
          int scheduledArrivalDiff = scheduledArrivalCancelled - prevScheduledDeparture;
          double interpolatedArrival = prevDeparture + travelTimeRatio * scheduledArrivalDiff;
          int scheduledDepartureDiff = scheduledDepartureCancelled - prevScheduledDeparture;
          double interpolatedDeparture = prevDeparture + travelTimeRatio * scheduledDepartureDiff;

          // Set Interpolated Times
          withArrivalTime(cancelledIndex, (int) interpolatedArrival);
          withDepartureTime(cancelledIndex, (int) interpolatedDeparture);
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
   * A transitional method to emulate the past behavior of copying ScheduledTripTimes to
   * RealTimeTripTimes. To be removed after the interpolation logic is refactored out.
   */
  private void copyMissingTimesFromScheduledTimetable() {
    for (var i = 0; i < scheduledTripTimes.getNumStops(); i++) {
      if (arrivalTimes[i] == null) {
        arrivalTimes[i] = scheduledTripTimes.getScheduledArrivalTime(i);
      }
      if (departureTimes[i] == null) {
        departureTimes[i] = scheduledTripTimes.getScheduledDepartureTime(i);
      }
    }
  }

  public RealTimeTripTimes build() {
    return new RealTimeTripTimes(this);
  }
}
