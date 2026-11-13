package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferRealtimeUpdater;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;

/**
 * Updates the regular transfers of one write task. It writes to the mutable repository of the
 * task, and keeps no state of its own: what must last from one transaction to the next is stored
 * in the repository. Create a new instance for each write task.
 * <p>
 * Only {@link #closeFacilities} is implemented. The transfers of stops are not searched again at
 * runtime yet.
 *
 * @param <P> the street path type
 */
public class DefaultRegularTransferRealtimeUpdater<P> implements RegularTransferRealtimeUpdater<P> {

  private final RegularTransferRepository<P> repository;

  public DefaultRegularTransferRealtimeUpdater(RegularTransferRepository<P> repository) {
    this.repository = repository;
  }

  @Override
  public void updateTransfersForStop(FeedScopedId stopId) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void updateTransfersForStops(List<FeedScopedId> stops, Predicate<P> filterPaths) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public void closeFacilities(Set<String> closedFacilityIds) {
    repository.closeFacilities(closedFacilityIds);
  }
}
