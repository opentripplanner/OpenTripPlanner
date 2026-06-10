package org.opentripplanner.raptor.api.model;

import java.util.Objects;

/**
 * This class contains information to identify a given stop in a stop pattern for a given trip
 * schedule in a route. This can for example be used to identify where a bording or alighting
 * happens.
 */
@SuppressWarnings("ClassCanBeRecord")
public final class RaptorTripScheduleStopPosition {

  private final int routeIndex;
  private final int tripScheduleIndex;
  private final int stopPositionInPattern;

  public RaptorTripScheduleStopPosition(
    int routeIndex,
    int tripScheduleIndex,
    int stopPositionInPattern
  ) {
    this.routeIndex = routeIndex;
    this.tripScheduleIndex = tripScheduleIndex;
    this.stopPositionInPattern = stopPositionInPattern;
  }

  /// The index of the boarded route
  public int routeIndex() {
    return routeIndex;
  }

  /// The index of the boarded trip within the route
  public int tripScheduleIndex() {
    return tripScheduleIndex;
  }

  /// The stop position in the route stop-pattern. Knowing the stop index is not enough to identify
  /// a stop, a stop may occour multiple times in a stop pattern, in other words a circular stop
  /// pattern visits some of the same stops multiple times.
  public int stopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (RaptorTripScheduleStopPosition) o;
    return (
      routeIndex == that.routeIndex &&
      tripScheduleIndex == that.tripScheduleIndex &&
      stopPositionInPattern == that.stopPositionInPattern
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(routeIndex, tripScheduleIndex, stopPositionInPattern);
  }

  @Override
  public String toString() {
    // The field labels are shortened to improve reading - should be easy to get in the context
    return (
      "[route: " +
      routeIndex +
      ", trip: " +
      tripScheduleIndex +
      ", pos: " +
      stopPositionInPattern +
      "]"
    );
  }
}
