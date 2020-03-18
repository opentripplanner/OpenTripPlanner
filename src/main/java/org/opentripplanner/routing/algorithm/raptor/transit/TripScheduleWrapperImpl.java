package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * TODO OTP2 - Performance test this and the other candidate (none wrapper) and clean up code.
 *           - This is discussed PR #2794
 */
public class TripScheduleWrapperImpl implements TripSchedule {

    private final TripTimes originalTripTimes;
    private final TripPattern originalTripPattern;

    public TripScheduleWrapperImpl (TripTimes originalTripTimes, TripPattern originalTripPattern) {
        this.originalTripTimes = originalTripTimes;
        this.originalTripPattern = originalTripPattern;
    }

    @Override
    public TripTimes getOriginalTripTimes() {
        return originalTripTimes;
    }

    @Override
    public TripPattern getOriginalTripPattern() {
        return originalTripPattern;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return originalTripTimes.getArrivalTime(stopPosInPattern);
    }

    @Override
    public int departure(int stopPosInPattern) {
        return originalTripTimes.getDepartureTime(stopPosInPattern);
    }

    @Override
    public String debugInfo() {
        throw new UnsupportedOperationException();
    }

}
