package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 * An implementation of the {@link RaptorTripSchedule} for unit-testing.
 * <p>
 * The {@link RaptorTripPattern} for this schedule return {@code stopIndex == stopPosInPattern + 1 }.
 */
public class TestRaptorTripSchedule implements RaptorTripSchedule {
    private final String name;
    private final int[] arrivalTimes;
    private final int[] depatureTimes;
    private final int[] stopIndexes;

    private final RaptorTripPattern pattern = new RaptorTripPattern() {
        @Override public int stopIndex(int stopPositionInPattern) {
            return stopIndexes[stopPositionInPattern];
        }
        @Override public int numberOfStopsInPattern() { return stopIndexes.length; }
    };

    private TestRaptorTripSchedule(String name, int[] arrivalTimes, int[] depatureTimes, int[] stopIndexes) {
        this.name = name;
        this.arrivalTimes = arrivalTimes;
        this.depatureTimes = depatureTimes;
        this.stopIndexes = stopIndexes;
    }

    public static Builder create(String name) { return new Builder(name); }

    @Override
    public int arrival(int stopPosInPattern) {
        return arrivalTimes[stopPosInPattern];
    }

    @Override
    public int departure(int stopPosInPattern) {
        return depatureTimes[stopPosInPattern];
    }

    @Override
    public String debugInfo() {
        return name;
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
        return ToStringBuilder
                .of(TestRaptorTripSchedule.class)
                .addAsHhMm("arrivals", arrivalTimes)
                .addAsHhMm("departures", depatureTimes)
                .addInts("stops", stopIndexes)
                .toString();
    }

    public static class Builder {
        private final String name;
        private int[] departureTimes = null;
        private int[] arrivalTimes = null;
        private int[] stopIndexes = null;
        private int stopIndexDelta = 1;
        private int departureDelay = 10;

        private Builder(String name) {
            this.name = name;
        }

        public Builder withArrivalTimes(int ... arrivalTimes) {
            this.arrivalTimes = arrivalTimes;
            return this;
        }

        public Builder withDepartureTimes(int ... departureTimes) {
            this.departureTimes = departureTimes;
            return this;
        }

        public Builder withStopIndexes(int ... stopIndexes) {
            this.stopIndexes = stopIndexes;
            return this;
        }

        public Builder withDepartureDelay(int departureDelay) {
            this.departureDelay = departureDelay;
            return this;
        }

        public Builder withStopIndexDelta(int stopIndexDelta) {
            this.stopIndexDelta = stopIndexDelta;
            return this;
        }

        public TestRaptorTripSchedule build() {
            if(arrivalTimes == null && departureTimes == null) {
                throw new IllegalArgumentException("Either arrival or departure time must be set.");
            }
            if(arrivalTimes == null) {
                arrivalTimes = new int[departureTimes.length];
                for (int i = 0; i < departureTimes.length; i++) {
                    arrivalTimes[i] = departureTimes[i] - departureDelay;
                }
            }
            else if(departureTimes == null) {
                departureTimes = new int[arrivalTimes.length];
                for (int i = 0; i < arrivalTimes.length; i++) {
                    departureTimes[i] = arrivalTimes[i] + departureDelay;
                }
            }
            if(stopIndexes == null) {
                stopIndexes = new int[arrivalTimes.length];
                for (int i = 0; i < stopIndexes.length; i++) {
                    stopIndexes[i] = i + stopIndexDelta;
                }
            }
            return new TestRaptorTripSchedule(name, arrivalTimes, departureTimes, stopIndexes);
        }
    }
}
