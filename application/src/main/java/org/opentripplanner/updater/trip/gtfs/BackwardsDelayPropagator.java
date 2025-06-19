package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

public interface BackwardsDelayPropagator {
  boolean adjustTimes(RealTimeTripTimesBuilder realTimeTripTimesBuilder, int firstUpdatedIndex);

  /**
   * Adjusts arrival time for the stop at the firstUpdatedIndex if no update was given for it and
   * arrival/departure times for the stops before that stop. Returns {@code true} if times have been
   * adjusted.
   */
  static BackwardsDelayPropagator getBackwardsDelayPropagator(
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    return switch (backwardsDelayPropagationType) {
      case ALWAYS -> (builder, firstUpdatedIndex) -> {
        boolean hasAdjustedTimes = false;
        int delay = builder.getDepartureDelay(firstUpdatedIndex);
        if (builder.getArrivalDelay(firstUpdatedIndex) == 0) {
          builder.withArrivalDelay(firstUpdatedIndex, delay);
          hasAdjustedTimes = true;
        }
        delay = builder.getArrivalDelay(firstUpdatedIndex);
        if (delay == 0) {
          return false;
        }
        for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
          hasAdjustedTimes = true;
          builder.withDepartureDelay(i, delay);
          builder.withArrivalDelay(i, delay);
        }
        return hasAdjustedTimes;
      };
      case REQUIRED, REQUIRED_NO_DATA -> (builder, firstUpdatedIndex) -> {
        var setNoData =
          backwardsDelayPropagationType == BackwardsDelayPropagationType.REQUIRED_NO_DATA;
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
      };
    };
  }
}
