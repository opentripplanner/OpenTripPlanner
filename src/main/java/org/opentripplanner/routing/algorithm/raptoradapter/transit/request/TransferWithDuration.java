package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class TransferWithDuration implements RaptorTransfer {

    private final int durationSeconds;
    private final int cost;

    private final Transfer transfer;

    public TransferWithDuration(Transfer transfer, int durationSeconds, int cost) {
        this.transfer = transfer;
        this.durationSeconds = durationSeconds;
        this.cost = cost;
    }

    public Transfer transfer() {
        return transfer;
    }

    @Override
    public int stop() {
        return transfer.getToStop();
    }

    @Override
    public int generalizedCost() {
        return cost;
    }

    @Override
    public int durationInSeconds() {
        return this.durationSeconds;
    }

    @Override
    public boolean hasOpeningHours() {
        return false;
    }

    @Override
    public String toString() {
        return asString();
    }
}
