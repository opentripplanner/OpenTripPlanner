package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class TransferWithDuration implements RaptorTransfer {

    private final int durationSeconds;

    private final Transfer transfer;

    public TransferWithDuration(Transfer transfer, double walkSpeed) {
        this.transfer = transfer;
        this.durationSeconds = (int) Math.round(transfer.getEffectiveWalkDistanceMeters() / walkSpeed);
    }

    @Override
    public int stop() {
        return transfer.getToStop();
    }

    @Override
    public int earliestDepartureTime(int requestedDepartureTime) {
        return requestedDepartureTime;
    }

    @Override
    public int latestArrivalTime(int requestedArrivalTime) {
        return requestedArrivalTime;
    }

    @Override
    public int durationInSeconds() {
        return this.durationSeconds;
    }
}
