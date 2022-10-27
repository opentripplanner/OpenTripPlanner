package org.opentripplanner.transit.raptor.rangeraptor.internalapi;

import java.time.Duration;

/**
 * Heuristic data for a given stop.
 */
public record HeuristicAtStop(int minTravelDuration, int minNumTransfers, int minCost) {
  @Override
  public String toString() {
    return (
      "HeuristicAtStop{" +
      "minTravelDuration: " +
      Duration.ofSeconds(minTravelDuration).toString() +
      ", minNumTransfers: " +
      minNumTransfers +
      ", minCost: $" +
      (minCost / 100.0) +
      "}"
    );
  }
}
