package org.opentripplanner.transfer.internal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.transfer.TransferRepository;
import org.opentripplanner.transit.model.site.StopLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTransferRepository implements TransferRepository {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultTransferRepository.class);

  private final Multimap<StopLocation, PathTransfer> transfersByStop = HashMultimap.create();

  private final TransferIndex index;

  public DefaultTransferRepository(TransferIndex index) {
    this.index = index;
  }

  @Override
  public Collection<PathTransfer> findTransfersByStop(StopLocation stop) {
    return transfersByStop.get(stop);
  }

  /** Pre-generated transfers between all stops filtered based on the modes in the PathTransfer. */
  @Override
  public List<PathTransfer> findTransfersByMode(StreetMode mode) {
    return transfersByStop
      .values()
      .stream()
      .filter(pathTransfer -> pathTransfer.getModes().contains(mode))
      .toList();
  }

  @Override
  public Collection<PathTransfer> listPathTransfers() {
    return transfersByStop.values();
  }

  @Override
  public void addAllTransfersByStops(Multimap<StopLocation, PathTransfer> transfersByStop) {
    index.invalidate();
    this.transfersByStop.putAll(transfersByStop);
  }

  @Override
  public void index() {
    LOG.info("Transfer repository indexing...");
    index.index(this);
    LOG.info("Transfer repository indexing complete.");
  }

  @Override
  public Collection<PathTransfer> findWalkTransfersToStop(StopLocation toStop) {
    return index.findWalkTransfersToStop(toStop);
  }

  @Override
  public Collection<PathTransfer> findWalkTransfersFromStop(StopLocation fromStop) {
    return index.findWalkTransfersFromStop(fromStop);
  }
}
