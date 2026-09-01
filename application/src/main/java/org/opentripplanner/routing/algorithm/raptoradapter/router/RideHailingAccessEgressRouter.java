package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.ridehailing.RideHailingAccessShifter;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;

/**
 * Decorates another {@link AccessEgressRouter} (the default, street-based router) by
 * time-shifting its access results so that they only start at the time when a ride-hailing
 * vehicle can actually arrive to pick up passengers. Egress results are passed through
 * unchanged.
 */
public class RideHailingAccessEgressRouter implements AccessEgressRouter {

  private final AccessEgressRouter delegate;
  private final List<RideHailingService> rideHailingServices;

  public RideHailingAccessEgressRouter(
    AccessEgressRouter delegate,
    List<RideHailingService> rideHailingServices
  ) {
    this.delegate = delegate;
    this.rideHailingServices = rideHailingServices;
  }

  @Override
  public Collection<? extends RoutingAccessEgress> route(AccessEgressRouterContext context) {
    var results = delegate.route(context);
    return RideHailingAccessShifter.shiftAccesses(
      context.type().isAccess(),
      List.copyOf(results),
      rideHailingServices,
      context.routeRequest(),
      Instant.now()
    );
  }
}
