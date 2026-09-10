package org.opentripplanner.standalone.configure;

import javax.annotation.Nullable;
import org.opentripplanner.apis.transmodel.TransmodelGraphQLRequestContext;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayService;
import org.opentripplanner.place.NearbyPlaceFinder;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StraightLineNearbyStopFinder;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.place.placefinder.StreetNearbyPlaceFinder;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehicleparking.VehicleParkingService;
import org.opentripplanner.service.vehiclerental.VehicleRentalService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Production {@link TransmodelGraphQLRequestContext}: every accessor delegates to the enclosing
 * {@link RequestScopedFactory}.
 */
final class DaggerTransmodelGraphQLRequestContext implements TransmodelGraphQLRequestContext {

  private final RequestScopedFactory factory;
  private NearbyPlaceFinder nearbyPlaceFinder;
  private NearbyStopFinder nearbyStopFinder;

  DaggerTransmodelGraphQLRequestContext(RequestScopedFactory factory) {
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

  @Nullable
  @Override
  public EmpiricalDelayService empiricalDelayService() {
    return factory.empiricalDelayService();
  }

  @Override
  public RouteRequest defaultRouteRequest() {
    return factory.defaultRouteRequest();
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
  public Graph graph() {
    return factory.graph();
  }

  @Override
  public RegularTransferService transferService() {
    return factory.transferService();
  }

  @Override
  public StreetDetailsService streetDetailsService() {
    return factory.streetDetailsService();
  }

  @Override
  public LinkingContextFactory linkingContextFactory() {
    return factory.linkingContextFactory();
  }

  @Override
  public StreetLimitationParametersService streetLimitationParametersService() {
    return factory.streetLimitationParametersService();
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
}
