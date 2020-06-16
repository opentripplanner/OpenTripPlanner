package org.opentripplanner.routing.api.response;

import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.plan.TripPlan;

import java.util.List;
import java.util.StringJoiner;

public class RoutingResponse {
    private final TripPlan tripPlan;
    private final TripSearchMetadata metadata;
    private final List<RoutingError> routingErrors;
    private final DebugOutput debugOutput;

    public RoutingResponse(
        TripPlan tripPlan,
        TripSearchMetadata metadata,
        List<RoutingError> routingErrors,
        DebugOutput debugOutput
    ) {
        this.tripPlan = tripPlan;
        this.metadata = metadata;
        this.routingErrors = routingErrors;
        this.debugOutput = debugOutput;
    }

    public TripPlan getTripPlan() {
        return tripPlan;
    }

    public TripSearchMetadata getMetadata() {
        return metadata;
    }

    public DebugOutput getDebugOutput() {
        return debugOutput;
    }

    public List<RoutingError> getRoutingErrors() { return routingErrors; }

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
