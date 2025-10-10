package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.street.search.request.StreetSearchRequest;

class OnDemandRaptorTransferIndex implements RaptorTransferIndex {

  private final List<List<Transfer>> forwardTransfers;
  private List<List<TransferWithSource>> reversedTransfers;
  private final Collection<DefaultRaptorTransfer>[] forwardRaptorTransfers;

  private final Collection<DefaultRaptorTransfer>[] reversedRaptorTransfers;

  private final StreetSearchRequest request;

  private record TransferWithSource(Transfer transfer, int fromStopIndex) {}

  OnDemandRaptorTransferIndex(
    List<List<Transfer>> transfersByStopIndex,
    StreetSearchRequest request
  ) {
    this.request = request;
    forwardTransfers = transfersByStopIndex;

    //noinspection unchecked
    forwardRaptorTransfers = new Collection[transfersByStopIndex.size()];
    //noinspection unchecked
    reversedRaptorTransfers = new Collection[transfersByStopIndex.size()];
  }

  private synchronized void initializeReversedTransfers() {
    if (reversedTransfers == null) {
      reversedTransfers = Stream.generate(() ->
        (List<TransferWithSource>) new ArrayList<TransferWithSource>()
      )
        .limit(forwardTransfers.size())
        .toList();

      for (var i = 0; i < forwardTransfers.size(); ++i) {
        var transfers = forwardTransfers.get(i);
        for (var transfer : transfers) {
          reversedTransfers.get(transfer.getToStop()).add(new TransferWithSource(transfer, i));
        }
      }
    }
  }

  @Override
  public Collection<DefaultRaptorTransfer> getForwardTransfers(int stopIndex) {
    if (forwardRaptorTransfers[stopIndex] == null) {
      forwardRaptorTransfers[stopIndex] = RaptorTransferIndex.getRaptorTransfers(
        request,
        forwardTransfers.get(stopIndex)
      );
    }

    return forwardRaptorTransfers[stopIndex];
  }

  @Override
  public Collection<DefaultRaptorTransfer> getReversedTransfers(int stopIndex) {
    initializeReversedTransfers();

    if (reversedRaptorTransfers[stopIndex] == null) {
      reversedRaptorTransfers[stopIndex] = getReversedRaptorTransfers(
        reversedTransfers.get(stopIndex)
      );
    }

    return reversedRaptorTransfers[stopIndex];
  }

  private Collection<DefaultRaptorTransfer> getReversedRaptorTransfers(
    List<TransferWithSource> transferWithSources
  ) {
    var mode = request.mode();
    // The transfers are filtered so that there is only one possible directional transfer
    // for a stop pair.
    return transferWithSources
      .stream()
      .filter(s -> s.transfer.allowsMode(mode))
      .flatMap(s ->
        s.transfer.asRaptorTransfer(request).stream().map(rt -> rt.reverseOf(s.fromStopIndex))
      )
      .collect(toMap(RaptorTransfer::stop, Function.identity(), (a, b) -> a.c1() < b.c1() ? a : b))
      .values();
  }
}
