package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.FeedScopedId;

public class TimeMissingForTrip implements DataImportIssue {

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
