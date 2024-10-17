package org.opentripplanner.raptor.rangeraptor.standard.internalapi;

import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

public interface DestinationArrivalListener {
  void newDestinationArrival(
    int round,
    int fromStopArrivalTime,
    boolean stopReachedOnBoard,
    RaptorAccessEgress egressPath
  );
}
