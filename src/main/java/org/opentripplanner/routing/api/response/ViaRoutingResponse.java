package org.opentripplanner.routing.api.response;

import java.util.List;
import java.util.Map;
import org.opentripplanner.model.plan.Itinerary;

/**
 * The response will contain a list of errors and {@link RoutingResponse} for each step in the via
 * searches. It will contain a Map where key is an itinerary and the value is a list of itineraries
 * that part from the key itinierary.
 */
public record ViaRoutingResponse(
  Map<Itinerary, List<Itinerary>> plan,
  List<RoutingResponse> routingResponses,
  List<RoutingError> routingErrors
) {}
