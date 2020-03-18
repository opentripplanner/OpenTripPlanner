package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * This represents a single trip within a TripPattern, but with a time offset in seconds. This is used to represent
 * a trip on a subsequent service day than the first one in the date range used.
 */

public class TripScheduleWithOffset implements TripSchedule {

    private final int secondsOffset;
    private final TripPattern pattern;
    private final TripTimes tripTimes;

    TripScheduleWithOffset(TripPattern pattern, TripTimes tripTimes, int offset) {
        this.pattern = pattern;
        this.tripTimes = tripTimes;
        this.secondsOffset = offset;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return this.tripTimes.getArrivalTime(stopPosInPattern) + secondsOffset;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return this.tripTimes.getDepartureTime(stopPosInPattern) + secondsOffset;
    }

    @Override
    public String debugInfo() {
        return null;
    }

    @Override
    public TripTimes getOriginalTripTimes() {
        return this.tripTimes;
    }

    @Override
    public TripPattern getOriginalTripPattern() {
        return this.pattern;
    }

    public int getSecondsOffset() {
        return secondsOffset;
    }
}
