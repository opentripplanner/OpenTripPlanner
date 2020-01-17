package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor.api.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

import static org.junit.Assert.assertEquals;

public class RaptorSearchWindowCalculatorTest {

    // 15 minutes = 900 seconds
    private static final int C = 15;
    private static final float T = 0.5f;

    /** Round search-window to nearest 5 minutes (300 seconds) */
    private static final int STEP = 5;


    @Test
    public void calcEarliestDeparture() {
        SearchParams searchParams = new RaptorRequestBuilder<TestRaptorTripSchedule>()
                .searchParams()
                // 35 minutes = 2100 seconds
                .latestArrivalTime(10_200)
                .buildSearchParam();

        int minTripTime = 500;

        RaptorSearchWindowCalculator subject = new RaptorSearchWindowCalculator(C, T, STEP);

        subject.withMinTripTime(minTripTime)
                .withSearchParams(searchParams)
                .calculate();

        /*
           search-window = round_300(C + T * minTripTime)
           search-window = round_300(900s  + 0.5 * 500s) = round_300(1150s) = 1200s

           EDT = LAT - (search-window + minTripTime)
           EDT = 10200s - (1200s + roundUp_60(500))
           EDT = 10200s - (1200s + 540s)
           EDT = 8460
         */
        assertEquals(1200, subject.getSearchWindowInSeconds());
        assertEquals(8460, subject.getEarliestDepartureTime());
        // Given - verify not changed
        assertEquals(10200, subject.getLatestArrivalTime());
    }

    @Test
    public void calcLatestArrivalTime() {
        SearchParams searchParams = new RaptorRequestBuilder<TestRaptorTripSchedule>()
                .searchParams()
                // 35 minutes = 2100 seconds
                .searchWindowInSeconds(35 * 60)
                .earliestDepartureTime(10_200)
                .buildSearchParam();

        int minTripTime = 300;

        RaptorSearchWindowCalculator subject = new RaptorSearchWindowCalculator(C, T, STEP);

        subject.withMinTripTime(minTripTime)
                .withSearchParams(searchParams)
                .calculate();

        /*
           search-window = 2100
           LAT = EAT + (search-window + roundUp_60(minTripTime))
           LAT = 10200s + (2100s + roundUp60(300s))
           EDT = 10200s + (2100s + 300s)
           EDT = 12600s
         */
        assertEquals(2100, subject.getSearchWindowInSeconds());
        assertEquals(12600, subject.getLatestArrivalTime());
        // Given - verify not changed
        assertEquals(10200, subject.getEarliestDepartureTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculateNotDefinedIfMinTravelTimeNotSet() {
        new RaptorSearchWindowCalculator(C, T, STEP).calculate();
    }

    @Test
    public void roundUpToNearestMinute() {
        RaptorSearchWindowCalculator subject = new RaptorSearchWindowCalculator(C, T, STEP);
        assertEquals(0, subject.roundUpToNearestMinute(0));
        assertEquals(60, subject.roundUpToNearestMinute(1));
        assertEquals(60, subject.roundUpToNearestMinute(60));
        assertEquals(120, subject.roundUpToNearestMinute(61));
    }

    @Test(expected = IllegalArgumentException.class)
    public void roundUpToNearestMinuteNotDefinedForNegativeNumbers() {
        new RaptorSearchWindowCalculator(C, T, STEP).roundUpToNearestMinute(-1);
    }

    @Test
    public void roundStep() {
        RaptorSearchWindowCalculator subject = new RaptorSearchWindowCalculator(C, T, STEP);
        assertEquals(-300, subject.roundStep(-151f));
        assertEquals(0, subject.roundStep(-150f));
        assertEquals(0, subject.roundStep(0f));
        assertEquals(300, subject.roundStep(300f));
        assertEquals(300, subject.roundStep(449f));
        assertEquals(600, subject.roundStep(450f));
    }
}