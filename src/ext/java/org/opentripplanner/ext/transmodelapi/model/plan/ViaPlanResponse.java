package org.opentripplanner.ext.transmodelapi.model.plan;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.response.RoutingError;

public record ViaPlanResponse(
  List<List<Itinerary>> viaJourneys,
  List<List<List<Integer>>> viaJourneyConnections,
  List<RoutingError> routingErrors
) {
  /**
   * Static method to create a failed response
   */
  public static ViaPlanResponse failed(RoutingError routingError) {
    return new ViaPlanResponse(List.of(), List.of(), List.of(routingError));
  }
}
