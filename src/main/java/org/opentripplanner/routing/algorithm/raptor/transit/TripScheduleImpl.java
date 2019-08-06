package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.util.DateUtils.secToHHMM;

/**
 * This represents a single trip within a TripPattern.
 * It is the OTP2 Raptor equivalent of an OTP1 TripTimes.
 */
public class TripScheduleImpl implements TripSchedule {

    private static final Logger LOG = LoggerFactory.getLogger(TripScheduleImpl.class);

    private final TripTimes originalTripTimes;

    private final TripPattern originalTripPattern;

    /**
     * Arrival times in seconds from midnight by stop index
     */
    private final int[] arrivals;

    /**
     * Departure times in seconds from midnight by stop index
     */
    private final int[] departures;

    /** How many seconds to shift the arrivals and departures (enabling reuse of arrival/departure arrays). */
    private final int timeShift;

    public TripScheduleImpl (
            TripTimes originalTripTimes,
            TripPattern originalTripPattern,
            int[] arrivals,
            int[] departures,
            int timeShift
    ) {
        // Sanity check array and stop pattern dimensions.
        final int numStops = originalTripTimes.getNumStops();
        if (originalTripPattern.getStops().size() != numStops) {
            LOG.error("TripPattern is not the same size as the TripTimes. This indicates a bug.");
        }
        if (arrivals.length != numStops) {
            LOG.error("Arrivals arrays is not the same size as the TripTimes. This indicates a bug.");
        }
        if (departures.length != numStops) {
            LOG.error("Departures arrays is not the same size as the TripTimes. This indicates a bug.");
        }
        this.originalTripTimes = originalTripTimes;
        this.originalTripPattern = originalTripPattern;
        this.arrivals = arrivals;
        this.departures = departures;
        this.timeShift = timeShift;
    }

    /**
     * For tests.
     */
    public TripScheduleImpl() {
        originalTripTimes = null;
        originalTripPattern = null;
        arrivals = null;
        departures = null;
        timeShift = 0;
    }

    @Override
    public int arrival(int stopPosInPattern) {
        return arrivals[stopPosInPattern] + timeShift;
    }

    @Override
    public int departure(int stopPosInPattern) {
        return departures[stopPosInPattern] + timeShift;
    }

    @Override
    public String debugInfo() {
        // Create a short description of a trip, which can be used to for logging
        // and debugging. Does not need to be 100% uniq, but nice if it is human
        // readable. The format used here is <Mode>-<Route name(short)>-<departure-time>
        // Example: Bus-32-12:56   (The depature time is )

        String mode = originalTripPattern.mode.name();
        String r = originalTripPattern.route.getShortName();
        if(r == null || r.isEmpty()) {
            r = originalTripPattern.route.getLongName();
        }
        return mode + "-" + r + "-" + secToHHMM(departure(0));
    }

    @Override
    public TripTimes getOriginalTripTimes() {
        return originalTripTimes;
    }

    @Override
    public TripPattern getOriginalTripPattern() {
        return originalTripPattern;
    }

}
