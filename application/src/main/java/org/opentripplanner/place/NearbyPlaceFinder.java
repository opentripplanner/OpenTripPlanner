package org.opentripplanner.place;

import java.util.List;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.api.PlaceAtDistance;
import org.opentripplanner.place.api.PlaceType;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.service.TransitService;

/**
 * Functional interface for finding nearby places such as stops, rental stations, and parking.
 */
@FunctionalInterface
public interface NearbyPlaceFinder {

  /**
   * Search for the closest places (stops, bike rental stations, bike and car parking, etc.)
   * from a given coordinate, within a specified radius.
   *
   * @param lat                  Origin latitude
   * @param lon                  Origin longitude
   * @param radiusMeters         Search radius from the origin in meters
   * @param maxResults           Maximum number of results to return
   * @param filterByModes        Transit modes to filter stops and patterns. Use {@code null} for no filtering.
   * @param filterByPlaceTypes   Place types to include in the search. Use {@code null} for all types.
   * @param filterByStops        Specific stop IDs to include. Use {@code null} for no filtering.
   * @param filterByStations     Specific station IDs to include. Use {@code null} for no filtering.
   * @param filterByRoutes       Route IDs to filter stops served by those routes. Use {@code null} for no filtering.
   * @param filterByRentalStations Vehicle rental station IDs to filter. Use {@code null} for no filtering.
   * @param filterByNetworks     Rental networks to filter. Use {@code null} for no filtering.
   * @param transitService       Transit service reference for querying transit data
   *
   * @return A list of nearby places within the given radius, subject to filters
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
    List<String> filterByRentalStations,
    List<String> filterByNetworks,
    TransitService transitService
  );
}
