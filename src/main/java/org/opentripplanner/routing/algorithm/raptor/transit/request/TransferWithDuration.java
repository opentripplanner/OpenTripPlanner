package org.opentripplanner.routing.algorithm.raptor.transit.request;

import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class TransferWithDuration implements RaptorTransfer {

    private final int durationSeconds;

    private final Transfer transfer;

    public TransferWithDuration(Transfer transfer, int durationSeconds) {
        this.transfer = transfer;
        this.durationSeconds = durationSeconds;
    }

    public Transfer transfer() {
        return transfer;
    }

    @Override
    public int stop() {
        return transfer.getToStop();
    }

    @Override
    public int durationInSeconds() {
        return this.durationSeconds;
    }

    @Override
    public String toString() {
        return asString();
    }
}
