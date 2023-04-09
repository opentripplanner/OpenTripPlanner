package org.opentripplanner.routing.service;

import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.Result;

/**
 * A simple interface to allow modifying a request before routing on it is started.
 * <p>
 * This can be useful if you want to change the start time or any other properties.
 */
@FunctionalInterface
public interface RequestModifier {
  Result<RouteRequest, RoutingError> modify(OtpServerRequestContext context, RouteRequest req);

  /**
   * Default implementation that does nothing.
   */
  RequestModifier NOOP = (context, request) -> Result.success(request);
}
