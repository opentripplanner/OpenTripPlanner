package org.opentripplanner.place;

import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.api.PlaceAtDistance;
import org.opentripplanner.place.api.PlaceType;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * Interface for finding places (stops, rental stations etc.).
 */
@FunctionalInterface
public interface NearbyPlaceFinder {
  /**
   * Search closest places, including stops, bike rental stations, bike and car parking etc, from a
   * given coordinate, extending up to a specified max radius.
   *
   * @param lat                        Origin latitude
   * @param lon                        Origin longitude
   * @param radiusMeters               Search radius from the origin in meters
   * @param maxResults                 Maximum number of results to return within the search
   *                                   radius.
   * @param filterByModes              A list of TransitModes for which to find Stops and
   *                                   PatternAtStops. Use null to disable the filtering.
   * @param filterByPlaceTypes         A list of PlaceTypes to search for. Use null to disable the
   *                                   filtering, and search for all types.
   * @param filterByStops              A list of Stop ids for which to find Stops and
   *                                   PatternAtStops. Use null to disable the filtering.
   * @param filterByRoutes             A list of Route ids used for filtering Stops. Only the stops
   *                                   which are served by the route are returned. Use null to
   *                                   disable the filtering.
   * @param filterByBikeRentalStations A list of VehicleRentalStation ids to use in filtering. Use
   *                                   null to disable the filtering.
   */
  List<PlaceAtDistance> findClosestPlaces(
    double lat,
    double lon,
    double radiusMeters,
    int maxResults,
    List<TransitMode> filterByModes,
    List<PlaceType> filterByPlaceTypes,
    List<FeedScopedId> filterByStops,
    List<FeedScopedId> filterByStations,
    List<FeedScopedId> filterByRoutes,
    List<String> filterByBikeRentalStations,
    List<String> filterByNetwork,
    TransitService transitService
  );
}
