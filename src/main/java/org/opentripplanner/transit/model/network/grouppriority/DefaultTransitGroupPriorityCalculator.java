package org.opentripplanner.transit.model.network.grouppriority;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;

/**
 * Implement {@link RaptorTransitGroupPriorityCalculator}.
 */
public final class DefaultTransitGroupPriorityCalculator
  implements RaptorTransitGroupPriorityCalculator {

  @Override
  public int mergeInGroupId(int currentGroupIds, int boardingGroupId) {
    return TransitGroupPriority32n.mergeInGroupId(currentGroupIds, boardingGroupId);
  }

  @Override
  public DominanceFunction dominanceFunction() {
    return TransitGroupPriority32n::dominate;
  }

  @Override
  public String toString() {
    return "DefaultTransitGroupCalculator{Using TGP32n}";
  }
}
