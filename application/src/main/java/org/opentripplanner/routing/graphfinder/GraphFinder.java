package org.opentripplanner.routing.graphfinder;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.street.model.PropulsionType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;

/**
 * Common interface between different types of GraphFinders, currently two types exist, one which
 * traverses the street network, and one which doesn't.
 */
public interface GraphFinder {
  /**
   * Get a new GraphFinder instance depending on whether the graph includes a street network or
   * not.
   */
  static GraphFinder getInstance(
    boolean graphHasStreets,
    StopResolver stopResolver,
    Function<Envelope, Collection<RegularStop>> queryNearbyStops,
    LinkingContextFactory linkingContextFactory
  ) {
    return graphHasStreets
      ? new StreetGraphFinder(linkingContextFactory, stopResolver)
      : new DirectGraphFinder(queryNearbyStops);
  }

  /**
   * Search closest stops from a given coordinate, extending up to a specified max radius.
   *
   * @param coordinate   Origin
   * @param radiusMeters Search radius from the origin in meters
   */
  List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters);

  /**
   * Search closest places, including stops, bike rental stations, bike and car parking etc, from a
   * given coordinate, extending up to a specified max radius.
   *
   * @param lat                           Origin latitude
   * @param lon                           Origin longitude
   * @param radiusMeters                  Search radius from the origin in meters
   * @param maxResults                    Maximum number of results to return within the search
   *                                      radius.
   * @param filterByModes                 A list of TransitModes for which to find Stops and
   *                                      PatternAtStops. Use null to disable the filtering.
   * @param filterByPlaceTypes            A list of PlaceTypes to search for. Use null to disable
   *                                      the filtering, and search for all types.
   * @param filterByStops                 A list of Stop ids for which to find Stops and
   *                                      PatternAtStops. Use null to disable the filtering.
   * @param filterByStations              A list of Station ids for which to use in filtering. Use
   *                                      null to disable the filtering.
   * @param filterByRoutes                A list of Route ids used for filtering Stops. Only the
   *                                      stops which are served by the route are returned. Use null
   *                                      to disable the filtering.
   * @param filterByBikeRentalStations    A list of VehicleRentalStation ids to use in filtering.
   *                                      Use null to disable the filtering.
   * @param filterByVehicleFormFactor     A list of RentalFormFactors to use in filtering. Use null
   *                                      to disable the filtering.
   * @param filterByVehiclePropulsionType A list of PropulsionTypes to use in filtering. Use null to
   *                                      disable the filtering.
   * @param filterByNetwork               A list of networks to use in the filtering. Use null to
   *                                      disable the filtering.
   * @param transitService                A TransitService used in finding information about the
   *                                      various places.
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
    List<RentalFormFactor> filterByVehicleFormFactor,
    List<PropulsionType> filterByVehiclePropulsionType,
    List<String> filterByNetwork,
    TransitService transitService
  );
}
