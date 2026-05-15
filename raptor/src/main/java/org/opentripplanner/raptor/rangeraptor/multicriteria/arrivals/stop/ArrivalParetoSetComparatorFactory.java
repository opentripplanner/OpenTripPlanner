package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;

/**
 * This class creates and hold two {@code ParetoComparator}s which is used in a single McRaptor
 * search.
 *
 * The creation of the two {@link ParetoComparator}s should be done in such way that the JIT
 * compiler can inline all lamdas for the best possible performance. Changes this this class
 * should be checked with the SpeedTest to avoid degration in performance.
 */
public final class ArrivalParetoSetComparatorFactory<T extends McStopArrival<?>> {

  private final ParetoComparator<T> compareRegularStopArrivals;
  private final ParetoComparator<T> compareStopArrivalsIncludingOnBoardCriteria;

  public ArrivalParetoSetComparatorFactory(ParetoComparator<T> compareArrivalTimeRoundAndC1) {
    this.compareRegularStopArrivals = compareArrivalTimeRoundAndC1;
    this.compareStopArrivalsIncludingOnBoardCriteria = compareFunctionWithArrivedOnBoard(
      compareArrivalTimeRoundAndC1
    );
  }

  public static <T extends McStopArrival<?>> ArrivalParetoSetComparatorFactory<T> ofCompareC1() {
    return new ArrivalParetoSetComparatorFactory<>((l, r) -> compareC1(l, r));
  }

  public static <T extends McStopArrival<?>> ArrivalParetoSetComparatorFactory<T> ofCompareC1AndC2(
    final DominanceFunction c2DominanceFunction
  ) {
    return new ArrivalParetoSetComparatorFactory<>((l, r) ->
      compareC1AndC2(c2DominanceFunction, l, r)
    );
  }

  public static <T extends McStopArrival<?>> ArrivalParetoSetComparatorFactory<
    T
  > ofCompareC1RelaxedOnC2Dominance(
    final RelaxFunction relaxC1,
    final DominanceFunction c2DominanceFunction
  ) {
    return new ArrivalParetoSetComparatorFactory<>((l, r) ->
      compareC1RelaxedOnC2Dominance(relaxC1, c2DominanceFunction, l, r)
    );
  }

  /**
   * This comparator is used to compareFunction regular stop arrivals.
   *  It uses {@code arrivalTime}, {@code paretoRound} and {@code c1} to compareFunction arrivals. It
   *  does NOT include {@code arrivedOnBoard}. Normally arriving on-board should give the
   *  arrival an advantage - you can continue on foot, walking to the next stop. But, we only
   *  do this if it happens in the same Raptor iteration and round - if it does, it is taken
   *  care of by the order which the algorithm works - not by this comparator.
   */
  public ParetoComparator<T> compareArrivalTimeRoundAndCost() {
    return compareRegularStopArrivals;
  }

  /**
   * This includes {@code arrivedOnBoard} in the comparison compared with
   * {@link #compareArrivalTimeRoundAndCost()}.
   */
  public ParetoComparator<T> compareArrivalTimeRoundCostAndOnBoardArrival() {
    return compareStopArrivalsIncludingOnBoardCriteria;
  }

  /* private methods */

  private static <T extends McStopArrival<?>> ParetoComparator<T> compareFunctionWithArrivedOnBoard(
    ParetoComparator<T> compareArrivalTimeRoundAndC1
  ) {
    return (l, r) ->
      compareArrivalTimeRoundAndC1.leftDominanceExist(l, r) || compareArrivedOnBoard(l, r);
  }

  /**
   * Compare arrivalTime, paretoRound and c1.
   */
  private static <T extends McStopArrival<?>> boolean compareC1(T l, T r) {
    // This is important with respect to performance. Using the short-circuit logical OR(||) is
    // faster than bitwise inclusive OR(|) (even between boolean expressions)
    return (l.arrivalTime() < r.arrivalTime() || l.round() < r.round() || l.c1() < r.c1());
  }

  /**
   * Compare arrivalTime, paretoRound, c1, and c21.
   */
  private static <T extends McStopArrival<?>> boolean compareC1AndC2(
    DominanceFunction c2Function,
    T l,
    T r
  ) {
    return (
      l.arrivalTime() < r.arrivalTime() ||
      l.round() < r.round() ||
      l.c1() < r.c1() ||
      c2Function.leftDominateRight(l.c2(), r.c2())
    );
  }

  /**
   * Compare arrivalTime, paretoRound and c1, relaxing c1.
   */
  private static <T extends McStopArrival<?>> boolean compareC1Relaxed(
    final RelaxFunction relaxC1,
    T l,
    T r
  ) {
    return (
      l.arrivalTime() < r.arrivalTime() || l.round() < r.round() || l.c1() < relaxC1.relax(r.c1())
    );
  }

  /**
   * Compare arrivalTime, paretoRound, and c1 relaxed if c2 dominates.
   */
  private static <T extends McStopArrival<?>> boolean compareC1RelaxedOnC2Dominance(
    final RelaxFunction relaxC1,
    DominanceFunction c2Function,
    T l,
    T r
  ) {
    return c2Function.leftDominateRight(l.c2(), r.c2())
      ? compareC1Relaxed(relaxC1, l, r)
      : compareC1(l, r);
  }

  /**
   * Compare arrivedOnBoard. On-board arrival dominate arrive by transfer(foot) since
   * you can continue on foot; hence has more options.
   */
  private static <T extends McStopArrival<?>> boolean compareArrivedOnBoard(T l, T r) {
    return l.arrivedOnBoard() && !r.arrivedOnBoard();
  }
}
