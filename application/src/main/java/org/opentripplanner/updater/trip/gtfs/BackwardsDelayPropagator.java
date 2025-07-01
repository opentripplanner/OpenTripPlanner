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
      case ALWAYS -> new BackwardsDelayAlwaysPropagator();
      case REQUIRED, REQUIRED_NO_DATA -> new BackwardsDelayRequiredPropagator(backwardsDelayPropagationType == BackwardsDelayPropagationType.REQUIRED_NO_DATA);
    };
  }
}
