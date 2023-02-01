package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;

public interface RoutingService {
  RoutingResponse route(RouteRequest request);

  ViaRoutingResponse route(RouteViaRequest request);
}
