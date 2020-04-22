package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

import static org.junit.Assert.assertEquals;

public class CostCalculatorTest {

    private static final int BOARD_COST = 5;
    private static final double WALK_RELUCTANCE_FACTOR = 2.0;
    private static final double WAIT_RELUCTANCE_FACTOR = 0.5;

    private LifeCycleSubscriptions lifeCycleSubscriptions = new LifeCycleSubscriptions();


    private CostCalculator subject = new DefaultCostCalculator(
            new int[] { 0, 25 },
            BOARD_COST,
            WALK_RELUCTANCE_FACTOR,
            WAIT_RELUCTANCE_FACTOR,
            lifeCycleSubscriptions
    );

    @Test
    public void transitArrivalCost() {
        LifeCycleEventPublisher lifeCycle = new LifeCycleEventPublisher(lifeCycleSubscriptions);
        assertEquals("Board cost", 500, subject.transitArrivalCost(0, 0, 0, 0));
        assertEquals("Transit + board cost", 600, subject.transitArrivalCost(0, 1, 0, 0));
        // There is no wait cost for the first transit leg
        assertEquals("Board cost", 500, subject.transitArrivalCost(1, 0, 0, 0));

        // Simulate round 2
        lifeCycle.prepareForNextRound(2);
        // There is a small cost (2-1) * 0.5 * 100 = 50 added for the second transit leg
        assertEquals("Wait + board cost", 550, subject.transitArrivalCost(1, 0, 0, 0));
        assertEquals("wait + board + transit", 750, subject.transitArrivalCost(1, 2, 0, 0));
    }

    @Test
    public void walkCost() {
        assertEquals(200, subject.walkCost(1));
        assertEquals(600, subject.walkCost(3));
    }

    @Test
    public void calculateMinCost() {
        // Board cost is 500, then add:
        assertEquals(500, subject.calculateMinCost(0, 0));
        assertEquals(600, subject.calculateMinCost(1, 0));
        assertEquals(1000, subject.calculateMinCost(0, 1));

        // Expect:  precision * (minTravelTime + (minNumTransfers + 1) * boardCost)
        // =>  100 * ( 200 + (3+1) * 5)
        assertEquals(22_000, subject.calculateMinCost(200, 3));
    }

    @Test
    public void testConvertBetweenRaptorAndMainOtpDomainModel() {
        assertEquals(
            BOARD_COST,
            RaptorCostConverter.toOtpDomainCost(subject.calculateMinCost(0,0))
        );
        assertEquals(
            3 + BOARD_COST,
            RaptorCostConverter.toOtpDomainCost(subject.calculateMinCost(3,0))
        );
    }
}