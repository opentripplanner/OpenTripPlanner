package org.opentripplanner.transit.raptor.api;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import java.util.Arrays;

public class TestRaptorTripSchedule implements RaptorTripSchedule {
    private static final int DEFAULT_DEPARTURE_DELAY = 10;
    private final int departureDelay;
    private final int[] arrivalTimes;

    public static TestRaptorTripSchedule createTripSchedule(int departureDelay, int ... arrivalTimes) {
        return new TestRaptorTripSchedule(departureDelay, arrivalTimes);
    }

    public static TestRaptorTripSchedule createTripScheduleUseingArrivalTimes(int ... arrivalTimes) {
        return new TestRaptorTripSchedule(DEFAULT_DEPARTURE_DELAY, arrivalTimes);
    }

    public static TestRaptorTripSchedule createTripScheduleUseingDepartureTimes(int ... departureTimes) {
        int[] arrivalTimes = Arrays.copyOf(departureTimes, departureTimes.length);
        for (int i = 0; i < arrivalTimes.length; i++) {
            arrivalTimes[i] -= DEFAULT_DEPARTURE_DELAY;
        }
        return new TestRaptorTripSchedule(DEFAULT_DEPARTURE_DELAY, arrivalTimes);
    }

    private TestRaptorTripSchedule(int departureDelay, int ... arrivalTimes) {
        this.departureDelay = departureDelay;
        this.arrivalTimes = arrivalTimes;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return arrivalTimes[stopPosInPattern];
    }

    @Override
    public int departure(int stopPosInPattern) {
        return arrivalTimes[stopPosInPattern] + departureDelay;
    }

    @Override
    public String debugInfo() {
        return Arrays.toString(arrivalTimes);
    }

    public int size() {
        return arrivalTimes.length;
    }
}
