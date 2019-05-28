package org.opentripplanner.routing.algorithm.raptor.transit.request;

import com.conveyal.r5.otp2.api.transit.TransferLeg;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;

public class TransferWithDuration implements TransferLeg {

    private final int durationSeconds;

    private final Transfer transfer;

    public TransferWithDuration(Transfer transfer, double walkSpeed) {
        this.transfer = transfer;
        this.durationSeconds = (int) Math.round(transfer.getDistanceMeters() / walkSpeed);
    }

    @Override
    public int stop() {
        return transfer.stop();
    }

    @Override
    public int durationInSeconds() {
        return this.durationSeconds;
    }
}
