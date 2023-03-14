package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c2;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * Abstract super class for multi-criteria stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
abstract class AbstractStopArrivalC2<T extends RaptorTripSchedule> extends McStopArrival<T> {

  private final int c2;

  /**
   * Transit or transfer.
   *
   * @param previous             the previous arrival visited for the current trip
   * @param paretoRoundIncrement the increment to add to the paretoRound
   * @param stop                 stop index for this arrival
   * @param arrivalTime          the arrival time for this stop index
   * @param c1                   first criteria, the total accumulated generalized-cost-1 criteria
   * @param c2                   second criteria, the total accumulated generalized-cost-2 criteria
   */
  AbstractStopArrivalC2(
    McStopArrival<T> previous,
    int paretoRoundIncrement,
    int stop,
    int arrivalTime,
    int c1,
    int c2
  ) {
    super(previous, paretoRoundIncrement, stop, arrivalTime, c1);
    this.c2 = c2;
  }

  /**
   * Initial state - first stop visited during the RAPTOR algorithm.
   */
  AbstractStopArrivalC2(
    int stop,
    int departureTime,
    int travelDuration,
    int paretoRound,
    int c1,
    int c2
  ) {
    super(stop, departureTime, travelDuration, c1, paretoRound);
    this.c2 = c2;
  }

  /**
   * TODO C2 - DOC
   *
   * This comparator is used to compare regular stop arrivals. It uses {@code arrivalTime},
   * {@code paretoRound} and {@code generalizedCost} to compare arrivals. It does NOT include
   * {@code arrivedOnBoard}. Normally arriving on-board should give the arrival an advantage
   * - you can continue on foot, walking to the next stop. But, we only do this if it happens
   * in the same Raptor iteration and round - if it does it is taken care of by the order
   * witch the algorithm work - not by this comparator.
   */
  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<AbstractStopArrivalC2<T>> compareArrivalTimeRoundC1AndC2(
    RelaxFunction relaxArrivalTime,
    RelaxFunction relaxC1,
    DominanceFunction dominanceFunctionC2
  ) {
    // If c2 dominates, then a slack is added to arrival-time and cost (c1)
    return (l, r) ->
      dominanceFunctionC2.leftDominateRight(l.c2, r.c2)
        ? relaxedCompareBase(relaxArrivalTime, relaxC1, l, r)
        : compareBase(l, r);
  }

  /**
   * This include {@code arrivedOnBoard} in the comparison compared with
   * {@link #compareArrivalTimeRoundAndCost()}.
   */
  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<AbstractStopArrivalC2<T>> compareArrivalTimeRoundC1C2AndOnBoardArrival(
    RelaxFunction relaxArrivalTime,
    RelaxFunction relaxC1,
    DominanceFunction dominanceFunctionC2
  ) {
    return (l, r) ->
      (
        dominanceFunctionC2.leftDominateRight(l.c2, r.c2)
          ? relaxedCompareBase(relaxArrivalTime, relaxC1, l, r)
          : compareBase(l, r)
      ) ||
      compareArrivedOnBoard(l, r);
  }

  public final int c2() {
    return c2;
  }

  @Override
  public String toString() {
    return asString();
  }
}
