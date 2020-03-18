package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Objects;

public class TripPattern {
    private final org.opentripplanner.model.TripPattern originalTripPattern;

    private final TraverseMode transitMode;

    private final int[] stopIndexes;

    public TripPattern(
        TraverseMode transitMode,
        int[] stopIndexes,
        org.opentripplanner.model.TripPattern originalTripPattern
    ) {
        this.transitMode = transitMode;
        this.stopIndexes = stopIndexes;
        this.originalTripPattern = originalTripPattern;
    }

    public FeedScopedId getId() { return originalTripPattern.getId(); }

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
     * See {@link RaptorTripPattern#stopIndex(int)}
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
