package org.opentripplanner.routing.algorithm.raptor.transit;

import java.time.ZonedDateTime;
import org.opentripplanner.ext.flex.FlexAccessEgress;

/**
 * This class is used to adapt the FlexAccessEgress into a time-dependent multi-leg AccessEgress.
 */
public class FlexAccessEgressAdapter extends AccessEgress {
  private final FlexAccessEgress flexAccessEgress;

  public FlexAccessEgressAdapter(
          FlexAccessEgress flexAccessEgress,
          StopIndexForRaptor stopIndex,
          ZonedDateTime startOfTime,
          boolean isEgress
  ) {
    super(
        stopIndex.indexByStop.get(flexAccessEgress.stop),
        isEgress ? flexAccessEgress.lastState.reverse() : flexAccessEgress.lastState,
        startOfTime
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
  public int numberOfRides() {
    // We only support one flex leg at the moment
    return 1;
  }

  @Override
  public boolean stopReachedOnBoard() {
    return flexAccessEgress.directToStop;
  }

  @Override
  public String toString() {
    return asString();
  }
}
