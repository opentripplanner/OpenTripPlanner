package org.opentripplanner.model.routing;

import org.opentripplanner.model.plan.TripPlan;

import java.util.StringJoiner;

public class RoutingResponse {
    private final TripPlan tripPlan;
    private final TripSearchMetadata metadata;

    public RoutingResponse(TripPlan tripPlan, TripSearchMetadata metadata) {
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
