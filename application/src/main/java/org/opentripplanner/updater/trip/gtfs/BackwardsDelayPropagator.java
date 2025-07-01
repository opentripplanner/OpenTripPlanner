package org.opentripplanner.updater.trip.gtfs;

import java.util.OptionalInt;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

interface BackwardsDelayPropagator {
  /**
   * Propagate backwards from the first stop with real-time information
   * @return The first stop position with given time if propagation is done.
   */
  OptionalInt propagateBackwards(RealTimeTripTimesBuilder builder);

  static BackwardsDelayPropagator getBackwardsDelayPropagator(
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    return switch (backwardsDelayPropagationType) {
      case NONE -> builder -> OptionalInt.empty();
      case ALWAYS -> new BackwardsDelayAlwaysPropagator();
      case REQUIRED, REQUIRED_NO_DATA -> new BackwardsDelayRequiredPropagator(
        backwardsDelayPropagationType == BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
    };
  }
}
