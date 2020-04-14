package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.IntIterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.transit.raptor.util.TimeUtils.hm2time;

public class ReverseTransitCalculatorTest {
    private static final int TRIP_SEARCH_BINARY_SEARCH_THRESHOLD = 7;

    private int slackInSeconds = 30;
    private int latestArrivalTime = hm2time(8, 0);
    private int searchWindowSizeInSeconds = 2 * 60 * 60;
    private int earliestAcceptableDepartureTime = hm2time(16, 0);
    private int iterationStep = 60;


    private TransitCalculator create() {
        return new ReverseTransitCalculator(
                TRIP_SEARCH_BINARY_SEARCH_THRESHOLD,
                latestArrivalTime,
                searchWindowSizeInSeconds,
                earliestAcceptableDepartureTime,
                iterationStep
        );
    }

    @Test
    public void isBest() {
        TransitCalculator subject = create();

        assertTrue(subject.isBest(11, 10));
        assertFalse(subject.isBest(10, 11));
        assertFalse(subject.isBest(10, 10));
    }

    @Test
    public void exceedsTimeLimit() {
        earliestAcceptableDepartureTime = 1200;
        TransitCalculator subject = create();

        assertFalse(subject.exceedsTimeLimit(200_000));
        assertFalse(subject.exceedsTimeLimit(1200));
        assertTrue(subject.exceedsTimeLimit(1199));

        earliestAcceptableDepartureTime = hm2time(16, 0);

        assertEquals(
                "The departure time exceeds the time limit, depart to early: 16:00:00.",
                create().exceedsTimeLimitReason()
        );

        earliestAcceptableDepartureTime = -1;
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
        assertEquals(400, create().plusDuration(500, 100));
        assertEquals(600, create().minusDuration(500, 100));
        assertEquals(400, create().duration(500, 100));
    }

    @Test
    public void unreachedTime() {
        assertEquals(Integer.MIN_VALUE, create().unreachedTime());
    }

    @Test
    public void latestArrivalTime() {
        // Ignore board slack for reverse search, boardSlack is added to alight times.
        slackInSeconds = 75;
        TestRaptorTripSchedule s = TestRaptorTripSchedule.create("T1").withBoardTimes(500).build();
        assertEquals(425, create().stopArrivalTime(s, 0, slackInSeconds));
    }

    @Test
    public void rangeRaptorMinutes() {
        latestArrivalTime = 500;
        searchWindowSizeInSeconds = 200;
        iterationStep = 100;

        assertIntIterator(create().rangeRaptorMinutes(), 400, 500);
    }

    @Test
    public void patternStopIterator() {
        assertIntIterator(create().patternStopIterator(2), 1, 0);
        assertIntIterator(create().patternStopIterator(3, 6), 2, 1, 0);
    }


    private void assertIntIterator(IntIterator it, int ... values) {
        for (int v : values) {
            assertTrue(it.hasNext());
            assertEquals(v, it.next());
        }
        assertFalse(it.hasNext());
    }
}