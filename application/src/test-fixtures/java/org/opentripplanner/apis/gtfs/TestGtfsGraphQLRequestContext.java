package org.opentripplanner.apis.gtfs;

import graphql.schema.GraphQLSchema;
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.transfer.regular.FlexTransferService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;

/**
 * A plain, hand-built {@link GtfsGraphQLRequestContext} for tests that don't need (or want) a
 * Dagger-assembled request scope.
 */
public record TestGtfsGraphQLRequestContext(
  RoutingService routingService,
  TransitService transitService,
  TransitAlertService transitAlertService,
  FlexTransferService transferService,
  RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> regularTransfers,
  FareService fareService,
  VehicleRentalService vehicleRentalService,
  VehicleParkingService vehicleParkingService,
  RealtimeVehicleService realTimeVehicleService,
  GraphQLSchema schema,
  NearbyPlaceFinder nearbyPlaceFinder,
  NearbyStopFinder nearbyStopFinder,
  RouteRequest defaultRouteRequest
) implements GtfsGraphQLRequestContext {}
