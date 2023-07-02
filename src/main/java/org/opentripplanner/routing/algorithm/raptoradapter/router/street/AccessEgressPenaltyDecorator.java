package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;

/**
 * This class is responsible for filtering the list of access and egress before
 * the transit routing is performed.
 */
public class AccessEgressPenaltyDecorator {

  private final RouteRequest request;

  public AccessEgressPenaltyDecorator(RouteRequest request) {
    this.request = request;
  }

  public Collection<DefaultAccessEgress> decorateAccess(Collection<DefaultAccessEgress> list) {
    return decorate(list, request.journey().modes().accessMode);
  }

  public Collection<DefaultAccessEgress> decorateEgress(Collection<DefaultAccessEgress> list) {
    return decorate(list, request.journey().modes().egressMode);
  }

  /**
   * Decorate each access-egress with a penalty according to the specified street-mode.
   */
  private Collection<DefaultAccessEgress> decorate(
    Collection<DefaultAccessEgress> input,
    StreetMode requestedMode
  ) {
    if (input.isEmpty()) {
      return input;
    }
    // The routing request only have one access/egress street-mode set, so we can use it to
    // find the "actual street-mode" of the access/egress. We assume the mode is WALK if all edges
    // in AStar is WALK, if not we assume the mode is the mode set in the request input.
    var penaltyWalking = request
      .preferences()
      .street()
      .accessEgressPenalty()
      .valueOf(StreetMode.WALK);

    if (requestedMode == StreetMode.WALK) {
      return penaltyWalking.isEmpty()
        ? input
        : input
          .stream()
          .map(it -> it.withPenalty(penaltyWalking.calculate(it.durationInSeconds())))
          .toList();
    }

    var penaltyRequestedMode = request
      .preferences()
      .street()
      .accessEgressPenalty()
      .valueOf(requestedMode);

    if (penaltyRequestedMode.isEmpty() && penaltyWalking.isEmpty()) {
      return input;
    }

    return input
      .stream()
      .map(it ->
        it.isWalkOnly()
          ? it.withPenalty(penaltyWalking.calculate(it.durationInSeconds()))
          : it.withPenalty(penaltyRequestedMode.calculate(it.durationInSeconds()))
      )
      .toList();
  }
}
