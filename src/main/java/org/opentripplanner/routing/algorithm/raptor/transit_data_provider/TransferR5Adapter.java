package org.opentripplanner.routing.algorithm.raptor.transit_data_provider;

import com.conveyal.r5.otp2.api.transit.TransferLeg;
import org.opentripplanner.routing.algorithm.raptor.transit_layer.Transfer;

public class TransferR5Adapter implements TransferLeg {

    private final int duration;

    private final Transfer transfer;

    TransferR5Adapter(Transfer transfer, double walkSpeed) {
        this.transfer = transfer;
        this.duration = (int) Math.round(transfer.getDistance() / walkSpeed);
    }

    @Override
    public int stop() {
        return transfer.stop();
    }

    @Override
    public int durationInSeconds() {
        return this.duration;
    }
}
