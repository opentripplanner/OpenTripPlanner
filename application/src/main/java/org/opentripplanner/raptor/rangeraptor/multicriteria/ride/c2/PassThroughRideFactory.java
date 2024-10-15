package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2;

import org.opentripplanner.framework.lang.IntBox;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.internalapi.PassThroughPointsService;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;

public class PassThroughRideFactory<T extends RaptorTripSchedule>
  implements PatternRideFactory<T, PatternRideC2<T>> {

  private final PassThroughPointsService passThroughPointsService;

  public PassThroughRideFactory(PassThroughPointsService passThroughPointsService) {
    this.passThroughPointsService = passThroughPointsService;
  }

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
      calculateC2(prevArrival),
      trip.tripSortIndex(),
      trip
    );
  }

  /**
   * We need to update the c2 value if the board stop is a pass-through stop; Raptor only update
   * the c2 value for "existing" rides. There is no need to check if the current stop is a
   * pass-through stop, because the {@code passThroughPoints} service is stateful and already
   * knows if the current stop is a pass-through stop.
   */
  private int calculateC2(McStopArrival<T> prevArrival) {
    IntBox c2 = new IntBox(prevArrival.c2());
    passThroughPointsService.updateC2Value(c2.get(), c2::set);
    return c2.get();
  }
}
