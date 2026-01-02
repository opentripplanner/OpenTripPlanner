package org.opentripplanner.updater.trip.gtfs;

import java.util.OptionalInt;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;

abstract class AbstractBackwardsDelayInterpolator implements BackwardsDelayInterpolator {

  /**
   * Propagate backwards from the first stop with real-time information
   * @return The first stop position with given time if propagation is done.
   */
  public OptionalInt propagateBackwards(RealTimeTripTimesBuilder builder) {
    var firstUpdatedIndex = getFirstUpdatedIndex(builder);
    // if the first stop already has a real-time update, there is nothing to propagate
    if (firstUpdatedIndex == 0) {
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
