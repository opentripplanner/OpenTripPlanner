package org.opentripplanner.routing.api;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;

public interface RoutingService {
  RoutingResponse route(RouteRequest request);

  ViaRoutingResponse route(RouteViaRequest request);
}
