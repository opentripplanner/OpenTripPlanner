package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.FeedScopedId;

public class TimeMissingForTrip extends GraphBuilderAnnotation {

    public static final String FMT = "Time missing for trip %s";

    final FeedScopedId tripId;

    public TimeMissingForTrip(FeedScopedId tripId) {
        this.tripId = tripId;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, tripId);
    }
}
