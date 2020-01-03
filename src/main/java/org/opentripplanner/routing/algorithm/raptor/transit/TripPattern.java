package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;
import java.util.Objects;

public class TripPattern {
    // TODO These are only used in an intermediary step during mapping and could be deleted after the mapping has been refactored
    private final List<TripSchedule> tripSchedules;

    private final org.opentripplanner.model.TripPattern originalTripPattern;

    private final TraverseMode transitMode;

    private final int[] stopIndexes;

    public TripPattern(
        List<TripSchedule> tripSchedules,
        TraverseMode transitMode,
        int[] stopIndexes,
        org.opentripplanner.model.TripPattern originalTripPattern
    ) {
        this.tripSchedules = tripSchedules;
        this.transitMode = transitMode;
        this.stopIndexes = stopIndexes;
        this.originalTripPattern = originalTripPattern;
    }

    public FeedScopedId getId() { return originalTripPattern.getId(); }

    public List<TripSchedule> getTripSchedules() {
        return tripSchedules;
    }

    public TraverseMode getTransitMode() {
        return transitMode;
    }

    public int[] getStopIndexes() {
        return stopIndexes;
    }

    public org.opentripplanner.model.TripPattern getOriginalTripPattern() {
        return this.originalTripPattern;
    }

    /**
     * See {@link org.opentripplanner.transit.raptor.api.transit.TripPatternInfo#stopIndex(int)}
     */
    public int stopIndex(int stopPositionInPattern) {
        return stopIndexes[stopPositionInPattern];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TripPattern that = (TripPattern) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "TripPattern{" +
                "id=" + getId() +
                ", transitMode=" + transitMode +
                '}';
    }
}
