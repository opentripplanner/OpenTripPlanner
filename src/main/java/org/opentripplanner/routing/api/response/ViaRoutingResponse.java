package org.opentripplanner.routing.api.response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.model.plan.Itinerary;

/**
 * The response will contain a list of errors and {@link RoutingResponse} for each step in the via
 * searches. It will contain a Map where key is an itinerary and the value is a list of itineraries
 * that part from the key itinerary.
 */
public record ViaRoutingResponse(
  Map<Itinerary, List<Itinerary>> plan,
  List<RoutingResponse> routingResponses,
  List<RoutingError> routingErrors
) {
  /**
   * Map each of the RoutingResponse to its itineraries
   */
  public List<List<Itinerary>> getItineraries() {
    return routingResponses
      .stream()
      .map(RoutingResponse::getTripPlan)
      .map(plan -> plan.itineraries)
      .toList();
  }

  /**
   * Create a list of possible connections between the routingResponses.
   * The response contains three levels of nested lists. The first level is the segment index, i.e.
   * the first list contains the connections around the first via location. The second level
   * represents the nth itinerary of the segment before the via location and the third, innermost,
   * list contains all indices of itineraries after the via that are compatible with the itinerary
   * before the via location.
   */
  public List<List<ViaRoutingResponseConnection>> createConnections() {
    var connectionLists = new ArrayList<List<ViaRoutingResponseConnection>>();

    List<List<Itinerary>> viaJourneys = getItineraries();

    for (int i = 0; i < viaJourneys.size() - 1; i++) {
      var connectionList = new ArrayList<ViaRoutingResponseConnection>();
      connectionLists.add(connectionList);
      List<Itinerary> itineraries = viaJourneys.get(i);
      List<Itinerary> nextItineraries = viaJourneys.get(i + 1);
      for (int j = 0; j < itineraries.size(); j++) {
        Itinerary itinerary = itineraries.get(j);
        var connections = plan.get(itinerary);
        if (connections != null) {
          for (var connection : connections) {
            var index = nextItineraries.indexOf(connection);
            if (index != -1) {
              connectionList.add(new ViaRoutingResponseConnection(j, index));
            }
          }
        }
      }
    }
    return connectionLists;
  }
}
