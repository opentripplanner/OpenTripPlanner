package org.opentripplanner.routing.algorithm.raptor.transit;

import java.util.List;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

public class RaptorTransferIndex {

    private final List<List<RaptorTransfer>> forwardTransfers;

    private final List<List<RaptorTransfer>> reversedTransfers;

    public RaptorTransferIndex(
            List<List<RaptorTransfer>> forwardTransfers,
            List<List<RaptorTransfer>> reversedTransfers
    ) {
        this.forwardTransfers = forwardTransfers;
        this.reversedTransfers = reversedTransfers;
    }

    public List<List<RaptorTransfer>> getForwardTransfers() {
        return forwardTransfers;
    }

    public List<List<RaptorTransfer>> getReversedTransfers() {
        return reversedTransfers;
    }
}
