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
    private static final int DELAY_BETWEEN_ALIGHT_AND_BOARD = 10;
    private final String name;
    private final int[] arrivalTimes;
    private final int[] departureTimes;
    private final int[] stopIndexes;
    private final int[] restrictions;

    private final RaptorTripPattern pattern = new RaptorTripPattern() {
        @Override public int stopIndex(int stopPositionInPattern) {
            return stopIndexes[stopPositionInPattern];
        }
        @Override
        public boolean boardingPossibleAt(int stopPositionInPattern) {
            return isNotRestricted(stopPositionInPattern, 0b001);
        }
        @Override
        public boolean alightingPossibleAt(int stopPositionInPattern) {
            return isNotRestricted(stopPositionInPattern, 0b010);
        }
        @Override public int numberOfStopsInPattern() { return stopIndexes.length; }

        @Override
        public String modeInfo() { return "BUS"; }

        private boolean isNotRestricted(int index, int mask) {
            return restrictions == null || (restrictions[index] & mask) > 0;
        }
    };

    private TestRaptorTripSchedule(String name, int[] arrivalTimes, int[] departureTimes, int[] stopIndexes, int[] restrictions) {
        this.name = name;
        this.arrivalTimes = arrivalTimes;
        this.departureTimes = departureTimes;
        this.stopIndexes = stopIndexes;
        this.restrictions = restrictions;
    }

    public static Builder create(String name) { return new Builder(name); }

    @Override
    public int arrival(int stopPosInPattern) {
        return arrivalTimes[stopPosInPattern];
    }

    @Override
    public int departure(int stopPosInPattern) {
        return departureTimes[stopPosInPattern];
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
                .addAsHhMm("departures", departureTimes)
                .addInts("stops", stopIndexes)
                .toString();
    }

    public static class Builder {
        private final String name;
        private int[] departureTimes = null;
        private int[] arrivalTimes = null;
        private int[] stopIndexes = null;
        private int[] boardAlightRestrictions;

        private Builder(String name) {
            this.name = name;
        }

        public Builder withAlightTimes(int ... arrivalTimes) {
            this.arrivalTimes = arrivalTimes;
            return this;
        }

        public Builder withBoardTimes(int ... departureTimes) {
            this.departureTimes = departureTimes;
            return this;
        }

        public Builder withBoardAndAlightTimes(int ... times) {
            this.departureTimes = times;
            this.arrivalTimes = times;
            return this;
        }

        public Builder withStopIndexes(int ... stopIndexes) {
            this.stopIndexes = stopIndexes;
            return this;
        }

        /**
         * 0 - 000 : No restriction
         * 1 - 001 : No Boarding.
         * 2 - 010 : No Alighting.
         * 3 - 011 : No Boarding. No Alighting.
         * 4 - 100 : No wheelchair.
         * 5 - 101 : No wheelchair. No Boarding. No Alighting.
         * 6 - 110 : No wheelchair. No Alighting.
         * 7 - 111 : No wheelchair. No Boarding. No Alighting.
         */
        public void setBoardAlightRestrictions(int[] boardAlightRestrictions) {
            this.boardAlightRestrictions = boardAlightRestrictions;
        }

        public TestRaptorTripSchedule build() {
            if(arrivalTimes == null && departureTimes == null) {
                throw new IllegalArgumentException("Either arrival or departure time must be set.");
            }
            if(arrivalTimes == null) {
                arrivalTimes = new int[departureTimes.length];
                for (int i = 0; i < departureTimes.length; i++) {
                    arrivalTimes[i] = departureTimes[i] - DELAY_BETWEEN_ALIGHT_AND_BOARD;
                }
            }
            else if(departureTimes == null) {
                departureTimes = new int[arrivalTimes.length];
                for (int i = 0; i < arrivalTimes.length; i++) {
                    departureTimes[i] = arrivalTimes[i] + DELAY_BETWEEN_ALIGHT_AND_BOARD;
                }
            }
            if(stopIndexes == null) {
                stopIndexes = new int[arrivalTimes.length];
                for (int i = 0; i < stopIndexes.length; i++) {
                    stopIndexes[i] = i + 1;
                }
            }
            return new TestRaptorTripSchedule(
                    name,
                    arrivalTimes,
                    departureTimes,
                    stopIndexes,
                    boardAlightRestrictions
            );
        }
    }
}
