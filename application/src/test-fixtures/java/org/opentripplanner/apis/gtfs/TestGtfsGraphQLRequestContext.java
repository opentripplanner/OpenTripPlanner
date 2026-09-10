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
 * A plain, hand-built {@link GtfsGraphQLRequestContext} for tests that don't need (or want) a
 * Dagger-assembled request scope.
 */
public record TestGtfsGraphQLRequestContext(
  RoutingService routingService,
  TransitService transitService,
  TransitAlertService transitAlertService,
  RegularTransferService transferService,
  FareService fareService,
  VehicleRentalService vehicleRentalService,
  VehicleParkingService vehicleParkingService,
  RealtimeVehicleService realTimeVehicleService,
  GraphQLSchema schema,
  NearbyPlaceFinder nearbyPlaceFinder,
  NearbyStopFinder nearbyStopFinder,
  RouteRequest defaultRouteRequest
) implements GtfsGraphQLRequestContext {}
