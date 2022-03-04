package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.util.ReversedRaptorTransfer;

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

    public static RaptorTransferIndex create(
            List<List<Transfer>> transfersByStopIndex,
            RoutingRequest routingRequest
    ) {
        var forwardTransfers = new ArrayList<List<RaptorTransfer>>(transfersByStopIndex.size());
        var reversedTransfers = new ArrayList<List<RaptorTransfer>>(transfersByStopIndex.size());

        for (int i = 0; i < transfersByStopIndex.size(); i++) {
            forwardTransfers.add(new ArrayList<>());
            reversedTransfers.add(new ArrayList<>());
        }

        for (int fromStop = 0; fromStop < transfersByStopIndex.size(); fromStop++) {
            // The transfers are filtered so that there is only one possible directional transfer
            // for a stop pair.
            var transfers = transfersByStopIndex.get(fromStop)
                    .stream()
                    .flatMap(s -> s.asRaptorTransfer(routingRequest).stream())
                    .collect(toMap(
                            RaptorTransfer::stop,
                            Function.identity(),
                            (a, b) -> a.generalizedCost() < b.generalizedCost() ? a : b
                    ))
                    .values();

            forwardTransfers.get(fromStop).addAll(transfers);

            for (RaptorTransfer forwardTransfer : transfers) {
                reversedTransfers.get(forwardTransfer.stop())
                        .add(new ReversedRaptorTransfer(fromStop, forwardTransfer));
            }
        }

        return new RaptorTransferIndex(forwardTransfers, reversedTransfers);
    }
}
