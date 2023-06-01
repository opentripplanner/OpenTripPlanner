package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * This class is responsible for filtering the list of access and egress before
 * the transit routing is performed.
 */
public class AccessEgressFilter {

  private final RouteRequest request;

  public AccessEgressFilter(RouteRequest request) {
    this.request = request;
  }

  public Collection<DefaultAccessEgress> filterAccess(Collection<DefaultAccessEgress> list) {
    return filterMinDurationForMode(list, request.journey().modes().accessMode);
  }

  public Collection<DefaultAccessEgress> filterEgress(Collection<DefaultAccessEgress> list) {
    return filterMinDurationForMode(list, request.journey().modes().egressMode);
  }

  /**
   * Filter for the preferred street minimum duration.
   */
  private Collection<DefaultAccessEgress> filterMinDurationForMode(
    Collection<DefaultAccessEgress> input,
    StreetMode streetMode
  ) {
    if (input.isEmpty()) {
      return input;
    }
    // The Routing request may only have one access/egress street-mode set, so we can use it to
    // find the min duration to apply to all access/egress witch is not walking. Walking is the
    // only mode allowed in combination with other modes(the request mode).
    var minDurationNotWalking = request
      .preferences()
      .street()
      .minAccessEgressDuration()
      .valueOf(streetMode);

    if (minDurationNotWalking.isZero()) {
      return input;
    }

    return input
      .stream()
      .filter(it -> it.isWalkOnly() || it.durationInSeconds() >= minDurationNotWalking.toSeconds())
      .toList();
  }
}
