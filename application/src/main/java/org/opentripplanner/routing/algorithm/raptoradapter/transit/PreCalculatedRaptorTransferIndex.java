package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class PreCalculatedRaptorTransferIndex implements RaptorTransferIndex {

  private final List<DefaultRaptorTransfer>[] forwardTransfers;

  private final List<DefaultRaptorTransfer>[] reversedTransfers;

  PreCalculatedRaptorTransferIndex(
    List<List<Transfer>> transfersByStopIndex,
    StreetSearchRequest request
  ) {
    var forwardTransfers = new ArrayList<List<DefaultRaptorTransfer>>(transfersByStopIndex.size());
    var reversedTransfers = new ArrayList<List<DefaultRaptorTransfer>>(transfersByStopIndex.size());

    for (int i = 0; i < transfersByStopIndex.size(); i++) {
      forwardTransfers.add(new ArrayList<>());
      reversedTransfers.add(new ArrayList<>());
    }

    IntStream.range(0, transfersByStopIndex.size())
      .parallel()
      .forEach(fromStop -> {
        var transfers = transfersByStopIndex.get(fromStop);
        var raptorTransfers = RaptorTransferIndex.getRaptorTransfers(request, transfers);

        // forwardTransfers is not modified here, and no two threads will access the same element
        // in it, so this is still thread safe.
        forwardTransfers.get(fromStop).addAll(raptorTransfers);
      });

    for (int fromStop = 0; fromStop < transfersByStopIndex.size(); fromStop++) {
      for (var forwardTransfer : forwardTransfers.get(fromStop)) {
        reversedTransfers.get(forwardTransfer.stop()).add(forwardTransfer.reverseOf(fromStop));
      }
    }

    // Create immutable copies of the lists for each stop to make them immutable and faster to iterate
    //noinspection unchecked
    this.forwardTransfers = forwardTransfers.stream().map(List::copyOf).toArray(List[]::new);
    //noinspection unchecked
    this.reversedTransfers = reversedTransfers.stream().map(List::copyOf).toArray(List[]::new);
  }

  public Collection<DefaultRaptorTransfer> getForwardTransfers(int stopIndex) {
    return forwardTransfers[stopIndex];
  }

  public Collection<DefaultRaptorTransfer> getReversedTransfers(int stopIndex) {
    return reversedTransfers[stopIndex];
  }
}
