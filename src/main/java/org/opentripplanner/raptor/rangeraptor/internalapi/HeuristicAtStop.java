package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.time.Duration;

/**
 * Heuristic data for a given stop.
 */
public record HeuristicAtStop(int minTravelDuration, int minNumTransfers, int minCost) {
  /**
   * Representation for a stop, which has not been reached by the heuristic search
   */
  public static final HeuristicAtStop UNREACHED = new HeuristicAtStop(
    Integer.MAX_VALUE,
    Integer.MAX_VALUE,
    Integer.MAX_VALUE
  );

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
