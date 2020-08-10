package org.opentripplanner.ext.legacygraphqlapi;

import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.standalone.server.Router;

public class LegacyGraphQLRequestContext {
  private final Router router;
  private final RoutingService routingService;

  public LegacyGraphQLRequestContext(Router router, RoutingService routingService) {
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
