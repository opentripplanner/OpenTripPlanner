package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * This represents a single trip within a TripPattern
 */
public class TripScheduleImpl implements TripSchedule {

    /**
     * Arrival times in seconds from midnight by stop index
     */
    private final int[] arrivals;

    /**
     * Departure times in seconds from midnight by stop index
     */
    private final int[] departures;

    private final TripTimes originalTripTimes;

    private final TripPattern originalTripPattern;

    private final int serviceCode;

    public TripScheduleImpl(TripTimes originalTripTimes, TripPattern originalTripPattern) {
        final int numStops = originalTripTimes.getNumStops();
        // Copy over realtime or scheduled times
        // We could just read through to the underlying tripTimes but that may be less efficient (measure this).
        arrivals = new int[numStops];
        departures = new int[numStops];
        for (int i = 0; i < numStops; i++) {
            arrivals[i] = originalTripTimes.getArrivalTime(i);
            departures[i] = originalTripTimes.getDepartureTime(i);
        }
        this.serviceCode = originalTripTimes.serviceCode;
        this.originalTripTimes = originalTripTimes;
        this.originalTripPattern = originalTripPattern;
    }

    /**
     * For tests.
     */
    public TripScheduleImpl() {
        arrivals = null;
        departures = null;
        originalTripTimes = null;
        originalTripPattern = null;
        serviceCode = 0;
    }

    @Override
    public int arrival(int stopPosInPattern) { return arrivals[stopPosInPattern]; }

    @Override
    public int departure(int stopPosInPattern) {
        return departures[stopPosInPattern];
    }

    @Override
    public String debugInfo() {
        throw new UnsupportedOperationException();
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
        return serviceCode;
    }
}
