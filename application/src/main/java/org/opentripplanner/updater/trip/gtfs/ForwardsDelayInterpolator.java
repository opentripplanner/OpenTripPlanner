package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

interface ForwardsDelayInterpolator {
  /**
   * Interpolate delays forwards from the first given real-time information
   * @return If propagation is done
   */
  boolean interpolateDelay(RealTimeTripTimesBuilder builder);

  static ForwardsDelayInterpolator getInstance(
    ForwardsDelayPropagationType forwardsDelayPropagationType
  ) {
    return switch (forwardsDelayPropagationType) {
      case NONE -> builder -> false;
      case DEFAULT -> new DefaultForwardsDelayInterpolator();
    };
  }
}
