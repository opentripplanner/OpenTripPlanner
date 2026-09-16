package org.opentripplanner.apis.gtfs;

import graphql.schema.GraphQLSchema;
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * The per-HTTP-request context threaded through the GTFS GraphQL API's data fetchers, bundling
 * the services a query may need (routing, transit, fares, realtime vehicles, ...).
 * <p>
 * Implementations should resolve these dependencies lazily rather than up front: the production
 * implementation delegates to the request-scoped Dagger component, so a service is only
 * constructed if some data fetcher actually asks for it during that request. This interface also
 * lets tests substitute a plain, hand-built implementation (e.g. {@code
 * TestGtfsGraphQLRequestContext}) without needing a Dagger component at all.
 */
public interface GtfsGraphQLRequestContext {
  RoutingService routingService();

  TransitService transitService();

  TransitAlertService transitAlertService();

  RegularTransferService transferService();

  FareService fareService();

  VehicleRentalService vehicleRentalService();

  VehicleParkingService vehicleParkingService();

  RealtimeVehicleService realTimeVehicleService();

  GraphQLSchema schema();

  NearbyPlaceFinder nearbyPlaceFinder();

  NearbyStopFinder nearbyStopFinder();

  /**
   * Returns a clone of the default route request. The clone is necessary because one HTTP
   * request can lead to several GraphQL queries, for example through batch or alias queries.
   */
  RouteRequest defaultRouteRequest();
}
