package org.opentripplanner.routing.api.response;

import java.util.List;
import java.util.Map;
import org.opentripplanner.model.plan.Itinerary;

/**
 * The response will contain a list of errors and {@link RoutingResponse} for each step in the via
 * searches. It will contain a Map where key is an itinerary and the value is a list of itineraries
 * that part from the key itinierary.
 */
public class ViaRoutingResponse {

  private final List<RoutingError> routingErrors;
  private final List<RoutingResponse> routingResponses;
  private final Map<Itinerary, List<Itinerary>> plan;

  public ViaRoutingResponse(
    Map<Itinerary, List<Itinerary>> plan,
    List<RoutingResponse> routingResponses,
    List<RoutingError> routingErrors
  ) {
    this.routingErrors = routingErrors;
    this.routingResponses = routingResponses;
    this.plan = plan;
  }

  public List<RoutingResponse> getRoutingResponses() {
    return routingResponses;
  }

  public Map<Itinerary, List<Itinerary>> getPlan() {
    return this.plan;
  }

  public List<RoutingError> getRoutingErrors() {
    return this.routingErrors;
  }
}
