package org.opentripplanner.routing.api.response;

import java.util.List;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.model.plan.paging.cursor.PageCursor;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.utils.tostring.ToStringBuilder;

public class RoutingResponse {

  private final TripPlan tripPlan;
  private final PageCursor nextPageCursor;
  private final PageCursor previousPageCursor;
  private final TripSearchMetadata metadata;
  private final List<RoutingError> routingErrors;
  private final DebugTimingAggregator debugTimingAggregator;

  public RoutingResponse(
    TripPlan tripPlan,
    PageCursor previousPageCursor,
    PageCursor nextPageCursor,
    TripSearchMetadata metadata,
    List<RoutingError> routingErrors,
    DebugTimingAggregator debugTimingAggregator
  ) {
    this.tripPlan = tripPlan;
    this.nextPageCursor = nextPageCursor;
    this.previousPageCursor = previousPageCursor;
    this.metadata = metadata;
    this.routingErrors = routingErrors;
    this.debugTimingAggregator = debugTimingAggregator;
  }

  public TripPlan getTripPlan() {
    return tripPlan;
  }

  public PageCursor getNextPageCursor() {
    return nextPageCursor;
  }

  public PageCursor getPreviousPageCursor() {
    return previousPageCursor;
  }

  public TripSearchMetadata getMetadata() {
    return metadata;
  }

  public DebugTimingAggregator getDebugTimingAggregator() {
    return debugTimingAggregator;
  }

  public List<RoutingError> getRoutingErrors() {
    return routingErrors;
  }

  /**
   * Generate empty result with just an error.
   */
  public static RoutingResponse ofError(RoutingError error) {
    return new RoutingResponse(null, null, null, null, List.of(error), new DebugTimingAggregator());
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(RoutingResponse.class)
      .addObj("tripPlan", tripPlan)
      .addObj("nextPageCursor", nextPageCursor)
      .addObj("previousPageCursor", previousPageCursor)
      .addObj("metadata", metadata)
      .addObj("routingErrors", routingErrors)
      .toString();
  }
}
