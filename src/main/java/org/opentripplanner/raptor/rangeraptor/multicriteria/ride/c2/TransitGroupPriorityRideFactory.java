package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2;

import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupCalculator;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;

/**
 * This factory creates new {@link PatternRide}s and merge in transit-group-priority ids
 * into c2.
 */
public class TransitGroupPriorityRideFactory<T extends RaptorTripSchedule>
  implements PatternRideFactory<T, PatternRideC2<T>> {

  private int currentPatternGroupPriority;
  private final RaptorTransitGroupCalculator transitGroupPriorityCalculator;

  public TransitGroupPriorityRideFactory(
    RaptorTransitGroupCalculator transitGroupPriorityCalculator
  ) {
    this.transitGroupPriorityCalculator = transitGroupPriorityCalculator;
  }

  @Override
  public PatternRideC2<T> createPatternRide(
    McStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardCost1,
    int relativeC1,
    T trip
  ) {
    return new PatternRideC2<>(
      prevArrival,
      boardStopIndex,
      boardPos,
      boardTime,
      boardCost1,
      relativeC1,
      calculateC2(prevArrival.c2()),
      trip.tripSortIndex(),
      trip
    );
  }

  @Override
  public void prepareForTransitWith(RaptorTripPattern pattern) {
    this.currentPatternGroupPriority = pattern.priorityGroupId();
  }

  /**
   * Currently transit-group-priority is the only usage of c2
   */
  private int calculateC2(int c2) {
    return transitGroupPriorityCalculator.mergeGroupIds(c2, currentPatternGroupPriority);
  }
}
