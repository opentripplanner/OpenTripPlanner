package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriority32n;

public class DefaultTransitGroupPriorityCalculator implements RaptorTransitGroupPriorityCalculator {

  @Override
  public int mergeGroupIds(int currentGroupIds, int boardingGroupId) {
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
