package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.utils.time.TimeUtils;

class ArrivalParetoSetComparatorFactoryTest implements RaptorTestConstants {

  private static final DominanceFunction DOMINANCE_FUNCTION = (left, right) -> left < right;
  private static final RelaxFunction RELAX_FUNCTION_PLUS_10 = v -> v + 10;
  private static final int DEPATURE_TIME = TimeUtils.time("10:00");

  /// INPUT CRITERIA
  ///
  /// This tests test the pareto-comparator produced by the factory. The 5 first colums are
  /// input (criteria to compare) with a given value(same as thefirst row).
  ///
  /// - `arrivalTime` : Arrive early is better
  /// - ´round´ : The second column is the Raptor round, but it is the pareto-round which is used
  ///   in the pareto-set compare function.
  /// - `c1` : Lower c1 is better
  /// - `c2` : Lower c2 is better or used to relax c1
  /// - `arriveOnBoard` : arrive-on-board is better then arrive-on-street. It allowes you to
  ///    continue with a transfer
  ///
  /// EXPECTED
  ///
  /// The las column is
  /// the expected result. The factory can create 4 variants of the factory with 2
  /// pareto-comparators in each (with and without arrive-on-board `...]` or `..., arriveOnBoard]`):
  /// - With c1 `[arrivalTime, round, c1, ...`
  /// - With c1 & c2 `[arrivalTime, round, c1, c2, ...`
  /// - With c2 enable relaxed c1(+10) `[arrivalTime, round, c2 ? c1(+10) : c1, ...`
  ///
  /// For each of the 6 pareto-function variations we determine the dominance
  /// `NONE | LEFFT | RIGHT | BOTH`. The last column consist a string with the expected result
  ///  for these 6 variants.
  ///
  ///
  @ParameterizedTest
  @CsvSource(
    value = {
      // Case #1 - All criteria equals
      "10:10 | 2 | 100 | 500 | false | NONE  NONE  - NONE  NONE  - NONE  NONE",
      // Case #2-6 - Single criteria better
      "10:09 | 2 | 100 | 500 | false | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:10 | 1 | 100 | 500 | false | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:10 | 2 |  97 | 500 | false | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:10 | 2 | 100 | 499 | false | NONE  NONE  - LEFT  LEFT  - LEFT  LEFT",
      "10:10 | 2 | 100 | 500 | true  | NONE  LEFT  - NONE  LEFT  - NONE  LEFT",
      // Case #7-9 - arrivalPath-time & round
      "10:09 | 1 | 100 | 500 | false | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:09 | 3 | 100 | 500 | false | BOTH  BOTH  - BOTH  BOTH  - BOTH  BOTH",
      "10:11 | 1 | 100 | 500 | false | BOTH  BOTH  - BOTH  BOTH  - BOTH  BOTH",
      // Case #10-12 - arrivalPath-time & c1
      "10:09 | 2 |  99 | 500 | false | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:11 | 2 |  99 | 500 | false | BOTH  BOTH  - BOTH  BOTH  - BOTH  BOTH",
      "10:09 | 2 | 101 | 500 | false | BOTH  BOTH  - BOTH  BOTH  - BOTH  BOTH",
      // Case #13-15 arrivalPath-time & c2
      "10:09 | 2 | 100 | 499 | false | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:11 | 2 | 100 | 499 | false | RIGHT RIGHT - BOTH  BOTH  - BOTH  BOTH",
      "10:09 | 2 | 100 | 501 | false | LEFT  LEFT  - BOTH  BOTH  - BOTH  BOTH",
      // Case #16-17 arrivalPath-time & on-board
      "10:09 | 2 | 100 | 500 | true  | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:11 | 2 | 100 | 500 | true  | RIGHT BOTH  - RIGHT BOTH  - RIGHT BOTH",
      // Case #18-19  ride & on-board
      "10:10 | 1 | 100 | 500 | true  | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:10 | 3 | 100 | 500 | true  | RIGHT BOTH  - RIGHT BOTH  - RIGHT BOTH",
      // Case #20-21 - c1 & on-board
      "10:10 | 2 |  99 | 500 | true  | LEFT  LEFT  - LEFT  LEFT  - LEFT  LEFT",
      "10:10 | 2 | 101 | 500 | true  | RIGHT BOTH  - RIGHT BOTH  - RIGHT BOTH",
      // Case #22-25 - c1 & c2
      "10:10 | 2 | 102 | 500 | false | RIGHT RIGHT - RIGHT RIGHT - RIGHT RIGHT",
      "10:10 | 2 | 103 | 500 | false | RIGHT RIGHT - RIGHT RIGHT - RIGHT RIGHT",
      "10:10 | 2 | 109 | 499 | false | RIGHT RIGHT - BOTH  BOTH  - BOTH  BOTH",
      "10:10 | 2 | 110 | 499 | false | RIGHT RIGHT - BOTH  BOTH  - RIGHT RIGHT",
      // Case #26-31 - c1, c2 & on-board
      "10:10 | 2 | 100 | 500 | true  | NONE  LEFT  - NONE  LEFT  - NONE  LEFT",
      "10:10 | 2 | 102 | 500 | true  | RIGHT BOTH  - RIGHT BOTH  - RIGHT BOTH",
      "10:10 | 2 | 103 | 500 | true  | RIGHT BOTH  - RIGHT BOTH  - RIGHT BOTH",
      "10:10 | 2 | 109 | 499 | true  | RIGHT BOTH  - BOTH  BOTH  - BOTH  BOTH",
      "10:10 | 2 | 109 | 500 | true  | RIGHT BOTH  - RIGHT BOTH  - RIGHT BOTH",
      "10:10 | 2 | 110 | 499 | true  | RIGHT BOTH  - BOTH  BOTH  - RIGHT BOTH",
    },
    delimiter = '|'
  )
  void testCompareArrivalTimeRoundAndCost(
    String arrivalTime,
    int round,
    int c1,
    int c2,
    boolean arriveOnBoard,
    String expexted
  ) {
    var left = createStopArrival(arrivalTime, round, c1, c2, arriveOnBoard);
    var right = createStopArrival("10:10", 2, 100, 500, false);

    var result = new StringBuilder();
    var factories = List.of(
      ArrivalParetoSetComparatorFactory.ofCompareC1(),
      ArrivalParetoSetComparatorFactory.ofCompareC1AndC2(DOMINANCE_FUNCTION),
      ArrivalParetoSetComparatorFactory.ofCompareC1RelaxedOnC2Dominance(
        RELAX_FUNCTION_PLUS_10,
        DOMINANCE_FUNCTION
      )
    );
    for (var factory : factories) {
      result.append(" - ").append(toStr(factory, left, right));
    }
    assertEquals(expexted, result.toString().substring(3).trim());
  }

