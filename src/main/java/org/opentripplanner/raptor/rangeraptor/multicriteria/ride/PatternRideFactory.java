package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;

@FunctionalInterface
public interface PatternRideFactory<T extends RaptorTripSchedule, R extends PatternRide<T>> {
  R createPatternRide(
    McStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardCost1,
    int relativeCost1,
    T trip,
    // TODO: 2023-05-15 via pass through: this is temporary. We need to figure out how to inject
    //  c2 value for via stops inside ride

    int c2
  );

  /**
   * This method is called for each pattern before boarding. It allows the factory
   * to compute and cache values for each pattern, which can be used when creating
   * rides. This optimization make sure the pattern is accesses once - before
   * potentially hundreds of boardings.
   */
  default void prepareForTransitWith(RaptorTripPattern pattern) {}
}
