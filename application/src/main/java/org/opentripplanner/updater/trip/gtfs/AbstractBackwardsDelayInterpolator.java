package org.opentripplanner.updater.trip.gtfs;

import java.util.OptionalInt;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

abstract class AbstractBackwardsDelayInterpolator implements BackwardsDelayInterpolator {

  /**
   * Propagate backwards from the first stop with real-time information. This includes propagating
   * the delay from the departure time to the same stop's arrival time if it's missing.
   *
   * @return The first stop position with given time if propagation is done.
   */
  public OptionalInt propagateBackwards(RealTimeTripTimesBuilder builder) {
    var firstUpdatedIndex = getFirstUpdatedIndex(builder);
    // Don't need to do anything if the first stop has an updated arrival time already
    if (firstUpdatedIndex == 0 && builder.getArrivalTime(0) != null) {
      return OptionalInt.empty();
    }
    fillInMissingTimes(builder, firstUpdatedIndex);
    return OptionalInt.of(firstUpdatedIndex);
  }

  protected int getFirstUpdatedIndex(RealTimeTripTimesBuilder builder) {
    var firstUpdatedIndex = 0;
    while (builder.containsNoRealTimeTimes(firstUpdatedIndex)) {
      ++firstUpdatedIndex;
      if (firstUpdatedIndex == builder.numberOfStops()) {
        throw new IllegalArgumentException(
          "No real-time updates exist in the builder, can't propagate backwards."
        );
      }
    }
    return firstUpdatedIndex;
  }

  protected abstract void fillInMissingTimes(
    RealTimeTripTimesBuilder builder,
    int firstUpdatedIndex
  );
}
