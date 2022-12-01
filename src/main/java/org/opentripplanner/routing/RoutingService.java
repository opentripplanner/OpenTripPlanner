package org.opentripplanner.routing;

import java.time.ZoneId;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.via.ViaRoutingWorker;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteViaRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.ViaRoutingResponse;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.services.RealtimeVehiclePositionService;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

// TODO VIA: 2022-08-29 javadocs
/**
 * Entry point for requests towards the routing API.
 */
public class RoutingService implements org.opentripplanner.routing.api.request.RoutingService {

  private final OtpServerRequestContext serverContext;
  private final Graph graph;

  private final ZoneId timeZone;

  private final GraphFinder graphFinder;

  public RoutingService(OtpServerRequestContext serverContext) {
    this.serverContext = serverContext;
    this.graph = serverContext.graph();
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

  public RealtimeVehiclePositionService getVehiclePositionService() {
    return this.graph.getVehiclePositionService();
  }

  /** {@link Graph#getVehicleRentalService()} */
  public VehicleRentalService getVehicleRentalService() {
    return this.graph.getVehicleRentalService();
  }

  /** {@link Graph#getVehicleParkingService()} */
  public VehicleParkingService getVehicleParkingService() {
    return this.graph.getVehicleParkingService();
  }

  /** {@link GraphFinder#findClosestStops(Coordinate, double)} */
  public List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters) {
    return this.graphFinder.findClosestStops(coordinate, radiusMeters);
  }

  /**
   * {@link GraphFinder#findClosestPlaces(double, double, double, int, List, List, List, List, List, TransitService)}
   */
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
