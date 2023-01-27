package org.opentripplanner.ext.transmodelapi;

import org.opentripplanner.routing.DefaultRoutingService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public class TransmodelRequestContext {

  private final OtpServerRequestContext serverContext;
  private final DefaultRoutingService routingService;
  private final TransitService transitService;

  public TransmodelRequestContext(
    OtpServerRequestContext serverContext,
    DefaultRoutingService routingService,
    TransitService transitService
  ) {
    this.serverContext = serverContext;
    this.routingService = routingService;
    this.transitService = transitService;
  }

  public OtpServerRequestContext getServerContext() {
    return serverContext;
  }

  public DefaultRoutingService getRoutingService() {
    return routingService;
  }

  public TransitService getTransitService() {
    return transitService;
  }
}
