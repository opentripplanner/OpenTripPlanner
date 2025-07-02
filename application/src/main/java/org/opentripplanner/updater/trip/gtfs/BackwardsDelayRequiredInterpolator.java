package org.opentripplanner.updater.trip.gtfs;

import java.util.Objects;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.StopRealTimeState;

/**
 * This class fills in missing times before the first updated time directly from the scheduled
 * time, propagating negative delay only when necessary to keep the trip times non-decreasing.
 */
class BackwardsDelayRequiredInterpolator extends BackwardsDelayInterpolatorBase {

  /**
   * If true, updated stops are set NO_DATA and not exposed in APIs.
   */
  private final boolean setNoData;

  BackwardsDelayRequiredInterpolator(boolean setNoData) {
    this.setNoData = setNoData;
  }

  @Override
  protected void fillInMissingTimes(RealTimeTripTimesBuilder builder, int firstUpdatedIndex) {
    while (
      builder.getArrivalDelay(firstUpdatedIndex) == null &&
      builder.getDepartureDelay(firstUpdatedIndex) == null
    ) {
      ++firstUpdatedIndex;
    }
    var time = builder.getArrivalTime(firstUpdatedIndex);
    if (time == null) {
      builder.withArrivalTime(
        firstUpdatedIndex,
        time = Math.min(
          Objects.requireNonNull(builder.getDepartureTime(firstUpdatedIndex)),
          builder.getScheduledArrivalTime(firstUpdatedIndex)
        )
      );
    }
    for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
      if (setNoData && builder.stopRealTimeStates()[i] != StopRealTimeState.CANCELLED) {
        builder.withNoData(i);
      }
      builder.withDepartureTime(i, time = Math.min(time, builder.getScheduledDepartureTime(i)));
      builder.withArrivalTime(i, time = Math.min(time, builder.getScheduledArrivalTime(i)));
    }
  }
}
