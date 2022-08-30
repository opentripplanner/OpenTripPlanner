package org.opentripplanner.routing.api.request;

import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.RoutingRequest;

public record RoutingRequestAndPreferences(
  RoutingRequest request,
  RoutingPreferences preferences
) {}
