package org.opentripplanner.ext.transmodelapi.model.plan;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;

public record ViaPlanResponse(
  List<List<Itinerary>> viaJourneys,
  List<List<List<Integer>>> viaJourneyConnections,
  List<RoutingError> routingErrors
) {
  public static ViaPlanResponse of(ViaRoutingResponse res) {
    return new ViaPlanResponse(res.getItineraries(), res.createConnections(), res.routingErrors());
  }
  /**
   * Static method to create a failed response
   */
  public static ViaPlanResponse failed(RoutingError routingError) {
    return new ViaPlanResponse(List.of(), List.of(), List.of(routingError));
  }
}
