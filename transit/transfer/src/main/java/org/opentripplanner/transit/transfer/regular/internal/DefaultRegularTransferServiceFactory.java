package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.RegularTransferServiceFactory;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Default {@link RegularTransferServiceFactory}, bound to one snapshot. On a cache miss it builds a
 * {@link RaptorTransferStore} from the paths stored under the user preferences closest to the
 * request-time preferences, re-costed under the request-time preferences. The result is cached in the snapshot, see
 * {@link RegularTransferRepositorySnapshot#getOrCreateTransferService}.
 *
 * @param <P> the transfer path/template type
 */
public class DefaultRegularTransferServiceFactory<P> implements RegularTransferServiceFactory<P> {

  private final StopIndex stopIndex;
  private final RegularTransferRepositorySnapshot<P> snapshot;
  private final TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider;

  public DefaultRegularTransferServiceFactory(
    StopIndex stopIndex,
    RegularTransferRepositorySnapshot<P> snapshot,
    TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider
  ) {
    this.stopIndex = stopIndex;
    this.snapshot = snapshot;
    this.pathProvider = pathProvider;
  }

  @Override
  public RaptorRegularTransferService create(AbstractUserPreferences<?> userPreferences) {
    return snapshot.getOrCreateTransferService(userPreferences, () -> build(userPreferences));
  }

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since a {@link RaptorTransferStore} lookup only
   * carries {@code (stop, duration, c1)}, not the path itself.
   */
  @Nullable
  @Override
  public P findPath(AbstractUserPreferences<?> userPreferences, int fromStop, int toStop) {
    return snapshot.findPath(userPreferences, fromStop, toStop);
  }

  private RaptorRegularTransferService build(AbstractUserPreferences<?> userPreferences) {
    var builder = RaptorTransferStore.of(stopIndex.size());
    for (var stored : snapshot.pathsFor(userPreferences)) {
      Optional<PathCriteria> criteria = pathProvider.computePathCriteria(
        stored.path(),
        userPreferences
      );
      if (criteria.isEmpty()) {
        continue;
      }
      builder.addTransfer(
        stored.fromStop(),
        stored.toStop(),
        criteria.get().durationInSeconds(),
        criteria.get().c1()
      );
    }
    return asRegularTransferService(builder.build());
  }

  /**
   * {@link RaptorTransferStore} lives in {@code raptor-data}, which this module depends on - it
   * cannot itself implement {@link RaptorRegularTransferService}, defined here. Both expose the
   * same two methods, so this just delegates.
   */
  private static RaptorRegularTransferService asRegularTransferService(RaptorTransferStore store) {
    return new RaptorRegularTransferService() {
      @Override
      public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
        return store.getTransfersFromStop(fromStop);
      }

      @Override
      public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
        return store.getTransfersToStop(toStop);
      }
    };
  }
}
