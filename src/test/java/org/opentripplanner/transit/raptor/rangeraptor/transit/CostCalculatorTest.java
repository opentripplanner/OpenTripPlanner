package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

import static org.junit.Assert.assertEquals;

public class CostCalculatorTest {

    private static final int BOARD_COST = 5;
    private static final int BOARD_SLACK_IN_SECONDS = 3;
    private static final double WALK_RELUCTANCE_FACTOR = 2.0;
    private static final double WAIT_RELUCTANCE_FACTOR = 0.5;

    private LifeCycleSubscriptions lifeCycleSubscriptions = new LifeCycleSubscriptions();


    private CostCalculator subject = new CostCalculator(
            BOARD_COST,
            BOARD_SLACK_IN_SECONDS,
            WALK_RELUCTANCE_FACTOR,
            WAIT_RELUCTANCE_FACTOR,
            lifeCycleSubscriptions
    );

    @Test
    public void transitArrivalCost() {
        LifeCycleEventPublisher lifeCycle = new LifeCycleEventPublisher(lifeCycleSubscriptions);
        assertEquals("Board cost", 500, subject.transitArrivalCost(1, 1, 1));
        assertEquals("Transit + board cost", 600, subject.transitArrivalCost(1, 1, 2));
        // There is no wait cost for the first transit leg
        assertEquals("Board cost", 500, subject.transitArrivalCost(1, 2, 2));

        // Simulate round 2
        lifeCycle.prepareForNextRound(2);
        // There is a small cost (2-1) * 0.5 * 100 = 50 added for the second transit leg
        assertEquals("Wait + board cost", 550, subject.transitArrivalCost(1, 2, 2));
        assertEquals("wait + board + transit", 750, subject.transitArrivalCost(1, 2, 4));
    }

    @Test
    public void walkCost() {
        assertEquals(200, subject.walkCost(1));
        assertEquals(600, subject.walkCost(3));
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

    @Test
    public void testConvertBetweenRaptorAndMainOtpDomainModel() {
        assertEquals(0, CostCalculator.toOtpDomainCost(49));
        assertEquals(1, CostCalculator.toOtpDomainCost(50));
        assertEquals(300, CostCalculator.toOtpDomainCost(30_000));
        assertEquals(5, CostCalculator.toOtpDomainCost(subject.calculateMinCost(5,0)));
    }
}