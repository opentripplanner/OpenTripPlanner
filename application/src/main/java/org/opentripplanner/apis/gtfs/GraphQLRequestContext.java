package org.opentripplanner.apis.gtfs;

import graphql.schema.GraphQLSchema;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public record GraphQLRequestContext(
  RoutingService routingService,
  TransitService transitService,
  FareService fareService,
  VehicleRentalService vehicleRentalService,
  VehicleParkingService vehicleParkingService,
  RealtimeVehicleService realTimeVehicleService,
  GraphQLSchema schema,
  GraphFinder graphFinder,
  RouteRequest defaultRouteRequest
) {
  public static GraphQLRequestContext ofServerContext(OtpServerRequestContext context) {
    return new GraphQLRequestContext(
      context.routingService(),
      context.transitService(),
      context.fareService(),
      context.vehicleRentalService(),
      context.vehicleParkingService(),
      context.realtimeVehicleService(),
      context.schema(),
      context.graphFinder(),
      context.defaultRouteRequest()
    );
  }

  /**
   * Returns a clone of the default route request. The clone is necessary because one HTTP
   * request can lead to several GraphQL queries, for example through batch or alias queries.
   */
  @Override
  public RouteRequest defaultRouteRequest() {
    return defaultRouteRequest.clone();
  }
}
