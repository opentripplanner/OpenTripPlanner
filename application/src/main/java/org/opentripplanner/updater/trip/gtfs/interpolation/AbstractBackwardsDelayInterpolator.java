package org.opentripplanner.updater.trip.gtfs.interpolation;

import java.util.OptionalInt;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

abstract class AbstractBackwardsDelayInterpolator implements BackwardsDelayInterpolator {

  /**
   * Propagate backwards from the first stop with real-time information. This includes propagating
   * the delay from the departure time to the same stop's arrival time if it's missing.
   *
   * @return The first stop position with given time if propagation is done. Empty if the first
   *         stop already has an arrival time, or if no stop has real-time times to propagate from.
   */
  public OptionalInt propagateBackwards(RealTimeTripTimesBuilder builder) {
    for (int i = 0; i < builder.numberOfStops(); i++) {
      if (!builder.containsNoRealTimeTimes(i)) {
        // Don't need to do anything if the first stop has an updated arrival time already
        if (i == 0 && builder.getArrivalTime(0) != null) {
          return OptionalInt.empty();
        }
        fillInMissingTimes(builder, i);
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

  protected abstract void fillInMissingTimes(
    RealTimeTripTimesBuilder builder,
    int firstUpdatedIndex
  );
}
