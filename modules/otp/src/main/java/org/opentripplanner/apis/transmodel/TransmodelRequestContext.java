package org.opentripplanner.apis.transmodel;

import org.opentripplanner.routing.api.RoutingService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public class TransmodelRequestContext {

  private final OtpServerRequestContext serverContext;
  private final RoutingService routingService;
  private final TransitService transitService;

  public TransmodelRequestContext(
    OtpServerRequestContext serverContext,
    RoutingService routingService,
    TransitService transitService
  ) {
    this.serverContext = serverContext;
    this.routingService = routingService;
    this.transitService = transitService;
  }

  public OtpServerRequestContext getServerContext() {
    return serverContext;
  }

  public RoutingService getRoutingService() {
    return routingService;
  }

  public TransitService getTransitService() {
    return transitService;
  }
}
