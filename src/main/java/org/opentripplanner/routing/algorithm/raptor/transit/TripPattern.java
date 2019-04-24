package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.core.TraverseMode;

import java.util.Collection;
import java.util.List;

public class TripPattern {
    private final int id;

    private final List<TripSchedule> tripSchedules;

    private final TraverseMode transitMode;

    private final int[] stopIndexes;

    public TripPattern(int id, List<TripSchedule> tripSchedules, TraverseMode transitMode, int[] stopIndexes) {
        this.id = id;
        this.tripSchedules = tripSchedules;
        this.transitMode = transitMode;
        this.stopIndexes = stopIndexes;
    }

    public int getId() { return id; }

    public Collection<TripSchedule> getTripSchedules() {
        return tripSchedules;
    }

    public TraverseMode getTransitMode() {
        return transitMode;
    }

    public int[] getStopIndexes() {
        return stopIndexes;
    }

    /**
     * See {@link com.conveyal.r5.otp2.api.transit.TripPatternInfo#stopIndex(int)}
     */
    public int stopIndex(int stopPositionInPattern) {
        return stopIndexes[stopPositionInPattern];
    }
}
