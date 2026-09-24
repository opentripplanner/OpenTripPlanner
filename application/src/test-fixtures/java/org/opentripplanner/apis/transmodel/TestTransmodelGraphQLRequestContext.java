package org.opentripplanner.apis.transmodel;

import javax.annotation.Nullable;
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
 * A plain, hand-built {@link TransmodelGraphQLRequestContext} for tests that don't need (or want) a
 * Dagger-assembled request scope.
 */
public class TestTransmodelGraphQLRequestContext implements TransmodelGraphQLRequestContext {

  private final RoutingService routingService;
  private final TransitService transitService;
  private final TransitAlertService transitAlertService;
  private final @Nullable EmpiricalDelayService empiricalDelayService;
  private final RouteRequest defaultRouteRequest;
  private final VehicleRentalService vehicleRentalService;
  private final VehicleParkingService vehicleParkingService;
  private final Graph graph;
  private final RegularTransferService transferService;
  private final StreetDetailsService streetDetailsService;
  private final LinkingContextFactory linkingContextFactory;
  private final StreetLimitationParametersService streetLimitationParametersService;

  public TestTransmodelGraphQLRequestContext(
    RoutingService routingService,
    TransitService transitService,
    @Nullable TransitAlertService transitAlertService,
    @Nullable EmpiricalDelayService empiricalDelayService,
    RouteRequest defaultRouteRequest,
    VehicleRentalService vehicleRentalService,
    VehicleParkingService vehicleParkingService,
    Graph graph,
    RegularTransferService transferService,
    StreetDetailsService streetDetailsService,
    LinkingContextFactory linkingContextFactory,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    this.routingService = routingService;
    this.transitService = transitService;
    this.transitAlertService = transitAlertService;
    this.empiricalDelayService = empiricalDelayService;
    this.defaultRouteRequest = defaultRouteRequest;
    this.vehicleRentalService = vehicleRentalService;
    this.vehicleParkingService = vehicleParkingService;
    this.graph = graph;
    this.transferService = transferService;
    this.streetDetailsService = streetDetailsService;
    this.linkingContextFactory = linkingContextFactory;
    this.streetLimitationParametersService = streetLimitationParametersService;
  }

  @Override
  public RoutingService routingService() {
    return routingService;
  }

  @Override
  public TransitService transitService() {
    return transitService;
  }

  @Nullable
  @Override
  public TransitAlertService transitAlertService() {
    return transitAlertService;
  }

  @Nullable
  @Override
  public EmpiricalDelayService empiricalDelayService() {
    return empiricalDelayService;
  }

  @Override
  public RouteRequest defaultRouteRequest() {
    return defaultRouteRequest;
  }

  @Override
  public VehicleRentalService vehicleRentalService() {
    return vehicleRentalService;
  }

  @Override
  public VehicleParkingService vehicleParkingService() {
    return vehicleParkingService;
  }

  @Override
  public Graph graph() {
    return graph;
  }

  @Override
  public RegularTransferService transferService() {
    return transferService;
  }

  @Override
  public StreetDetailsService streetDetailsService() {
    return streetDetailsService;
  }

  @Override
  public LinkingContextFactory linkingContextFactory() {
    return linkingContextFactory;
  }

  @Override
  public StreetLimitationParametersService streetLimitationParametersService() {
    return streetLimitationParametersService;
  }

  @Override
  public NearbyPlaceFinder nearbyPlaceFinder() {
    return new StreetNearbyPlaceFinder(linkingContextFactory);
  }

  @Override
  public NearbyStopFinder nearbyStopFinder() {
    return graph.hasStreets
      ? StreetNearbyStopFinder.of(linkingContextFactory).build()
      : new StraightLineNearbyStopFinder(transitService::findRegularStopsByBoundingBox);
  }
}
