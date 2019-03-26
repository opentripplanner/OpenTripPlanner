package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import org.opentripplanner.routing.algorithm.raptor.transit_data_provider.TripSchedule;
import org.opentripplanner.routing.core.TraverseMode;

import java.util.List;

public class TripPattern {
    private final int id;

    private final List<TripSchedule> tripSchedules;

    private final TraverseMode transitMode;

    private final int[] stopPattern;

    public TripPattern(int id, List<TripSchedule> tripSchedules, TraverseMode transitMode, int[] stopPattern) {
        this.id = id;
        this.tripSchedules = tripSchedules;
        this.transitMode = transitMode;
        this.stopPattern = stopPattern;
    }

    public int getId() { return id; }

    public List<TripSchedule> getTripSchedules() {
        return tripSchedules;
    }

    public TraverseMode getTransitMode() {
        return transitMode;
    }

    public int[] getStopPattern() {
        return stopPattern;
    }
}
