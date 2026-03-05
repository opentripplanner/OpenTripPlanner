package org.opentripplanner.ext.flex;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.internal.TransferIndex;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.site.StopLocation;

public class FlexTransferIndex extends TransferIndex {

  private final Multimap<StopLocation, PathTransfer> transfersToStop = ArrayListMultimap.create();

  private final Multimap<StopLocation, PathTransfer> transfersFromStop = ArrayListMultimap.create();

  private boolean indexed = false;

  public void index(TransferRepository transferRepository) {
    if (indexed) {
      throw new IllegalStateException("Transfer index is already initialized");
    }
    super.index(transferRepository);
    // Flex transfers should only use WALK mode transfers.
    for (PathTransfer transfer : transferRepository.findTransfersByMode(StreetMode.WALK)) {
      transfersToStop.put(transfer.to, transfer);
      transfersFromStop.put(transfer.from, transfer);
    }
    indexed = true;
  }

  @Override
  public Collection<PathTransfer> findWalkTransfersToStop(StopLocation stopLocation) {
    checkIfIndexed();
    return transfersToStop.get(stopLocation);
  }

  @Override
  public Collection<PathTransfer> findWalkTransfersFromStop(StopLocation stopLocation) {
    checkIfIndexed();
    return transfersFromStop.get(stopLocation);
  }

  @Override
  public void invalidate() {
    indexed = false;
    super.invalidate();
    transfersToStop.clear();
    transfersFromStop.clear();
  }

  private void checkIfIndexed() {
    if (!indexed) {
      throw new IllegalStateException("The transfer index is needed but not initialized");
    }
  }
}
