package org.opentripplanner.routing.service;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.Result;

@FunctionalInterface
interface RequestModifier {
  Result<RouteRequest, RoutingError> modify(OtpServerRequestContext context, RouteRequest req);

  RequestModifier NOOP = (context, request) -> Result.success(request);
}
