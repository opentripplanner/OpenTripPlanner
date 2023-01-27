package org.opentripplanner.ext.legacygraphqlapi;

import org.opentripplanner.routing.DefaultRoutingService;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

public class LegacyGraphQLRequestContext {

  private final OtpServerRequestContext serverContext;
  private final DefaultRoutingService routingService;
  private final TransitService transitService;
  private final FareService fareService;

  public LegacyGraphQLRequestContext(
    OtpServerRequestContext serverContext,
    DefaultRoutingService routingService,
    TransitService transitService,
    FareService fareService
  ) {
    this.serverContext = serverContext;
    this.routingService = routingService;
    this.transitService = transitService;
    this.fareService = fareService;
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

  public FareService getFareService() {
    return fareService;
  }
}
