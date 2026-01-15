package org.opentripplanner.transfer.internal;

import java.util.Collection;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transfer.TransferService;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides read access to transfers. Maybe this will be a TransferSnapshot or similar
 * once we implement the concurrency management for updates
 */
public class DefaultTransferService implements TransferService {

  private static final Logger LOG = LoggerFactory.getLogger(TransferService.class);

  private final TransferRepository transferRepository;

  public DefaultTransferService(TransferRepository transferRepository) {
    this.transferRepository = transferRepository;
    LOG.info("Initializing TransferService");
  }

  public Collection<PathTransfer> findTransfersByStop(StopLocation fromStop) {
    return transferRepository.findTransfersByStop(fromStop);
  }

  public Collection<PathTransfer> findWalkTransfersToStop(StopLocation toStop) {
    return transferRepository.findWalkTransfersToStop(toStop);
  }

  public Collection<PathTransfer> findWalkTransfersFromStop(StopLocation fromStop) {
    return transferRepository.findWalkTransfersFromStop(fromStop);
  }
}
