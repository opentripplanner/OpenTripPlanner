package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.junit.Assert;
import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestLeg;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AccessStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.TransferStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.TransitStopArrival;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;

import java.util.Arrays;

import static org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator.testDummyCalculator;

public class StopArrivalStateParetoSetTest {
    // 08:35 in seconds
    private static final int A_TIME = ((8 * 60) + 35) * 60;
    private static final int ANY = 3;
    private static final int ROUND_1 = 1;
    private static final int ROUND_2 = 2;
    private static final int ROUND_3 = 3;
    private static final RaptorTripSchedule ANY_TRIP = null;
    private static final TransitCalculator CALCULATOR = testDummyCalculator(true);

    // In this test each stop is used to identify the pareto vector - it is just one
    // ParetoSet "subject" with multiple "stops" in it. The stop have no effect on
    // the Pareto functionality.
    private static final int STOP_1 = 1;
    private static final int STOP_2 = 2;
    private static final int STOP_3 = 3;
    private static final int STOP_4 = 4;
    private static final int STOP_5 = 5;
    private static final int STOP_6 = 6;

    private static final AbstractStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL = newAccessStopState(999, 10);
    private static final AbstractStopArrival<RaptorTripSchedule> TRANSFER_R1 = newMcTransitStopState(ROUND_1,998, 10);
    private static final AbstractStopArrival<RaptorTripSchedule> TRANSFER_R2 = newMcTransitStopState(ROUND_2,997, 20);

    private final StopArrivalParetoSet<RaptorTripSchedule> subject = new StopArrivalParetoSet<>(null);

    @Test
    public void addOneElementToSet() {
        subject.add(newAccessStopState(STOP_1, 10));
        assertStopsInSet(STOP_1);
    }

    @Test
    public void testTimeDominance() {
        subject.add(newAccessStopState(STOP_1, 10));
        subject.add(newAccessStopState(STOP_2, 9));
        subject.add(newAccessStopState(STOP_3, 9));
        subject.add(newAccessStopState(STOP_4, 11));
        assertStopsInSet(STOP_2);
    }

    @Test
    public void testRoundDominance() {
        subject.add(newTransferStopState(ROUND_1, STOP_1, 10, ANY));
        subject.add(newTransferStopState(ROUND_2, STOP_2, 10, ANY));
        assertStopsInSet(STOP_1);
    }


    @Test
    public void testCostDominance() {
        subject.add(newTransferStopState(ROUND_1, STOP_1, ANY, 20));
        subject.add(newTransferStopState(ROUND_1, STOP_2, ANY, 10));
        assertStopsInSet(STOP_2);
    }

    @Test
    public void testRoundAndTimeDominance() {
        subject.add(newTransferStopState(ROUND_1, STOP_1, 10, ANY));
        subject.add(newTransferStopState(ROUND_1, STOP_2, 8, ANY));

        assertStopsInSet(STOP_2);

        subject.add(newTransferStopState(ROUND_2, STOP_3, 8, ANY));

        assertStopsInSet(STOP_2);

        subject.add(newTransferStopState(ROUND_2, STOP_4, 7, ANY));

        assertStopsInSet(STOP_2, STOP_4);

        subject.add(newTransferStopState(ROUND_3, STOP_5, 6, ANY));

        assertStopsInSet(STOP_2, STOP_4, STOP_5);

        subject.add(newTransferStopState(ROUND_3, STOP_6, 6, ANY));

        assertStopsInSet(STOP_2, STOP_4, STOP_5);
    }

    /**
     * During the same round transfers should not dominate transits, but this is handled
     * by the worker state (2-phase transfer calculation), not by the pareto-set. Using
     * the pareto-set for this would cause unnecessary exploration in the following round.
     */
    @Test
    public void testTransitAndTransferDoesNotAffectDominance() {
        subject.add(newAccessStopState(STOP_1, 20));
        subject.add(newMcTransitStopState(ROUND_1, STOP_2, 10));
        subject.add(newTransferStopState(ROUND_1, STOP_4, 8, 0));
        assertStopsInSet(STOP_1, STOP_4);
    }

    private void assertStopsInSet(int ... expStopIndexes) {
        int[] result = subject.stream().mapToInt(AbstractStopArrival::stop).sorted().toArray();
        Assert.assertEquals("Stop indexes", Arrays.toString(expStopIndexes), Arrays.toString(result));
    }

    private static AccessStopArrival<RaptorTripSchedule> newAccessStopState(int stop, int accessDurationInSeconds) {
        return new AccessStopArrival<>(stop, A_TIME, accessDurationInSeconds, ANY, null);
    }

    private static TransitStopArrival<RaptorTripSchedule> newMcTransitStopState(int round, int stop, int arrivalTime) {
        return new TransitStopArrival<>(prev(round), stop, arrivalTime, ANY, ANY_TRIP, ANY, ANY);
    }

    private static TransferStopArrival<RaptorTripSchedule> newTransferStopState(int round, int stop, int arrivalTime, int cost) {
        return new TransferStopArrival<>(prev(round), new TestLeg(stop, ANY), arrivalTime, cost);
    }

    private static AbstractStopArrival<RaptorTripSchedule> prev(int round) {
        switch (round) {
            case 1 : return ACCESS_ARRIVAL;
            case 2 : return TRANSFER_R1;
            case 3 : return TRANSFER_R2;
            default: throw new IllegalArgumentException();
        }
    }
}