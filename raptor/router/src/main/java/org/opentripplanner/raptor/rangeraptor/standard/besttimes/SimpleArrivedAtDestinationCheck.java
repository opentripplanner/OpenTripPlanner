package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import org.opentripplanner.raptor.rangeraptor.standard.internalapi.ArrivedAtDestinationCheck;

public class SimpleArrivedAtDestinationCheck implements ArrivedAtDestinationCheck {

  /**
   * The list of egress stops used to terminate the search when a stop is reached by transit.
   * Egress paths that start by walking can not be used with transfer arrivals, since we
   * do not allow two walking legs after each other.
   */
  private final int[] walkToDestinationEgressStops;

  /**
   * The list of egress stops to terminate the search when a stop is the destination or is reached
   * by transfer.
   */
  private final int[] rideToDestinationEgressStops;

  private final BestTimes stopVisited;

  public SimpleArrivedAtDestinationCheck(
    BestTimes stopVisited,
    int[] walkToDestinationEgressStops,
    int[] rideToDestinationEgressStops
  ) {
    this.stopVisited = stopVisited;
    this.walkToDestinationEgressStops = walkToDestinationEgressStops;
    this.rideToDestinationEgressStops = rideToDestinationEgressStops;
  }

  @Override
  public boolean arrivedAtDestinationCurrentRound() {
    // This is not 100% correct, we can risk here that we abort the search before we have
    // found a valid path. This does not have an effect on the stop-arrivals
    // (used for mc-heuristics). The correct way to do this is to check the egress if it is
    // possible to use it in combination with the arrival time within the time limit.
    for (final int egressStop : walkToDestinationEgressStops) {
      if (stopVisited.isStopReachedOnBoardInCurrentRound(egressStop)) {
        return true;
      }
    }
    for (final int egressStop : rideToDestinationEgressStops) {
      if (stopVisited.isStopReachedInCurrentRound(egressStop)) {
        return true;
      }
    }
    return false;
  }
}
