package org.opentripplanner.routing.algorithm.transferoptimization.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider.defaultSlackProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptor.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.view.BoardAndAlightTime;
import org.opentripplanner.util.time.TimeUtils;


public class TransitPathLegSelectorTest implements RaptorTestConstants {

    private static final RaptorSlackProvider SLACK_PROVIDER = defaultSlackProvider(
            TRANSFER_SLACK, BOARD_SLACK, ALIGHT_SLACK
    );

    private static final CostCalculator COST_CALCULATOR = new DefaultCostCalculator(
            20, 60, 1.0, null, null
    );

    public static final MinCostFilterChain<OptimizedPathTail<TestTripSchedule>>
            FILTER_CHAIN = new MinCostFilterChain<>(List.of(OptimizedPathTail::generalizedCost));

    private final int T10_00 = TimeUtils.time("10:00");
    private final int T10_20 = TimeUtils.time("10:20");
    private final int T10_40 = TimeUtils.time("10:40");

    private final OptimizedPathTail<TestTripSchedule> pathTail = new OptimizedPathTail<>(
            SLACK_PROVIDER, COST_CALCULATOR, null, this::stopIndexToName
    );

    private final TestTripSchedule TRIP = TestTripSchedule.schedule()
            .pattern("L1", STOP_A, STOP_C, STOP_E)
            .times(T10_00, T10_20, T10_40).build();

    private final int EGRESS_START = T10_40 + D1m;
    private final int EGRESS_END = EGRESS_START + D5m;

    @Test
    public void testEmptySetDoesReturnEmtySet() {
        var subject = new TransitPathLegSelector<>(FILTER_CHAIN, Set.of());
        assertEquals(Set.of(), subject.next(0));
    }

    @Test
    public void testOneElementIsReturnedIfTimeLimitThresholdIsPassed() {
        var leg = pathTail.addTransitTail(transitLeg(STOP_E));

        var subject = new TransitPathLegSelector<>(FILTER_CHAIN, Set.of(leg));

        var result = subject.next(T10_40);
        assertTrue(result.isEmpty(), result.toString());

        result = subject.next(T10_40-1);
        assertFalse(result.isEmpty(), result.toString());
    }

    @Test
    public void testTwoPathLegs() {
        var leg1 = pathTail.mutate().addTransitTail(transitLeg(STOP_E));
        var leg2 = pathTail.mutate().addTransitTail(transitLeg(STOP_C));

        var subject = new TransitPathLegSelector<>(FILTER_CHAIN, Set.of(leg1, leg2));

        var result = subject.next(T10_40);
        assertTrue(result.isEmpty(), result.toString());

        result = subject.next(T10_40-1);
        assertEquals("BUS L1 10:00 10:40", firstRide(result));
        assertEquals(result.size(), 1);

        // No change yet
        result = subject.next(T10_20);
        assertEquals("BUS L1 10:00 10:40", firstRide(result));
        assertEquals(result.size(), 1);

        // Get next
        result = subject.next(T10_20-1);
        assertEquals("BUS L1 10:00 10:20", firstRide(result));
        assertEquals(result.size(), 1);

        // Same as previous
        result = subject.next(0);
        assertEquals("BUS L1 10:00 10:20", firstRide(result));
        assertEquals(result.size(), 1);
    }

    private TransitPathLeg<TestTripSchedule> transitLeg(int egressStop) {
        TestTransfer walk = walk(egressStop, EGRESS_END - EGRESS_START);
        var egress = new EgressPathLeg<TestTripSchedule>(
                walk, EGRESS_START, EGRESS_END, walk.generalizedCost()
        );
        int toTime = TRIP.arrival(TRIP.findArrivalStopPosition(Integer.MAX_VALUE, egressStop));
        var times = BoardAndAlightTime.create(TRIP, STOP_A, T10_00, egressStop, toTime);
        int cost = 100 * (T10_40 - T10_00);
        return new TransitPathLeg<>(TRIP, times, null, cost, egress);
    }

    private static  <T> String firstRide(Collection<T> c) {
        return c.stream()
                .map(Object::toString)
                .map(it -> it.substring(0, it.indexOf(" ~")))
                .collect(Collectors.joining(" "));
    }

}