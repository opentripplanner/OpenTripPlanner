package org.opentripplanner.transit.raptor.rangeraptor.standard.internalapi;

import org.opentripplanner.transit.raptor.api.transit.AccessEgress;

public interface DestinationArrivalListener {
  void newDestinationArrival(
    int round,
    int fromStopArrivalTime,
    boolean stopReachedOnBoard,
    AccessEgress egressPath
  );
}
