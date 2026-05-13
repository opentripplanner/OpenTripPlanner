package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.McStopArrival;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A {@link AbstractPatternRide} with support for c1 {@code generalized-cost} and c2 (custom-use-case-cost).
 */
public class PatternRideC2<T extends RaptorTripSchedule> extends AbstractPatternRide<T> {

  private final int c2;

  public PatternRideC2(
    McStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardC1,
    int relativeC1,
    int c2,
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
    this.c2 = c2;
  }

  public static <T extends RaptorTripSchedule> ParetoComparator<
    PatternRideC2<T>
  > comparatorRelaxedC1IfC2IsOptimal(RelaxFunction relaxC1, DominanceFunction dominanceFunctionC2) {
    return (l, r) ->
      l.compareArrivalTime(r) ||
      (dominanceFunctionC2.leftDominateRight(l.c2, r.c2)
        ? l.compareC1(relaxC1, r)
        : l.compareC1(r));
  }

  public int c2() {
    return c2;
  }

  @Override
  public String toString() {
    return toString(ToStringBuilder.of(PatternRideC2.class), b -> b.addNum("c2", c2));
  }
}
