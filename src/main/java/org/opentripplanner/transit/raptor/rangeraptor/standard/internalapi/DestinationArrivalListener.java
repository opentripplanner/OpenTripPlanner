package org.opentripplanner.transit.raptor.rangeraptor.standard.internalapi;

import org.opentripplanner.transit.raptor.api.transit.RaptorAccessEgress;

public interface DestinationArrivalListener {
  void newDestinationArrival(
    int round,
    int fromStopArrivalTime,
    boolean stopReachedOnBoard,
    RaptorAccessEgress egressPath
  );
}
