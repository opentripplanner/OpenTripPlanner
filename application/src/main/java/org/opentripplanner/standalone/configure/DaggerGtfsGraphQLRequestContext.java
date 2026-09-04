package org.opentripplanner.standalone.configure;

import graphql.schema.GraphQLSchema;
import org.opentripplanner.apis.gtfs.GtfsGraphQLRequestContext;
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.place.placefinder.StreetNearbyPlaceFinder;
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
 * Production {@link GtfsGraphQLRequestContext}: every accessor delegates to the enclosing
 * {@link RequestScopedFactory}.
 */
final class DaggerGtfsGraphQLRequestContext implements GtfsGraphQLRequestContext {

  private final RequestScopedFactory factory;
  private NearbyPlaceFinder nearbyPlaceFinder;
  private NearbyStopFinder nearbyStopFinder;

  DaggerGtfsGraphQLRequestContext(RequestScopedFactory factory) {
    this.factory = factory;
  }

  @Override
  public RoutingService routingService() {
    return factory.routingService();
  }

  @Override
  public TransitService transitService() {
    return factory.transitService();
  }

  @Override
  public TransitAlertService transitAlertService() {
    return factory.transitAlertService();
  }

  @Override
  public RegularTransferService transferService() {
    return factory.transferService();
  }

  @Override
  public FareService fareService() {
    return factory.fareService();
  }

  @Override
  public VehicleRentalService vehicleRentalService() {
    return factory.vehicleRentalService();
  }

  @Override
  public VehicleParkingService vehicleParkingService() {
    return factory.vehicleParkingService();
  }

  @Override
  public RealtimeVehicleService realTimeVehicleService() {
    return factory.realtimeVehicleService();
  }

  @Override
  public GraphQLSchema schema() {
    return factory.gtfsSchema();
  }

  @Override
  public synchronized NearbyPlaceFinder nearbyPlaceFinder() {
    if (nearbyPlaceFinder == null) {
      nearbyPlaceFinder = new StreetNearbyPlaceFinder(factory.linkingContextFactory());
    }
    return nearbyPlaceFinder;
  }

  @Override
  public synchronized NearbyStopFinder nearbyStopFinder() {
    if (nearbyStopFinder == null) {
      nearbyStopFinder = factory.graph().hasStreets
        ? StreetNearbyStopFinder.of(factory.linkingContextFactory()).build()
        : new StraightLineNearbyStopFinder(factory.transitService()::findRegularStopsByBoundingBox);
    }
    return nearbyStopFinder;
  }

  @Override
  public RouteRequest defaultRouteRequest() {
    return factory.defaultRouteRequest();
  }
}
