package org.opentripplanner.transit.raptor._data.transit;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.util.TimeUtils;

/**
 * An implementation of the {@link RaptorTripSchedule} for unit-testing.
 * <p>
 * The {@link RaptorTripPattern} for this schedule return {@code stopIndex == stopPosInPattern + 1 }.
 */
public class TestTripSchedule implements RaptorTripSchedule {
    private static final int DELAY_BETWEEN_ALIGHT_AND_BOARD = 10;
    private final RaptorTripPattern pattern;
    private final int[] arrivalTimes;
    private final int[] departureTimes;


    private TestTripSchedule(
        TestTripPattern pattern,
        int[] arrivalTimes,
        int[] departureTimes
    ) {
        this.pattern = pattern;
        this.arrivalTimes = arrivalTimes;
        this.departureTimes = departureTimes;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return arrivalTimes[stopPosInPattern];
    }

    @Override
    public int departure(int stopPosInPattern) {
        return departureTimes[stopPosInPattern];
    }

    @Override
    public RaptorTripPattern pattern() {
        return pattern;
    }

    public int size() {
        return arrivalTimes.length;
    }

    @Override
    public String toString() {
        if(arrivalTimes == departureTimes) {
            return ToStringBuilder
                .of(TestTripSchedule.class)
                .addServiceTimeSchedule("times", arrivalTimes)
                .toString();
        }
        return ToStringBuilder
                .of(TestTripSchedule.class)
                .addServiceTimeSchedule("arrivals", arrivalTimes)
                .addServiceTimeSchedule("departures", departureTimes)
                .toString();
    }

    public static TestTripSchedule.Builder schedule() {
        return new TestTripSchedule.Builder();
    }

    public static TestTripSchedule.Builder schedule(TestTripPattern pattern) {
        return schedule().pattern(pattern);
    }


    public static TestTripSchedule.Builder schedule(String times) {
        return new TestTripSchedule.Builder().times(times);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {
        private TestTripPattern pattern;
        private int[] arrivalTimes;
        private int[] departureTimes;

        public TestTripSchedule.Builder pattern(TestTripPattern pattern) {
            this.pattern = pattern;
            return this;
        }

        /** @param times departure and arrival times per stop. Example: "0:10, 0:20, 0:45 .." */
        public TestTripSchedule.Builder times(String times) {
            return times(TimeUtils.times(times));
        }

        /** @param times departure and arrival times per stop in seconds past midnight. */
        public TestTripSchedule.Builder times(int ... times) {
            arrivals(times);
            departures(times);
            return this;
        }

        /** @param arrivalTimes arrival times per stop in seconds past midnight. */
        public TestTripSchedule.Builder arrivals(int ... arrivalTimes) {
            this.arrivalTimes = arrivalTimes;
            return this;
        }

        /** @param departureTimes departure times per stop in seconds past midnight. */
        public TestTripSchedule.Builder departures(int ... departureTimes) {
            this.departureTimes = departureTimes;
            return this;
        }

        public TestTripSchedule build() {
            if(arrivalTimes == null) {
                arrivalTimes = copyDelta(-DELAY_BETWEEN_ALIGHT_AND_BOARD, departureTimes);
            }
            else if(departureTimes == null) {
                departureTimes = copyDelta(DELAY_BETWEEN_ALIGHT_AND_BOARD, arrivalTimes);
            }
            if(arrivalTimes.length != departureTimes.length) {
                throw new IllegalStateException(
                    "Number of arrival and departure times do not match."
                        + " Arrivals: " + arrivalTimes.length
                        + ", departures: " + arrivalTimes.length
                );
            }
            if(pattern == null) {
                pattern = TestTripPattern.pattern("DummyPattern", new int[arrivalTimes.length]);

            }
            if(arrivalTimes.length != pattern.numberOfStopsInPattern()) {
                throw new IllegalStateException(
                    "Number of arrival and departure times do not match stops in pattern."
                        + " Arrivals/departures: " + arrivalTimes.length
                        + ", stops: " + pattern.numberOfStopsInPattern()
                );
            }
            return new TestTripSchedule(pattern, arrivalTimes, departureTimes);
        }

        private static int[] copyDelta(int delta, int[] source) {
            int[] target = new int[source.length];
            for (int i = 0; i < source.length; i++) {
                target[i] = source[i] + delta;
            }
            return target;
        }
    }
}
