package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * This represents a single trip within a TripPattern, but with a time offset in seconds. This is used to represent
 * a trip on a subsequent service day than the first one in the date range used.
 */

public class TripScheduleWithOffset implements TripSchedule {

    private final int secondsOffset;

    private final TripSchedule tripSchedule;

    TripScheduleWithOffset(TripSchedule tripSchedule, int offset) {
        this.tripSchedule = tripSchedule;
        this.secondsOffset = offset;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return this.tripSchedule.arrival(stopPosInPattern) + secondsOffset;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return this.tripSchedule.departure(stopPosInPattern) + secondsOffset;
    }

    @Override
    public String debugInfo() {
        return null;
    }

    @Override
    public TripTimes getOriginalTripTimes() {
        return this.tripSchedule.getOriginalTripTimes();
    }

    @Override
    public TripPattern getOriginalTripPattern() {
        return this.tripSchedule.getOriginalTripPattern();
    }

    @Override
    public int getServiceCode() {
        return this.tripSchedule.getServiceCode();
    }

    public int getSecondsOffset() {
        return secondsOffset;
    }
}
