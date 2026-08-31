package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.ridehailing.RideHailingAccessShifter;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.StreetAccessEgressFinder;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.StreetMode;

/**
 * The default, street-based access/egress router.
 */
public class DefaultAccessEgressRouter implements AccessEgressRouter {

  private final AccessEgressMapper accessEgressMapper;
  private final List<RideHailingService> rideHailingServices;
  private final RouteRequest request;

  public DefaultAccessEgressRouter(
    AccessEgressMapper accessEgressMapper,
    List<RideHailingService> rideHailingServices,
    RouteRequest request
  ) {
    this.accessEgressMapper = accessEgressMapper;
    this.rideHailingServices = rideHailingServices;
    this.request = request;
  }

  @Override
  public Collection<? extends RoutingAccessEgress> route(AccessEgressRouterContext context) {
    StreetMode mode = context.streetRequest().mode();
    var accessEgressPreferences = context.accessRequest().preferences().street().accessEgress();
    Duration durationLimit = accessEgressPreferences.maxDuration().valueOf(mode);
    int stopCountLimit = accessEgressPreferences.maxStopCountLimit().limitForMode(mode);

    var nearbyStops = StreetAccessEgressFinder.findAccessEgresses(
      context.accessRequest(),
      mode,
      context.dataOverlayContext(),
      context.type(),
      durationLimit,
      stopCountLimit,
      context.linkingContext()
    );
    var accessEgresses = accessEgressMapper.mapNearbyStops(nearbyStops);
    return timeshiftRideHailing(context.streetRequest(), context.type(), accessEgresses);
  }

  /**
   * Given a list of {@code results} shift the access ones that contain driving so that they only
   * start at the time when the ride hailing vehicle can actually be there to pick up passengers.
   * <p>
   * If there are accesses/egresses with only walking, then they remain unchanged.
   * <p>
   * This method is a good candidate to be moved to the access/egress filter chain when that has
   * been added.
   */
  private List<RoutingAccessEgress> timeshiftRideHailing(
    StreetRequest streetRequest,
    AccessEgressType type,
    List<RoutingAccessEgress> accessEgressList
  ) {
    if (streetRequest.mode() != StreetMode.CAR_HAILING) {
      return accessEgressList;
    }
    return RideHailingAccessShifter.shiftAccesses(
      type.isAccess(),
      accessEgressList,
      rideHailingServices,
      request,
      Instant.now()
    );
  }
}
