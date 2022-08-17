package org.opentripplanner.routing.api.request.refactor;

import org.opentripplanner.routing.api.request.refactor.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.refactor.request.RouteRequest;
import org.opentripplanner.routing.api.request.refactor.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;

public interface RoutingService {

  RoutingResponse route(RouteRequest request, RoutingPreferences preferences);
  RoutingResponse route(RouteViaRequest request, RoutingPreferences preferences);
}
