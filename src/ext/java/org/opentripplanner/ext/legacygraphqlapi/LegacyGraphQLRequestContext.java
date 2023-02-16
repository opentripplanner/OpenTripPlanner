package org.opentripplanner.ext.legacygraphqlapi;

import javax.annotation.Nonnull;
import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.service.vehiclepositions.VehiclePositionService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLRequestContext {

  private final OtpServerRequestContext serverContext;
  private final RoutingService routingService;
  private final TransitService transitService;
  private final FareService fareService;

  public LegacyGraphQLRequestContext(
    OtpServerRequestContext serverContext,
    RoutingService routingService,
    TransitService transitService,
    FareService fareService
  ) {
    this.serverContext = serverContext;
    this.routingService = routingService;
    this.transitService = transitService;
    this.fareService = fareService;
  }

  @Nonnull
  public RoutingService routingService() {
    return routingService;
  }

  @Nonnull
  public TransitService transitService() {
    return transitService;
  }

  @Nonnull
  public FareService fareService() {
    return fareService;
  }

  @Nonnull
  public VehicleParkingService vehicleParkingService() {
    return serverContext.graph().getVehicleParkingService();
  }

  @Nonnull
  public VehicleRentalService vehicleRentalService() {
    return serverContext.graph().getVehicleRentalService();
  }

  @Nonnull
  public VehiclePositionService vehiclePositionService() {
    return serverContext.vehiclePositionService();
  }

  @Nonnull
  public RouteRequest defaultRouteRequest() {
    return serverContext.defaultRouteRequest().clone();
  }

  @Nonnull
  public GraphFinder graphFinder() {
    return serverContext.graphFinder();
  }
}
