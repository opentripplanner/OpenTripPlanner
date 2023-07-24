package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;

public class ViaRideFactory<T extends RaptorTripSchedule>
  implements PatternRideFactory<T, PatternRideC2<T>> {

  @Override
  public PatternRideC2<T> createPatternRide(
    McStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardCost1,
    int relativeCost1,
    T trip
  ) {
    return new PatternRideC2<>(
      prevArrival,
      boardStopIndex,
      boardPos,
      boardTime,
      boardCost1,
      relativeCost1,
      prevArrival.c2(),
      trip.tripSortIndex(),
      trip
    );
  }

  @Override
  public PatternRideC2<T> createPatternRide(PatternRideC2<T> ride, int c2) {
    return new PatternRideC2<>(
      ride.prevArrival(),
      ride.boardStopIndex(),
      ride.boardPos(),
      ride.boardTime(),
      ride.boardC1(),
      ride.relativeC1(),
      c2,
      ride.tripSortIndex(),
      ride.trip()
    );
  }
}
