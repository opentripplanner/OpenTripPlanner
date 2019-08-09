package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;
import java.util.Objects;

public class TripPattern {
    private final int id;

    // TODO These are only used in an intermediary step during mapping and could be deleted after the mapping has been refactored
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

    public List<TripSchedule> getTripSchedules() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripPattern that = (TripPattern) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "TripPattern{" +
                "id=" + id +
                ", transitMode=" + transitMode +
                '}';
    }
}
