package org.opentripplanner.routing.api.request;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

public interface RoutingService {
  RoutingResponse route(RouteRequest request);

  ViaRoutingResponse route(RouteViaRequest request);

  List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters);

  List<PlaceAtDistance> findClosestPlaces(
    double lat,
    double lon,
    double radiusMeters,
    int maxResults,
    List<TransitMode> filterByModes,
    List<PlaceType> filterByPlaceTypes,
    List<FeedScopedId> filterByStops,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    List<String> filterByBikeParks,
    List<String> filterByCarParks,
    TransitService transitService
  );
}
