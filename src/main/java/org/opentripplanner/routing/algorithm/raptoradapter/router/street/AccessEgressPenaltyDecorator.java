package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.TimeAndCostPenaltyForEnum;

/**
 * This class is responsible for filtering the list of access and egress before
 * the transit routing is performed.
 */
public class AccessEgressPenaltyDecorator {

  private final StreetMode accessMode;
  private final StreetMode egressMode;
  private final TimeAndCostPenaltyForEnum<StreetMode> penalty;

  public AccessEgressPenaltyDecorator(
    StreetMode accessMode,
    StreetMode egressMode,
    TimeAndCostPenaltyForEnum<StreetMode> penalty
  ) {
    this.accessMode = accessMode;
    this.egressMode = egressMode;
    this.penalty = penalty;
  }

  public Collection<DefaultAccessEgress> decorateAccess(Collection<DefaultAccessEgress> list) {
    return decorate(list, accessMode);
  }

  public Collection<DefaultAccessEgress> decorateEgress(Collection<DefaultAccessEgress> list) {
    return decorate(list, egressMode);
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
    var penaltyWalking = penalty.valueOf(StreetMode.WALK);

    // The routing request only has ONE access/egress street-mode set, So, if it is WALK, then we
    // can assume all access/egress legs are also walking. There is no need to check the
    // access/egress. This is an optimization for the most common use-ase.
    if (requestedMode == StreetMode.WALK) {
      return penaltyWalking.isEmpty()
        ? input
        : input
          .stream()
          .map(it -> it.withPenalty(penaltyWalking.calculate(it.durationInSeconds())))
          .toList();
    }

    // The request mode is NOT WALK, and we need to apply a penalty to the access/egress based on
    // the mode. We apply the walk penalty to all-walking access/egress and the penalty for the
    // requested mode to all other access/egress paths.
    var penaltyRequestedMode = penalty.valueOf(requestedMode);

    if (penaltyRequestedMode.isEmpty() && penaltyWalking.isEmpty()) {
      return input;
    }

    return input
      .stream()
      .map(it -> {
        var penalty = it.isWalkOnly() ? penaltyWalking : penaltyRequestedMode;
        return it.withPenalty(penalty.calculate(it.durationInSeconds()));
      })
      .toList();
  }
}
