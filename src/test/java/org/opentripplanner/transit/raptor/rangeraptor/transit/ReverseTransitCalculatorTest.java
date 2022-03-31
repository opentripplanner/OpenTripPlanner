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

public class ReverseTransitCalculatorTest {
    private static final int TRIP_SEARCH_BINARY_SEARCH_THRESHOLD = 7;

    private int latestArrivalTime = hm2time(8, 0);
    private int searchWindowSizeInSeconds = 2 * 60 * 60;
    private int earliestAcceptableDepartureTime = hm2time(16, 0);
    private int iterationStep = 60;


    private TransitCalculator<TestTripSchedule> create() {
        return new ReverseTransitCalculator<>(
                TRIP_SEARCH_BINARY_SEARCH_THRESHOLD,
                latestArrivalTime,
                searchWindowSizeInSeconds,
                earliestAcceptableDepartureTime,
                iterationStep
        );
    }

    @Test
    public void exceedsTimeLimit() {
        earliestAcceptableDepartureTime = 1200;
        var subject = create();

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
        var subject = create();

        assertFalse(subject.oneIterationOnly());

        searchWindowSizeInSeconds = 0;
        subject = create();

        assertTrue(subject.oneIterationOnly());
    }

    @Test
    public void latestArrivalTime() {
        // Ignore board slack for reverse search, boardSlack is added to alight times.
        int slackInSeconds = 75;
        TestTripSchedule s = TestTripSchedule.schedule().departures(500).build();
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
    }

    @Test
    public void getTransfers() {
        var subject = create();
        var transitData = new TestTransitData()
                        .withTransfer(STOP_A, walk(STOP_B, D1m));

        // Expect transfer from stop A to stop B (reversed)
        var transfersFromStopB = subject.getTransfers(transitData, STOP_B);
        assertEquals(1, transfersFromStopB.length);
        assertEquals(STOP_A, transfersFromStopB[0].stop());

        // No transfer form stop A expected
        assertEquals(0, subject.getTransfers(transitData, STOP_A).length);
    }

    private void assertIntIterator(IntIterator it, int ... values) {
        for (int v : values) {
            assertTrue(it.hasNext());
            assertEquals(v, it.next());
        }
        assertFalse(it.hasNext());
    }
}