  static String toStr(
    ArrivalParetoSetComparatorFactory factory,
    McStopArrival<?> left,
    McStopArrival<?> right
  ) {
    return "%-5s %-5s".formatted(
      Dominance.from(factory.compareArrivalTimeRoundAndCost(), left, right),
      Dominance.from(factory.compareArrivalTimeRoundCostAndOnBoardArrival(), left, right)
    );
  }

  private static McStopArrival<TestTripSchedule> createStopArrival(
    String arrivalTime,
    int round,
    int c1,
    int c2,
    boolean arrivedOnBoard
  ) {
    int arrTime = TimeUtils.time(arrivalTime);
    return TestStopArivalFactory.arrivalPath(DEPATURE_TIME, arrTime, round, c1, c2, arrivedOnBoard);
  }

  private static enum Dominance {
    LEFT,
    RIGHT,
    BOTH,
    NONE;

    static Dominance from(
      ParetoComparator<McStopArrival<?>> comp,
      McStopArrival<?> left,
      McStopArrival<?> right
    ) {
      if (comp.leftDominanceExist(left, right)) {
        return comp.leftDominanceExist(right, left) ? Dominance.BOTH : Dominance.LEFT;
      } else {
        return comp.leftDominanceExist(right, left) ? Dominance.RIGHT : Dominance.NONE;
      }
    }
  }
}
