package org.opentripplanner.transit.raptor.rangeraptor.standard;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public interface DestinationArrivalListener {
    void newDestinationArrival(
            int round,
            int fromStopArrivalTime,
            boolean stopReachedOnBoard,
            RaptorTransfer egressPath
    );
}
