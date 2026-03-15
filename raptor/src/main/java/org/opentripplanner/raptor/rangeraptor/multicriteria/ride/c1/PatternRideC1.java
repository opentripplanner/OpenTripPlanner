package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A {@link PatternRide} with support for c1 {@code generalized-cost}.
 */
public record PatternRideC1<T extends RaptorTripSchedule>(
  McStopArrival<T> prevArrival,
  int boardStopIndex,
  int boardPos,
  int boardTime,
  int boardC1,
  int relativeC1,
  int tripSortIndex,
  T trip
) implements PatternRide<T> {
  // Pareto vector: [relativeCost, tripSortIndex]

  public static <T extends RaptorTripSchedule> PatternRideFactory<T, PatternRideC1<T>> factory() {
    return new PatternRideFactory<T, PatternRideC1<T>>() {
      @Override
      public PatternRideC1<T> createPatternRide(
        McStopArrival<T> prevArrival,
        int boardStopIndex,
        int boardPos,
        int boardTime,
        int boardCost1,
        int relativeCost1,
        T trip
      ) {
        return new PatternRideC1<>(
          prevArrival,
          boardStopIndex,
          boardPos,
          boardTime,
          boardCost1,
          relativeCost1,
          trip.tripSortIndex(),
          trip
        );
      }
    };
  }

  /**
   * See {@link PatternRide} for the pareto comparison strategy used by this comparator.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<
    PatternRideC1<T>
  > paretoComparatorRelativeCost() {
    return (l, r) -> l.tripSortIndex < r.tripSortIndex || l.relativeC1 < r.relativeC1;
  }

  @Override
  public int c2() {
    return RaptorCostCalculator.ZERO_COST;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(PatternRideC1.class)
      .addNum("prevArrival", prevArrival.stop())
      .addNum("boardStop", boardStopIndex)
      .addNum("boardPos", boardPos)
      .addServiceTime("boardTime", boardTime)
      .addNum("boardC1", boardC1)
      .addNum("relativeC1", relativeC1)
      .addNum("tripSortIndex", tripSortIndex)
      .addObj("trip", trip)
      .toString();
  }

  @Override
  public PatternRide<T> updateC2(int newC2) {
    return this;
  }
}
