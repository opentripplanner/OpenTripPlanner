package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.util.ReversedRaptorTransfer;

public class RaptorTransferIndex {

  private final List<List<RaptorTransfer>> forwardTransfers;

  private final List<List<RaptorTransfer>> reversedTransfers;

  public RaptorTransferIndex(
    List<List<RaptorTransfer>> forwardTransfers,
    List<List<RaptorTransfer>> reversedTransfers
  ) {
    // Create immutable copies of the lists for each stop to make them immutable and faster to iterate
    this.forwardTransfers =
      forwardTransfers.stream().map(List::copyOf).collect(Collectors.toList());

    this.reversedTransfers =
      reversedTransfers.stream().map(List::copyOf).collect(Collectors.toList());
  }

  public List<List<RaptorTransfer>> getForwardTransfers() {
    return forwardTransfers;
  }

  public List<List<RaptorTransfer>> getReversedTransfers() {
    return reversedTransfers;
  }

  public static RaptorTransferIndex create(
    List<List<Transfer>> transfersByStopIndex,
    RoutingContext routingContext
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
      var transfers = transfersByStopIndex
        .get(fromStop)
        .stream()
        .flatMap(s -> s.asRaptorTransfer(routingContext).stream())
        .collect(
          toMap(
            RaptorTransfer::stop,
            Function.identity(),
            (a, b) -> a.generalizedCost() < b.generalizedCost() ? a : b
          )
        )
        .values();

      forwardTransfers.get(fromStop).addAll(transfers);

      for (RaptorTransfer forwardTransfer : transfers) {
        reversedTransfers
          .get(forwardTransfer.stop())
          .add(new ReversedRaptorTransfer(fromStop, forwardTransfer));
      }
    }

    return new RaptorTransferIndex(forwardTransfers, reversedTransfers);
  }
}
