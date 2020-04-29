package org.opentripplanner.ext.transmodelapi;

import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.Router;

public class TransmodelRequestContext {
  private final Router router;
  private final RoutingService routingService;

  public TransmodelRequestContext(Router router, RoutingService routingService) {
    this.router = router;
    this.routingService = routingService;
  }

  public Router getRouter() {
    return router;
  }

  public RoutingService getRoutingService() {
    return routingService;
  }
}
