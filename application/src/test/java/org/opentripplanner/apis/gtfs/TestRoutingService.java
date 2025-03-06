package org.opentripplanner.apis.gtfs;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.framework.DebugTimingAggregator;

public class TestRoutingService implements RoutingService {

  private final Instant instant = OffsetDateTime.parse("2023-01-27T21:08:35+01:00").toInstant();
  private final RoutingResponse routingResponse;

  public TestRoutingService(List<Itinerary> results) {
    routingResponse = new RoutingResponse(
      new TripPlan(PlanTestConstants.A, PlanTestConstants.B, instant, results),
      null,
      null,
      null,
      List.of(),
      new DebugTimingAggregator()
    );
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    return routingResponse;
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    throw new RuntimeException("Not implemented yet!");
  }
}
