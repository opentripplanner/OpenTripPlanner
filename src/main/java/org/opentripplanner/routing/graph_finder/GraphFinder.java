package org.opentripplanner.routing.graph_finder;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.graph.Graph;

import java.util.List;

public interface GraphFinder {
  List<StopAtDistance> findClosestStops(double lat, double lon, double radiusMeters);

  List<PlaceAtDistance> findClosestPlaces(
      double lat, double lon, double radiusMeters, int maxResults, List<TransitMode> filterByModes,
      List<PlaceType> filterByPlaceTypes, List<FeedScopedId> filterByStops,
      List<FeedScopedId> filterByRoutes, List<String> filterByBikeRentalStations,
      List<String> filterByBikeParks, List<String> filterByCarParks, RoutingService routingService
  );

  static GraphFinder getInstance(Graph graph) {
    return graph.hasStreets ? new StreetGraphFinder(graph) : new DirectGraphFinder(graph);
  }
}
