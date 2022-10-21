package org.opentripplanner.transit.raptor.rangeraptor.internalapi;

import org.opentripplanner.util.lang.ToStringBuilder;

/**
 * Heuristic data for a given stop.
 */
public record HeuristicAtStop(int minTravelDuration, int minNumTransfers, int minCost) {
  @Override
  public String toString() {
    return ToStringBuilder
      .of(HeuristicAtStop.class)
      .addDurationSec("minTravelDuration", minTravelDuration)
      .addNum("minNumTransfers", minNumTransfers)
      .addCostCenti("minCost", minCost, null)
      .toString();
  }
}
