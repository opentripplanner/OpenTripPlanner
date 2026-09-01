package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.Duration;
import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.StreetAccessEgressFinder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.street.model.StreetMode;

/**
 * The default, street-based access/egress router.
 */
public class DefaultAccessEgressRouter implements AccessEgressRouter {

  private final AccessEgressMapper accessEgressMapper;

  public DefaultAccessEgressRouter(AccessEgressMapper accessEgressMapper) {
    this.accessEgressMapper = accessEgressMapper;
  }

  @Override
  public Collection<? extends RoutingAccessEgress> route(AccessEgressRouterContext context) {
    StreetMode mode = context.streetRequest().mode();
    var accessEgressPreferences = context.routeRequest().preferences().street().accessEgress();
    Duration durationLimit = accessEgressPreferences.maxDuration().valueOf(mode);
    int stopCountLimit = accessEgressPreferences.maxStopCountLimit().limitForMode(mode);

    var nearbyStops = StreetAccessEgressFinder.findAccessEgresses(
      context.routeRequest(),
      mode,
      context.dataOverlayContext(),
      context.type(),
      durationLimit,
      stopCountLimit,
      context.linkingContext()
    );
    return accessEgressMapper.mapNearbyStops(nearbyStops);
  }
}
