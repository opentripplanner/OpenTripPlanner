package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Objects;

public class TripPatternWithRaptorStopIndexes {
    private final TripPattern pattern;

    private final int[] stopIndexes;

    public TripPatternWithRaptorStopIndexes(
        int[] stopIndexes,
        TripPattern pattern
    ) {
        this.stopIndexes = stopIndexes;
        this.pattern = pattern;
    }

    public FeedScopedId getId() { return pattern.getId(); }

    public TransitMode getTransitMode() {
        return pattern.getMode();
    }

    public int[] getStopIndexes() {
        return stopIndexes;
    }

    public final TripPattern getPattern() {
        return this.pattern;
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
        TripPatternWithRaptorStopIndexes that = (TripPatternWithRaptorStopIndexes) o;
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
                ", transitMode=" + pattern.getMode() +
                '}';
    }
}
