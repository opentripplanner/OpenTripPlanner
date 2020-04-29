package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.transit.raptor.util.TimeUtils.hm2time;

public class ForwardTransitCalculatorTest {
    private static final int TRIP_SEARCH_BINARY_SEARCH_THRESHOLD = 7;

    private int boardSlackInSeconds = 30;
    private int earliestDepartureTime = hm2time(8, 0);
    private int searchWindowSizeInSeconds = 2 * 60 * 60;
    private int latestAcceptableArrivalTime = hm2time(16, 0);
    private int iterationStep = 60;


    private TransitCalculator create() {
        return new ForwardTransitCalculator(
                TRIP_SEARCH_BINARY_SEARCH_THRESHOLD,
                earliestDepartureTime,
                searchWindowSizeInSeconds,
                latestAcceptableArrivalTime,
                iterationStep
        );
    }

    @Test
    public void isBest() {
        TransitCalculator subject = create();

        assertTrue(subject.isBest(10, 11));
        assertFalse(subject.isBest(11, 10));
        assertFalse(subject.isBest(10, 10));
    }

    @Test
    public void exceedsTimeLimit() {
        latestAcceptableArrivalTime = 1200;
        TransitCalculator subject = create();

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
        TransitCalculator subject = create();

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
        TestRaptorTripSchedule s = TestRaptorTripSchedule.create("T").withAlightTimes(500).build();
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
        assertIntIterator(create().patternStopIterator(3, 6), 4, 5);
    }


    private void assertIntIterator(IntIterator it, int ... values) {
        for (int v : values) {
            assertTrue(it.hasNext());
            assertEquals(v, it.next());
        }
        assertFalse(it.hasNext());
    }
}