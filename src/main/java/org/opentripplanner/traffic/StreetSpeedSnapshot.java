package org.opentripplanner.traffic;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;

import java.util.Map;

/**
 * A source of speeds for traversing streets.
 */
public class StreetSpeedSnapshot {
    private final Map<Segment, SegmentSpeedSample> samples;

    /** Get the speed for traversing the given edge with the given mode at the given time. Returns NaN if there is no speed information available. */
    public double getSpeed (StreetEdge edge, TraverseMode traverseMode, long timeMillis) {
        if (traverseMode != TraverseMode.CAR)
            return Double.NaN;

        SegmentSpeedSample sample = samples.get(new Segment(edge));

        if (sample == null) return Double.NaN;

        return sample.getSpeed(timeMillis);
    }

    public StreetSpeedSnapshot (Map<Segment, SegmentSpeedSample> samples) {
        this.samples = samples;
    }
}
