package org.opentripplanner.transit.raptor.rangeraptor.internalapi;

import org.opentripplanner.util.time.TimeUtils;

/**
 * Heuristic data for a given stop.
 */
public record HeuristicAtStop(int minTravelDuration, int minNumTransfers, int minCost) {
  @Override
  public String toString() {
    return (
      "Heuristic{" +
      "minTravelTime=" +
      TimeUtils.timeToStrCompact(minTravelDuration) +
      ", minNumTransfers=" +
      minNumTransfers +
      ", minCost=" +
      minCost +
      '}'
    );
  }
}
