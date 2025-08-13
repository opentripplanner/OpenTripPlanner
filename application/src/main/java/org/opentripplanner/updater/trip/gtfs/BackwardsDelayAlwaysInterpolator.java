package org.opentripplanner.updater.trip.gtfs;

import java.util.Objects;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

/**
 * This class fills in missing times before the first updated time by assuming that the vehicle
 * kept the same delay all the way from the starting stop to the first updated stop.
 */
class BackwardsDelayAlwaysInterpolator extends AbstractBackwardsDelayInterpolator {

  @Override
  protected void fillInMissingTimes(RealTimeTripTimesBuilder builder, int firstUpdatedIndex) {
    var delay = builder.getArrivalDelay(firstUpdatedIndex);
    if (delay == null) {
      delay = Objects.requireNonNull(builder.getDepartureDelay(firstUpdatedIndex));
      builder.withArrivalDelay(firstUpdatedIndex, delay);
    }
    for (int i = firstUpdatedIndex - 1; i >= 0; i--) {
      builder.withDepartureDelay(i, delay);
      builder.withArrivalDelay(i, delay);
    }
  }
}
