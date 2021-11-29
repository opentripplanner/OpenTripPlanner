package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.D1m;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_A;
import static org.opentripplanner.transit.raptor._data.RaptorTestConstants.STOP_B;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.util.time.TimeUtils.hm2time;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.transit.TestTransitData;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;

public class ForwardTransitCalculatorTest {
    private static final int TRIP_SEARCH_BINARY_SEARCH_THRESHOLD = 7;

    private int earliestDepartureTime = hm2time(8, 0);
    private int searchWindowSizeInSeconds = 2 * 60 * 60;
    private int latestAcceptableArrivalTime = hm2time(16, 0);
    private int iterationStep = 60;


    private TransitCalculator<TestTripSchedule> create() {
        return new ForwardTransitCalculator<>(
                TRIP_SEARCH_BINARY_SEARCH_THRESHOLD,
                earliestDepartureTime,
                searchWindowSizeInSeconds,
                latestAcceptableArrivalTime,
                iterationStep
        );
    }

    @Test
    public void isBest() {
        var subject = create();

        assertTrue(subject.isBest(10, 11));
        assertFalse(subject.isBest(11, 10));
        assertFalse(subject.isBest(10, 10));
    }

    @Test
    public void exceedsTimeLimit() {
        latestAcceptableArrivalTime = 1200;
        var subject = create();

        assertFalse(subject.exceedsTimeLimit(0));
        assertFalse(subject.exceedsTimeLimit(1200));
        assertTrue(subject.exceedsTimeLimit(1201));

        latestAcceptableArrivalTime = hm2time(16, 0);

        assertEquals(
                "The arrival time exceeds the time limit, arrive to late: 16:00:00.",
                create().exceedsTimeLimitReason()
        );

        latestAcceptableArrivalTime = TransitCalculator.TIME_NOT_SET;
        subject = create();
        assertFalse(subject.exceedsTimeLimit(0));
        assertFalse(subject.exceedsTimeLimit(2_000_000_000));
    }

    @Test
    public void oneIterationOnly() {
        var subject = create();

        assertFalse(subject.oneIterationOnly());

        searchWindowSizeInSeconds = 0;
        subject = create();

        assertTrue(subject.oneIterationOnly());
    }

    @Test
    public void duration() {
        assertEquals(600, create().plusDuration(500, 100));
        assertEquals(400, create().minusDuration(500, 100));
        assertEquals(400, create().duration(100, 500));
    }

    @Test
    public void unreachedTime() {
        assertEquals(Integer.MAX_VALUE, create().unreachedTime());
    }


    @Test
    public void latestArrivalTime() {
        var s = TestTripSchedule.schedule().arrivals(500).build();
        assertEquals(500, create().stopArrivalTime(s, 0, 0));
    }

    @Test
    public void rangeRaptorMinutes() {
        earliestDepartureTime = 500;
        searchWindowSizeInSeconds = 200;
        iterationStep = 100;

        assertIntIterator(create().rangeRaptorMinutes(), 600, 500);
    }

    @Test
    public void patternStopIterator() {
        assertIntIterator(create().patternStopIterator(2), 0, 1);
    }

    @Test
    public void getTransfers() {
        var subject = create();
        var transitData = new TestTransitData()
                .withTransfer(STOP_A, walk(STOP_B, D1m));

        // Expect transfer from stop A to stop B
        var transfersFromStopA = subject.getTransfers(transitData, STOP_A);
        assertTrue(transfersFromStopA.hasNext());
        assertEquals(STOP_B, transfersFromStopA.next().stop());

        // No transfer for stop B expected
        assertFalse(subject.getTransfers(transitData, STOP_B).hasNext());
    }

    private void assertIntIterator(IntIterator it, int ... values) {
        for (int v : values) {
            assertTrue(it.hasNext());
            assertEquals(v, it.next());
        }
        assertFalse(it.hasNext());
    }
}