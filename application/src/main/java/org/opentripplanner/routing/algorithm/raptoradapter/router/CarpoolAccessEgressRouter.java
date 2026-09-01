package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.ZonedDateTime;
import java.util.Collection;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Access/egress router for carpooling.
 */
public class CarpoolAccessEgressRouter implements AccessEgressRouter {

  private final CarpoolingService carpoolingService;
  private final TransitServiceResolver transitServiceResolver;
  private final ZonedDateTime transitSearchTimeZero;

  public CarpoolAccessEgressRouter(
    CarpoolingService carpoolingService,
    TransitServiceResolver transitServiceResolver,
    ZonedDateTime transitSearchTimeZero
  ) {
    this.carpoolingService = carpoolingService;
    this.transitServiceResolver = transitServiceResolver;
    this.transitSearchTimeZero = transitSearchTimeZero;
  }

  @Override
  public Collection<? extends RoutingAccessEgress> route(AccessEgressRouterContext context) {
    return carpoolingService.routeAccessEgress(
      context.routeRequest(),
      context.streetRequest(),
      context.type(),
      transitServiceResolver,
      transitSearchTimeZero
    );
  }
}
