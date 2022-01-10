package org.opentripplanner.graph_builder.model;

import com.google.common.base.MoreObjects;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("from", from)
                .add("to", to)
                .add("minTransferTime", minTransferTime)
                .toString();
    }
}
