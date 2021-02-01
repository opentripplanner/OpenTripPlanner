package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.Access;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorCostConverter;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleEventPublisher;
import org.opentripplanner.transit.raptor.rangeraptor.workerlifecycle.LifeCycleSubscriptions;

import static org.junit.Assert.assertEquals;

public class CostCalculatorTest {

    private static final int BOARD_COST = 5;
    private static final int TRANSFER_COST = 2;
    private static final double WALK_RELUCTANCE_FACTOR = 2.0;
    private static final double WAIT_RELUCTANCE_FACTOR = 0.5;

    private final LifeCycleSubscriptions lifeCycleSubscriptions = new LifeCycleSubscriptions();

    private final CostCalculator<TestTripSchedule> subject = new DefaultCostCalculator<>(
            new int[] { 0, 25 },
            BOARD_COST,
            TRANSFER_COST,
            WALK_RELUCTANCE_FACTOR,
            WAIT_RELUCTANCE_FACTOR,
            lifeCycleSubscriptions
    );

    private final LifeCycleEventPublisher lifeCycle = new LifeCycleEventPublisher(lifeCycleSubscriptions);

    @Before
    public void setup() {
        lifeCycle.prepareForNextRound(0);
    }

    @Test
    public void transitArrivalCost() {
        int fromStop = 0;
        ArrivalView<TestTripSchedule> prev = new Access(0, 0, 2);

        lifeCycle.prepareForNextRound(0);
        assertEquals(1000, prev.cost());

        lifeCycle.prepareForNextRound(1);
        assertEquals("Board cost", 500, subject.transitArrivalCost(fromStop, 0, 0, 0, null));
        assertEquals("Board + transit cost", 600, subject.transitArrivalCost(fromStop, 0, 1, 0, null));
        assertEquals("Board + transit + stop cost", 625, subject.transitArrivalCost(fromStop, 0, 1, 1, null));
        // There is no wait cost for the first transit leg
        assertEquals("Board cost", 500, subject.transitArrivalCost(fromStop, 1, 0, 0, null));

        // Simulate round 2
        lifeCycle.prepareForNextRound(2);
        // There is a small cost (2-1) * 0.5 * 100 = 50 added for the second transit leg
        assertEquals("Wait + board cost", 750, subject.transitArrivalCost(fromStop,1, 0, 0, null));
        assertEquals("wait + board + transit", 950, subject.transitArrivalCost(fromStop, 1, 2, 0, null));
    }

    @Test
    public void walkCost() {
        assertEquals(200, subject.walkCost(1));
        assertEquals(600, subject.walkCost(3));
    }

    @Test
    public void onTripRidingCost() {
        ArrivalView<TestTripSchedule> prev = new Access(0, 0, 2);

        lifeCycle.prepareForNextRound(0);
        assertEquals(1000, prev.cost());

        lifeCycle.prepareForNextRound(1);
        assertEquals("Board cost", 500, subject.onTripRidingCost(prev, 0, 5));
        // No extra cost added for the first boarding, assume the access can be time-shifted
        assertEquals("Board cost", 500, subject.onTripRidingCost(prev, 2, 5));

        lifeCycle.prepareForNextRound(2);
        assertEquals("Board cost", 500, subject.onTripRidingCost(prev, 0, 5));
        // 2 * 100 /2 = 100 (cost for waiting) is added
        assertEquals("Board cost", 600, subject.onTripRidingCost(prev, 2, 5));
    }

    @Test
    public void calculateMinCost() {
        // Board cost is 500, transfer cost is 200 then add:
        assertEquals(500, subject.calculateMinCost(0, 0));
        assertEquals(600, subject.calculateMinCost(1, 0));
        assertEquals(1200, subject.calculateMinCost(0, 1));

        // Expect:  precision * (minTravelTime + (minNumTransfers + 1) * boardCost)
        // =>  100 * ( 200 + (3+1) * 5)
        assertEquals(22_600, subject.calculateMinCost(200, 3));
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