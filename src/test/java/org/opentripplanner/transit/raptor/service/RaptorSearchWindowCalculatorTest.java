package org.opentripplanner.transit.raptor.service;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.request.DynamicSearchWindowCoefficients;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

import static org.junit.Assert.assertEquals;

public class RaptorSearchWindowCalculatorTest {

    private static final DynamicSearchWindowCoefficients C = new DynamicSearchWindowCoefficients() {
            @Override public double minTripTimeCoefficient() { return 0.5; }
            /** 15 minutes */
            @Override public int minWinTimeMinutes() { return 15; }
            /** Round search-window to nearest 5 minutes (300 seconds) */
            @Override public int stepMinutes() { return 5; }
            /** Set the max search-window length to 5 hours (300 minutes) */
            @Override public int maxWinTimeMinutes() { return 5 * 60; }
    };

    private final RaptorSearchWindowCalculator subject = new RaptorSearchWindowCalculator(C);

    @Test
    public void calcEarliestDeparture() {
        SearchParams searchParams = new RaptorRequestBuilder<TestRaptorTripSchedule>()
                .searchParams()
                // 35 minutes = 2100 seconds
                .latestArrivalTime(10_200)
                .buildSearchParam();

        int minTripTime = 500;

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
        assertEquals(1200, subject.getSearchWindowSeconds());
        assertEquals(8460, subject.getEarliestDepartureTime());
        // Given - verify not changed
        assertEquals(10200, subject.getLatestArrivalTime());
    }

    @Test
    public void calcSearchWindow() {
        SearchParams searchParams = new RaptorRequestBuilder<TestRaptorTripSchedule>()
                .searchParams()
                .earliestDepartureTime(10_200)
                .buildSearchParam();

        int minTripTime = 300;

        subject.withMinTripTime(minTripTime)
                .withSearchParams(searchParams)
                .calculate();

        /*
           EDT = 10_200
           search-window = round_300(0.5 * 300 + 900) = round_300(1_050) = 1200
           LAT = 10_200 + (1200 + roundUp_60(300)) = 11_700
         */
        assertEquals(1200, subject.getSearchWindowSeconds());
        assertEquals(11_700, subject.getLatestArrivalTime());
        // Given - verify not changed
        assertEquals(10200, subject.getEarliestDepartureTime());
    }

    @Test
    public void calcSearchWindowLimitByMaxLength() {
        SearchParams searchParams = new RaptorRequestBuilder<TestRaptorTripSchedule>()
                .searchParams()
                .earliestDepartureTime(10_000)
                .buildSearchParam();

        int minTripTime = 34_500;

        subject.withMinTripTime(minTripTime)
                .withSearchParams(searchParams)
                .calculate();

        /*
           EDT = 10_000
           sw = round_300(0.5 * 34_500 + 900) = round_300(18_150) = 18_300
           search-window = min(18_300, 18_000) = 18_000
           LAT = 10_000 + (18_000 + roundUp_60(34_500)) = 62_500
         */
        assertEquals(18_000, subject.getSearchWindowSeconds());
        assertEquals(62_500, subject.getLatestArrivalTime());
        // Given - verify not changed
        assertEquals(10_000, subject.getEarliestDepartureTime());
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
        assertEquals(2100, subject.getSearchWindowSeconds());
        assertEquals(12600, subject.getLatestArrivalTime());
        // Given - verify not changed
        assertEquals(10200, subject.getEarliestDepartureTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void calculateNotDefinedIfMinTravelTimeNotSet() {
        subject.calculate();
    }

    @Test
    public void roundUpToNearestMinute() {
        assertEquals(0, subject.roundUpToNearestMinute(0));
        assertEquals(60, subject.roundUpToNearestMinute(1));
        assertEquals(60, subject.roundUpToNearestMinute(60));
        assertEquals(120, subject.roundUpToNearestMinute(61));
    }

    @Test(expected = IllegalArgumentException.class)
    public void roundUpToNearestMinuteNotDefinedForNegativeNumbers() {
        subject.roundUpToNearestMinute(-1);
    }

    @Test
    public void roundStep() {
        assertEquals(-300, subject.roundStep(-151f));
        assertEquals(0, subject.roundStep(-150f));
        assertEquals(0, subject.roundStep(0f));
        assertEquals(300, subject.roundStep(300f));
        assertEquals(300, subject.roundStep(449f));
        assertEquals(600, subject.roundStep(450f));
    }
}