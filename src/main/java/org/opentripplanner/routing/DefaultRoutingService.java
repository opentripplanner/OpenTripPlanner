package org.opentripplanner.routing;

import java.time.ZoneId;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.via.ViaRoutingWorker;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.request.RoutingService;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

// TODO VIA: 2022-08-29 javadocs
/**
 * Entry point for requests towards the routing API.
 */
public class DefaultRoutingService implements RoutingService {

  private final OtpServerRequestContext serverContext;

  private final ZoneId timeZone;

  private final GraphFinder graphFinder;

  public DefaultRoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.timeZone = serverContext.transitService().getTimeZone();
    this.graphFinder = serverContext.graphFinder();
  }

  @Override
  public RoutingResponse route(RouteRequest request) {
    RoutingWorker worker = new RoutingWorker(serverContext, request, timeZone);
    return worker.route();
  }

  @Override
  public ViaRoutingResponse route(RouteViaRequest request) {
    var viaRoutingWorker = new ViaRoutingWorker(
      request,
      req ->
        new RoutingWorker(serverContext, req, serverContext.transitService().getTimeZone()).route()
    );
    return viaRoutingWorker.route();
  }

  /** {@link GraphFinder#findClosestStops(Coordinate, double)} */
  @Override
  public List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters) {
    return this.graphFinder.findClosestStops(coordinate, radiusMeters);
  }

  /**
   * {@link GraphFinder#findClosestPlaces(double, double, double, int, List, List, List, List, List, TransitService)}
   */
  @Override
  public List<PlaceAtDistance> findClosestPlaces(
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
  ) {
    return this.graphFinder.findClosestPlaces(
        lat,
        lon,
        radiusMeters,
        maxResults,
        filterByModes,
        filterByPlaceTypes,
        filterByStops,
        filterByRoutes,
        filterByBikeRentalStations,
        transitService
      );
  }
}
