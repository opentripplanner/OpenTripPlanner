package org.opentripplanner.updater.trip.gtfs;

import java.util.OptionalInt;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

public interface BackwardsDelayPropagator {
  /**
   * Fill in missing times before the first given time in the real-time update.
   * @return The first stop position with given time if propagation is done.
   */
  OptionalInt propagateBackwards(RealTimeTripTimesBuilder builder);

  static BackwardsDelayPropagator getBackwardsDelayPropagator(
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    return switch (backwardsDelayPropagationType) {
      case ALWAYS -> new BackwardsDelayAlwaysPropagator();
      case REQUIRED, REQUIRED_NO_DATA -> new BackwardsDelayRequiredPropagator(
        backwardsDelayPropagationType == BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
    };
  }
}
