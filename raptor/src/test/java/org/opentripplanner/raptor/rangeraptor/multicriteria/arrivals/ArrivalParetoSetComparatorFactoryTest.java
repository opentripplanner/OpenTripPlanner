package org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.PathLegType;
import org.opentripplanner.raptor.api.model.RelaxFunction;

class ArrivalParetoSetComparatorFactoryTest {

  private static final int STOP = 9;
  private static final boolean ARRIVED_ON_BOARD = true;
  private static final boolean ARRIVED_ON_FOOT = false;
  private static final int C1_100 = 100;
  private static final int C1_777 = 777;
  private static final int PARETO_ROUND_ONE = 1;
  private static final int PARETO_ROUND_TWO = 2;
  private static final int ARRIVAL_TIME_EARLY = 12;
  private static final int ARRIVAL_TIME_LATE = 13;

  private static final ArrivalParetoSetComparatorFactory<A> comparatorC1 =
    ArrivalParetoSetComparatorFactory.factory(RelaxFunction.NORMAL, null);

  private static final ArrivalParetoSetComparatorFactory<A> comparatorC1AndC2 =
    ArrivalParetoSetComparatorFactory.factory(RelaxFunction.NORMAL, (left, right) -> left > right);

  @Test
  void compareArrivalTimeRoundAndCost() {
    // Same values for arrival-time, pareto-round and c1. Ignore c2 and arrivedOnBoard
    assertFalse(
      comparatorC1
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_777, ARRIVED_ON_FOOT)
        )
    );
    // Arrival-time is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_777, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Pareto-round is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_777, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // C1 is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_777, C1_100, ARRIVED_ON_BOARD)
        )
    );
  }

  @Test
  void compareArrivalTimeRoundAndCostWithC2() {
    // Same values for arrival-time, pareto-round and c1. Ignore c2 and arrivedOnBoard
    assertFalse(
      comparatorC1AndC2
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_FOOT)
        )
    );
    // Arrival-time is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_777, C1_100, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Pareto-round is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_777, C1_100, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // C1 is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_777, C1_100, ARRIVED_ON_BOARD)
        )
    );

    // C2 is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
  }

  @Test
  void compareArrivalTimeRoundCostAndOnBoardArrivalWithC2() {
    // Same values for arrival-time, pareto-round and c1. Ignore c2 and arrivedOnBoard
    assertFalse(
      comparatorC1AndC2
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Arrival-time is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_777, C1_100, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Pareto-round is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_ONE, C1_777, C1_100, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // C1 is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, C1_100, C1_100, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_777, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Arrived on-board is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, C1_777, C1_100, ARRIVED_ON_BOARD),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_FOOT)
        )
    );
    // C2 is better
    assertTrue(
      comparatorC1AndC2
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, C1_100, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
  }

  @Test
  void compareArrivalTimeRoundCostAndOnBoardArrival() {
    // Same values for arrival-time, pareto-round and c1. Ignore c2 and arrivedOnBoard
    assertFalse(
      comparatorC1
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_777, ARRIVED_ON_BOARD)
        )
    );
    // Arrival-time is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_777, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Pareto-round is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_ONE, C1_777, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_TWO, C1_100, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // C1 is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, C1_100, C1_777, ARRIVED_ON_FOOT),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_777, C1_100, ARRIVED_ON_BOARD)
        )
    );
    // Arrived on-board is better
    assertTrue(
      comparatorC1
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, C1_777, C1_777, ARRIVED_ON_BOARD),
          new A(ARRIVAL_TIME_EARLY, PARETO_ROUND_ONE, C1_100, C1_100, ARRIVED_ON_FOOT)
        )
    );
  }

  @Test
  void compareRelaxedC1Test() {
    int bestC1 = 600;
    int okC1 = 799;
    int rejectC1 = okC1 + 1;
    var relaxC1 = GeneralizedCostRelaxFunction.of(1.0, 200);
    var referenceArrival = new A(
      ARRIVAL_TIME_EARLY,
      PARETO_ROUND_ONE,
      bestC1,
      C1_100,
      ARRIVED_ON_BOARD
    );

    var subject = ArrivalParetoSetComparatorFactory.factory(relaxC1, null);

    assertFalse(
      subject
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, rejectC1, C1_777, ARRIVED_ON_FOOT),
          referenceArrival
        )
    );
    assertTrue(
      subject
        .compareArrivalTimeRoundAndCost()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, okC1, C1_777, ARRIVED_ON_FOOT),
          referenceArrival
        )
    );

    // Test OnBoardArrival
    assertFalse(
      subject
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, rejectC1, C1_777, ARRIVED_ON_FOOT),
          referenceArrival
        )
    );
    assertTrue(
      subject
        .compareArrivalTimeRoundCostAndOnBoardArrival()
        .leftDominanceExist(
          new A(ARRIVAL_TIME_LATE, PARETO_ROUND_TWO, okC1, C1_777, ARRIVED_ON_FOOT),
          referenceArrival
        )
    );
  }

  private static class A extends McStopArrival<TestTripSchedule> {

    int c2;
    boolean arrivedOnBoard;

    public A(int arrivalTime, int paretoRound, int c1, int c2, boolean arrivedOnBoard) {
      super(STOP, 0, arrivalTime, c1, paretoRound);
      this.c2 = c2;
      this.arrivedOnBoard = arrivedOnBoard;
    }

    @Override
    public int c2() {
      return c2;
    }

    @Override
    public PathLegType arrivedBy() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean arrivedOnBoard() {
      return arrivedOnBoard;
    }

    @Override
    public McStopArrival<TestTripSchedule> addSlackToArrivalTime(int slack) {
      throw new UnsupportedOperationException();
    }
  }
}
