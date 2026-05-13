package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A {@link AbstractPatternRide} with support for c1 {@code generalized-cost}.
 */
public class PatternRideC1<T extends RaptorTripSchedule> extends AbstractPatternRide<T> {

  // Pareto vector: [relativeCost, tripSortIndex]

  public PatternRideC1(
    McStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardC1,
    int relativeC1,
    int tripSortIndex,
    T trip
  ) {
    super(
      prevArrival,
      boardStopIndex,
      boardPos,
      boardTime,
      boardC1,
      relativeC1,
      tripSortIndex,
      trip
    );
  }

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

  public static <T extends RaptorTripSchedule> ParetoComparator<
    PatternRideC1<T>
  > paretoComparatorRelativeCost() {
    return (l, r) -> l.compareArrivalTime(r) || l.compareC1(r);
  }

  @Override
  public int c2() {
    return RaptorCostCalculator.ZERO_COST;
  }

  @Override
  public String toString() {
    return toString(ToStringBuilder.of(PatternRideC1.class), null);
  }
}
