package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CostCalculatorTest {

    private static final int BOARD_COST = 5;
    private static final int BOARD_SLACK_IN_SECONDS = 3;
    private static final double WALK_RELUCTANCE_FACTOR = 2.0;
    private static final double WAIT_RELUCTANCE_FACTOR = 0.5;

    private static final int T1 = 1;
    private static final int T2 = 2;
    private static final int T3 = 3;
    private static final int T4 = 4;

    private CostCalculator subject = new CostCalculator(
            BOARD_COST,
            BOARD_SLACK_IN_SECONDS,
            WALK_RELUCTANCE_FACTOR,
            WAIT_RELUCTANCE_FACTOR
    );

    @Test
    public void transitArrivalCost() {
        assertEquals("Cost board cost", 500, subject.transitArrivalCost(T1, T1, T1));
        assertEquals("Cost transit + board cost", 600, subject.transitArrivalCost(T1, T1, T2));
        assertEquals("Cost wait + board", 550, subject.transitArrivalCost(T1, T2, T2));
        assertEquals("wait + board + transit", 750, subject.transitArrivalCost(T1, T2, T4));
    }

    @Test
    public void walkCost() {
        assertEquals(200, subject.walkCost(T1));
        assertEquals(600, subject.walkCost(T3));
    }

    @Test
    public void calculateMinCost() {
        assertEquals(0, subject.calculateMinCost(0, 0));
        assertEquals(100, subject.calculateMinCost(1, 0));
        assertEquals(650, subject.calculateMinCost(0, 1));

        // Expect:  precision * (minTravelTime + minNumTransfers * (boardCost + boardSlack * waitReluctance)
        // =>  100 * ( 200 + 3 * (5 + 3 * 0.5))
        assertEquals(21_950, subject.calculateMinCost(200, 3));
    }
}