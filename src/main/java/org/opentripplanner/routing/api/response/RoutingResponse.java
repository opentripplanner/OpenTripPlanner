package org.opentripplanner.routing.api.response;

import java.util.List;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.framework.DebugTimingAggregator;

public class RoutingResponse {
    private final TripPlan tripPlan;
    private final PageCursor pageCursor;
    private final TripSearchMetadata metadata;
    private final List<RoutingError> routingErrors;
    private final DebugTimingAggregator debugTimingAggregator;

    public RoutingResponse(
        TripPlan tripPlan,
        PageCursor pageCursor,
        TripSearchMetadata metadata,
        List<RoutingError> routingErrors,
        DebugTimingAggregator debugTimingAggregator
    ) {
        this.tripPlan = tripPlan;
        this.pageCursor = pageCursor;
        this.metadata = metadata;
        this.routingErrors = routingErrors;
        this.debugTimingAggregator = debugTimingAggregator;
    }

    public TripPlan getTripPlan() {
        return tripPlan;
    }

    public PageCursor getPageCursor() {
        return pageCursor;
    }

    public TripSearchMetadata getMetadata() {
        return metadata;
    }

    public DebugTimingAggregator getDebugTimingAggregator() {
        return debugTimingAggregator;
    }

    public List<RoutingError> getRoutingErrors() { return routingErrors; }

    @Override
    public String toString() {
        return ToStringBuilder.of(RoutingResponse.class)
                .addObj("tripPlan", tripPlan)
                .addObj("pageCursor", pageCursor)
                .addObj("metadata", metadata)
                .addObj("routingErrors", routingErrors)
                .toString();
    }
}
