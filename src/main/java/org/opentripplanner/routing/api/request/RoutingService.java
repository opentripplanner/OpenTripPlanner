package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;

public interface RoutingService {
  RoutingResponse route(RouteRequest request);
  RoutingResponse route(RouteViaRequest request, RoutingPreferences preferences);
}
