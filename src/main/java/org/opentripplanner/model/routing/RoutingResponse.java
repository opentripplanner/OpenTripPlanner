package org.opentripplanner.model.routing;

import org.opentripplanner.api.model.ApiTripPlan;

import java.util.StringJoiner;

public class RoutingResponse {
    private final ApiTripPlan tripPlan;
    private final TripSearchMetadata metadata;

    public RoutingResponse(
            ApiTripPlan tripPlan, TripSearchMetadata metadata
    ) {
        this.tripPlan = tripPlan;
        this.metadata = metadata;
    }

    public ApiTripPlan getTripPlan() {
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
