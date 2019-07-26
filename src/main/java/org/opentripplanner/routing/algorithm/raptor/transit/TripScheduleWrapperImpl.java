package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TripScheduleWrapperImpl implements TripSchedule {

    private static final Logger LOG = LoggerFactory.getLogger(TripScheduleWrapperImpl.class);

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
    public int getServiceCode() {
        return originalTripTimes.serviceCode;
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
