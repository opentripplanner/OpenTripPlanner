package org.opentripplanner.model.transfer;

import java.time.Duration;
import org.opentripplanner.model.StopLocation;

public class MinTimeTransfer {
    public final StopLocation from;
    public final StopLocation to;
    public final Duration minTransferTime;

    public MinTimeTransfer(StopLocation from, StopLocation to, Duration minTransferTime) {
        this.from = from;
        this.to = to;
        this.minTransferTime = minTransferTime;
    }
}
