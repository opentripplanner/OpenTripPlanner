package org.opentripplanner.ext.flex;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;

public final class FlexAccessEgress {

  private final RegularStop stop;
  private final FlexPathDurations pathDurations;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final FlexTrip trip;
  private final State lastState;
  private final boolean stopReachedOnBoard;

  public FlexAccessEgress(
    RegularStop stop,
    FlexPathDurations pathDurations,
    int fromStopIndex,
    int toStopIndex,
    FlexTrip trip,
    State lastState,
    boolean stopReachedOnBoard
  ) {
    this.stop = stop;
    this.pathDurations = pathDurations;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.trip = trip;
    this.lastState = lastState;
    this.stopReachedOnBoard = stopReachedOnBoard;
  }

  public RegularStop stop() {
    return stop;
  }

  public FlexTrip trip() {
    return trip;
  }

  public State lastState() {
    return lastState;
  }

  public boolean stopReachedOnBoard() {
    return stopReachedOnBoard;
  }

  public int earliestDepartureTime(int departureTime) {
    int requestedDepartureTime = pathDurations.mapToFlexTripDepartureTime(departureTime);
    int earliestDepartureTime = trip.earliestDepartureTime(
      requestedDepartureTime,
      fromStopIndex,
      toStopIndex,
      pathDurations.trip()
    );
    if (earliestDepartureTime == MISSING_VALUE) {
      return MISSING_VALUE;
    }
    return pathDurations.mapToRouterDepartureTime(earliestDepartureTime);
  }

  public int latestArrivalTime(int arrivalTime) {
    int requestedArrivalTime = pathDurations.mapToFlexTripArrivalTime(arrivalTime);
    int latestArrivalTime = trip.latestArrivalTime(
      requestedArrivalTime,
      fromStopIndex,
      toStopIndex,
      pathDurations.trip()
    );
    if (latestArrivalTime == MISSING_VALUE) {
      return MISSING_VALUE;
    }
    return pathDurations.mapToRouterArrivalTime(latestArrivalTime);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(FlexAccessEgress.class)
      .addNum("fromStopIndex", fromStopIndex)
      .addNum("toStopIndex", toStopIndex)
      .addObj("durations", pathDurations)
      .addObj("stop", stop)
      .addObj("trip", trip)
      .addObj("lastState", lastState)
      .addBoolIfTrue("stopReachedOnBoard", stopReachedOnBoard)
      .toString();
  }
}
