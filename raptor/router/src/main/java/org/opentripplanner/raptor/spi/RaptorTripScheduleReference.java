package org.opentripplanner.raptor.spi;

import java.util.Objects;

/*
 * Holds the route index and trip schedule index for a trip, allowing Raptor to look up route
 * and pattern information in contexts where the original iteration state is no longer available.
 */
public final class RaptorTripScheduleReference {

  private final int routeIndex;
  private final int tripScheduleIndex;

  public RaptorTripScheduleReference(int routeIndex, int tripScheduleIndex) {
    this.routeIndex = routeIndex;
    this.tripScheduleIndex = tripScheduleIndex;
  }

  public int routeIndex() {
    return routeIndex;
  }

  public int tripScheduleIndex() {
    return tripScheduleIndex;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (RaptorTripScheduleReference) o;
    return routeIndex == that.routeIndex && tripScheduleIndex == that.tripScheduleIndex;
  }

  @Override
  public int hashCode() {
    return Objects.hash(routeIndex, tripScheduleIndex);
  }

  @Override
  public String toString() {
    return new StringBuilder()
      .append('(')
      .append("route: ")
      .append(routeIndex)
      .append(", ")
      .append("trip: ")
      .append(tripScheduleIndex)
      .append(')')
      .toString();
  }
}
