package org.opentripplanner.transfer.regular.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.transfer.regular.model.DefaultRaptorTransfer;
import org.opentripplanner.transfer.regular.model.PathTransfer;

class OnDemandRaptorTransferIndex implements RaptorTransferIndex {

  private final List<List<PathTransfer>> transfersByFromStopIndex;
  private List<List<PathTransfer>> transfersByToStopIndex;
  private final Collection<DefaultRaptorTransfer>[] forwardRaptorTransfers;

  private final Collection<DefaultRaptorTransfer>[] reversedRaptorTransfers;

  private final StreetSearchRequest request;

  OnDemandRaptorTransferIndex(
    List<List<PathTransfer>> transfersByStopIndex,
    StreetSearchRequest request
  ) {
    this.request = request;
    transfersByFromStopIndex = transfersByStopIndex;

    //noinspection unchecked
    forwardRaptorTransfers = new Collection[transfersByStopIndex.size()];
    //noinspection unchecked
    reversedRaptorTransfers = new Collection[transfersByStopIndex.size()];
  }

  private synchronized void initializeTransfersByToStop() {
    if (transfersByToStopIndex == null) {
      transfersByToStopIndex = new ArrayList<>(transfersByFromStopIndex.size());
      for (int i = 0; i < transfersByFromStopIndex.size(); i++) {
        transfersByToStopIndex.add(new ArrayList<>());
      }

      for (List<PathTransfer> transfers : transfersByFromStopIndex) {
        for (var transfer : transfers) {
          transfersByToStopIndex.get(transfer.to.getIndex()).add(transfer);
        }
      }
    }
  }

  @Override
  public Collection<DefaultRaptorTransfer> getForwardTransfers(int stopIndex) {
    // This block is not fully thread safe as there may be a race condition between the check
    // and the assignment. However, the assignment is an atomic operation and the assigned value
    // should always be the same, so we don't think that it will be a major problem.
    // We don't think that the overhead of locking is worthwhile for the occasional chance of two
    // threads generating the transfers at the same time.
    if (forwardRaptorTransfers[stopIndex] == null) {
      forwardRaptorTransfers[stopIndex] = RaptorTransferIndex.getRaptorTransfers(
        request,
        transfersByFromStopIndex.get(stopIndex)
      );
    }

    return forwardRaptorTransfers[stopIndex];
  }

  @Override
  public Collection<DefaultRaptorTransfer> getReversedTransfers(int stopIndex) {
    initializeTransfersByToStop();

    if (reversedRaptorTransfers[stopIndex] == null) {
      reversedRaptorTransfers[stopIndex] = RaptorTransferIndex.getRaptorTransfers(
        request,
        transfersByToStopIndex.get(stopIndex)
      )
        .stream()
        .map(t -> t.reverseOf(t.transfer().from.getIndex()))
        .toList();
    }

    return reversedRaptorTransfers[stopIndex];
  }
}
