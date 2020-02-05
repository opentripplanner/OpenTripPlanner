package org.opentripplanner.model.routing;

import org.opentripplanner.api.model.TripPlan;

import java.util.StringJoiner;

public class RoutingResponse {
    // TODO TOP2 - The TripPlan need to be split into API and core.
    private final TripPlan tripPlan;
    private final TripSearchMetadata metadata;

    public RoutingResponse(
            TripPlan tripPlan, TripSearchMetadata metadata
    ) {
        this.tripPlan = tripPlan;
        this.metadata = metadata;
    }

    public TripPlan getTripPlan() {
        return tripPlan;
    }

    public TripSearchMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return new StringJoiner(
                ", ", RoutingResponse.class.getSimpleName() + "[", "]"
        )
                .add("tripPlan=" + tripPlan)
                .add("metadata=" + metadata)
                .toString();
    }
}
