package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

class BackwardsDelayRequiredPropagator implements BackwardsDelayPropagator {
  private final boolean setNoData;

  BackwardsDelayRequiredPropagator(boolean setNoData) {
    this.setNoData = setNoData;
  }

  @Override
  public boolean adjustTimes(RealTimeTripTimesBuilder builder, int firstUpdatedIndex) {
    if (
      builder.getArrivalTime(firstUpdatedIndex) > builder.getDepartureTime(firstUpdatedIndex)
    ) {
      // The given trip update has arrival time after departure time for the first updated stop.
      // This method doesn't try to fix issues in the given data, only for the missing part
      return false;
    }
    int nextStopArrivalTime = builder.getArrivalTime(firstUpdatedIndex);
    int delay = builder.getArrivalDelay(firstUpdatedIndex);
    boolean hasAdjustedTimes = false;
    boolean adjustTimes = true;
    for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
      if (setNoData && builder.stopRealTimeStates()[i] != StopRealTimeState.CANCELLED) {
        builder.withNoData(i);
      }
      if (adjustTimes) {
        if (builder.getDepartureTime(i) < nextStopArrivalTime) {
          adjustTimes = false;
          continue;
        } else {
          hasAdjustedTimes = true;
          builder.withDepartureDelay(i, delay);
        }
        if (builder.getArrivalTime(i) < builder.getDepartureTime(i)) {
          adjustTimes = false;
        } else {
          builder.withArrivalDelay(i, delay);
          nextStopArrivalTime = builder.getArrivalTime(i);
        }
      }
    }
    return hasAdjustedTimes;
  }
}
