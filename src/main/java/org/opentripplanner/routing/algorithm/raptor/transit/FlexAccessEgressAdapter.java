package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.ext.flex.FlexAccessEgress;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg AccessEgress.
 */
public class FlexAccessEgressAdapter extends AccessEgress {
  private final FlexAccessEgress flexAccessEgress;

  public FlexAccessEgressAdapter(
      FlexAccessEgress flexAccessEgress, StopIndexForRaptor stopIndex
  ) {
    super(
        stopIndex.indexByStop.get(flexAccessEgress.stop),
        flexAccessEgress.preFlexTime + flexAccessEgress.flexTime + flexAccessEgress.postFlexTime,
        flexAccessEgress.lastState
    );

    this.flexAccessEgress = flexAccessEgress;
  }

  @Override
  public int earliestDepartureTime(int requestedDepartureTime) {
    return flexAccessEgress.earliestDepartureTime(requestedDepartureTime);
  }

  @Override
  public int latestArrivalTime(int requestedArrivalTime) {
    return flexAccessEgress.latestArrivalTime(requestedArrivalTime);
  }

  @Override
  public int numberOfLegs() {
    return flexAccessEgress.directToStop ? 2 : 3;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return flexAccessEgress.directToStop;
  }
}
