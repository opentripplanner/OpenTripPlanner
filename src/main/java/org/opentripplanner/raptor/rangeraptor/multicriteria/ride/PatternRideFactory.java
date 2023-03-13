package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

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
    T trip
  );
}
