package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

public interface PatternRideFactory<T extends RaptorTripSchedule, R extends PatternRide<T>> {
  R createPatternRide(
    McStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardCost1,
    int relativeCost1,
    T trip
  );

  /**
   * This method is called for each pattern before boarding. It allows the factory
   * to compute and cache values for each pattern, which can be used when creating
   * rides. This optimization make sure the pattern is accesses once - before
   * potentially hundreds of boardings.
   */
  default void prepareForTransitWith(RaptorTripPattern pattern) {}
}
