package org.opentripplanner.routing.api.response;

import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.framework.DebugAggregator;

import java.util.List;
import java.util.StringJoiner;

public class RoutingResponse {
    private final TripPlan tripPlan;
    private final TripSearchMetadata metadata;
    private final List<RoutingError> routingErrors;
    private final DebugAggregator debugAggregator;

    public RoutingResponse(
        TripPlan tripPlan,
        TripSearchMetadata metadata,
        List<RoutingError> routingErrors,
        DebugAggregator debugAggregator
    ) {
        this.tripPlan = tripPlan;
        this.metadata = metadata;
        this.routingErrors = routingErrors;
        this.debugAggregator = debugAggregator;
    }

    public TripPlan getTripPlan() {
        return tripPlan;
    }

    public TripSearchMetadata getMetadata() {
        return metadata;
    }

    public DebugAggregator getDebugAggregator() {
        return debugAggregator;
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
