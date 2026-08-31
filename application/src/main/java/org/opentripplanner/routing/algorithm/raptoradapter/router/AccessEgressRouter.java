package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;

/**
 * A single router that produces access/egress results for the transit search.
 */
public interface AccessEgressRouter {
  Collection<? extends RoutingAccessEgress> route(AccessEgressRouterContext context);
}
