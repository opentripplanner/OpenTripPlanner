package org.opentripplanner.transit.raptor.api.transit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.Access;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;

public class CostCalculatorTest {

    private static final int BOARD_COST = 5;
    private static final int TRANSFER_COST = 2;
    private static final double WALK_RELUCTANCE_FACTOR = 2.0;
    private static final double WAIT_RELUCTANCE_FACTOR = 0.5;
    private static final double TRANSIT_RELUCTANCE_FACTOR_1 = 1.0;
    private static final double TRANSIT_RELUCTANCE_FACTOR_2 = 0.8;
    private static final int TRANSIT_RELUCTANCE_1 = 0;
    private static final int TRANSIT_RELUCTANCE_2 = 1;

    private final CostCalculator<TestTripSchedule> subject = new DefaultCostCalculator<>(
            BOARD_COST,
            TRANSFER_COST,
            WALK_RELUCTANCE_FACTOR,
            WAIT_RELUCTANCE_FACTOR,
            new int[] { 0, 25 },
            new double[] { TRANSIT_RELUCTANCE_FACTOR_1, TRANSIT_RELUCTANCE_FACTOR_2 }
    );

    @Test
    public void transitArrivalCost() {
        int fromStop = 0;
        ArrivalView<TestTripSchedule> prev = new Access(0, 0, 2, 1000);

        assertEquals(1000, prev.cost());

        // Simulate round 1
        assertEquals("Board cost", 500, subject.transitArrivalCost(true, fromStop, 0, 0, 0, TRANSIT_RELUCTANCE_1));
        assertEquals("Board + transit cost", 600, subject.transitArrivalCost(true, fromStop, 0, 1, 0, TRANSIT_RELUCTANCE_1));
        assertEquals("Board + transit + stop cost", 625, subject.transitArrivalCost(true, fromStop, 0, 1, TRANSIT_RELUCTANCE_1, 1));
        assertEquals("Board + wait cost", 550, subject.transitArrivalCost(true, fromStop, 1, 0, TRANSIT_RELUCTANCE_1, 0));

        // Simulate round 2
        // There is a small cost (2-1) * 0.5 * 100 = 50 added for the second transit leg
        assertEquals("Wait + board cost", 750, subject.transitArrivalCost(false, fromStop,1, 0, TRANSIT_RELUCTANCE_1, 0));
        assertEquals("wait + board + transit", 950, subject.transitArrivalCost(false, fromStop, 1, 2, TRANSIT_RELUCTANCE_1, 0));
    }

    @Test
    public void walkCost() {
        assertEquals(200, subject.walkCost(1));
        assertEquals(600, subject.walkCost(3));
    }

    @Test
    public void onTripRidingCost() {
        ArrivalView<TestTripSchedule> prev = new Access(0, 0, 2, 1000);

        assertEquals(1000, prev.cost());
        assertEquals("Board cost", 500, subject.onTripRidingCost(prev, 0, 5, TRANSIT_RELUCTANCE_1));
        assertEquals("Board cost", 600, subject.onTripRidingCost(prev, 0, 5, TRANSIT_RELUCTANCE_2));
        assertEquals("Board cost", 550, subject.onTripRidingCost(prev, 1, 5, TRANSIT_RELUCTANCE_1));
        assertEquals("Board cost", 650, subject.onTripRidingCost(prev, 1, 5, TRANSIT_RELUCTANCE_2));
    }

    @Test
    public void calculateMinCost() {
        // Given:
        //   - Board cost:     500
        //   - Transfer cost:  200
        //   - Transit factor:  80 (min of 80 and 100)

        // Board cost is 500:
        assertEquals(500, subject.calculateMinCost(0, 0));
        // The transfer 1s * 80 = 80 + board cost 500
        assertEquals(580, subject.calculateMinCost(1, 0));
        // Board 2 times and transfer 1: 2 * 500 + 200
        assertEquals(1200, subject.calculateMinCost(0, 1));

        // Transit 200s * 80 + Board 4 * 500 + Transfer 3 * 200
        assertEquals(18_600, subject.calculateMinCost(200, 3));
    }

    @Test
    public void testConvertBetweenRaptorAndMainOtpDomainModel() {
        assertEquals(
            BOARD_COST,
            RaptorCostConverter.toOtpDomainCost(subject.calculateMinCost(0,0))
        );
        assertEquals(
            20 * 4/5 + BOARD_COST,
            RaptorCostConverter.toOtpDomainCost(subject.calculateMinCost(20,0))
        );
    }
}