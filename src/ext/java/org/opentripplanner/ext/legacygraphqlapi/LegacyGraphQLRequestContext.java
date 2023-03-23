package org.opentripplanner.ext.legacygraphqlapi;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public record LegacyGraphQLRequestContext(
  RoutingService routingService,
  TransitService transitService,
  FareService fareService,
  VehicleParkingService vehicleParkingService,
  VehicleRentalService vehicleRentalService,
  VehiclePositionService vehiclePositionService,
  GraphFinder graphFinder,
  RouteRequest defaultRouteRequest
) {
  public static LegacyGraphQLRequestContext ofServerContext(OtpServerRequestContext context) {
    return new LegacyGraphQLRequestContext(
      context.routingService(),
      context.transitService(),
      context.graph().getFareService(),
      context.graph().getVehicleParkingService(),
      context.vehicleRentalService(),
      context.vehiclePositionService(),
      context.graphFinder(),
      context.defaultRouteRequest()
    );
  }

  /**
   * Returns a clone of the default route request. The clone is necessary because one HTTP
   * request can lead to several GraphQL queries, for example through batch or alias queries.
   */
  @Nonnull
  @Override
  public RouteRequest defaultRouteRequest() {
    return defaultRouteRequest.clone();
  }
}
