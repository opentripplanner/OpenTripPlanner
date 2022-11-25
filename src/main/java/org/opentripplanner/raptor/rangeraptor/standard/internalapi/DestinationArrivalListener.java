package org.opentripplanner.raptor.rangeraptor.standard.internalapi;

import org.opentripplanner.raptor.spi.RaptorAccessEgress;

public interface DestinationArrivalListener {
  void newDestinationArrival(
    int round,
    int fromStopArrivalTime,
    boolean stopReachedOnBoard,
    RaptorAccessEgress egressPath
  );
}
