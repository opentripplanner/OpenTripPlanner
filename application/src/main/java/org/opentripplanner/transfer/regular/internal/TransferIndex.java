package org.opentripplanner.transfer.regular.internal;

import java.util.Collection;
import org.opentripplanner.transfer.regular.TransferRepository;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * Index for fast transfer look up. At the moment this is only used and implemented for Flex
 * searches. Flex searches can not use the similar data structure in the {@code RaptorTransitData},
 * because flex searches only consider walking transfers.
 * <p>
 * ToDo: When the snapshot mechanic for the transfers gets implemented (see
 * <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/7074">issue 7074</a>), this
 * should be consolidated.
 */
public class TransferIndex {

  public Collection<PathTransfer> findWalkTransfersToStop(StopLocation stopLocation) {
    throw new UnsupportedOperationException("The transfer index used does not support this method");
  }

  public Collection<PathTransfer> findWalkTransfersFromStop(StopLocation stopLocation) {
    throw new UnsupportedOperationException("The transfer index used does not support this method");
  }

  public void invalidate() {}

  public void index(TransferRepository transferRepository) {}
}
