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
  public RoutingService getRoutingService() {
    return factory.routingService();
  }

  @Override
  public TransitService getTransitService() {
    return factory.transitService();
  }

  @Override
  public TransitAlertService getTransitAlertService() {
    return factory.transitAlertService();
  }

  @Nullable
  @Override
  public EmpiricalDelayService getEmpiricalDelayService() {
    return factory.empiricalDelayService();
  }

  @Override
  public RouteRequest getDefaultRouteRequest() {
    return factory.defaultRouteRequest();
  }

  @Override
  public VehicleRentalService getVehicleRentalService() {
    return factory.vehicleRentalService();
  }

  @Override
  public VehicleParkingService getVehicleParkingService() {
    return factory.vehicleParkingService();
  }

  @Override
  public Graph getGraph() {
    return factory.graph();
  }

  @Override
  public RegularTransferService getTransferService() {
    return factory.transferService();
  }

  @Override
  public StreetDetailsService getStreetDetailsService() {
    return factory.streetDetailsService();
  }

  @Override
  public LinkingContextFactory getLinkingContextFactory() {
    return factory.linkingContextFactory();
  }

  @Override
  public StreetLimitationParametersService getStreetLimitationParametersService() {
    return factory.streetLimitationParametersService();
  }

  @Override
  public synchronized NearbyPlaceFinder getNearbyPlaceFinder() {
    if (nearbyPlaceFinder == null) {
      nearbyPlaceFinder = new StreetNearbyPlaceFinder(factory.linkingContextFactory());
    }
    return nearbyPlaceFinder;
  }

  @Override
  public synchronized NearbyStopFinder getNearbyStopFinder() {
    if (nearbyStopFinder == null) {
      nearbyStopFinder = factory.graph().hasStreets
        ? StreetNearbyStopFinder.of(factory.linkingContextFactory()).build()
        : new StraightLineNearbyStopFinder(factory.transitService()::findRegularStopsByBoundingBox);
    }
    return nearbyStopFinder;
  }
}
