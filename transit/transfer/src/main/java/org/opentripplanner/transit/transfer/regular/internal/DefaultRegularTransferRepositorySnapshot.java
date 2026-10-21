package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.raptor.spi.RaptorTransfer;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created by
 * {@link RegularTransferRepositoryLifecycle#freeze}. The snapshot owns its
 * {@link TemplatePathsByUserProfile}, which is never written to after creation. Hence, the paths
 * are effectively immutable and safe to read from any number of request threads. The cache of the
 * Raptor transfers derived from them is the only mutable state, and it is thread-safe.
 *
 * @param <P> the transfer path/template type
 */
class DefaultRegularTransferRepositorySnapshot<P> implements RegularTransferRepositorySnapshot<P> {

  private final TemplatePathsByUserProfile<P> paths;
  private final StopIndex stopIndex;
  private final TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider;
  private final RegularTransferServiceCache transferServiceCache =
    new RegularTransferServiceCache();

  DefaultRegularTransferRepositorySnapshot(
    TemplatePathsByUserProfile<P> paths,
    StopIndex stopIndex,
    TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider
  ) {
    this.paths = paths;
    this.stopIndex = stopIndex;
    this.pathProvider = pathProvider;
  }

  @Override
  public RaptorRegularTransferService raptorTransfers(AbstractUserPreferences<?> userPreferences) {
    return transferServiceCache.getOrCreate(userPreferences, () ->
      createRaptorTransfers(userPreferences)
    );
  }

  @Nullable
  @Override
  public P findPath(AbstractUserPreferences<?> userPreferences, int fromStop, int toStop) {
    return paths.findPath(userPreferences, fromStop, toStop);
  }

  @Override
  public List<StoredPath<P>> pathsFor(AbstractUserPreferences<?> userPreferences) {
    return paths.pathsFor(userPreferences);
  }

  /**
   * Create a new mutable repository initialized with the state of this snapshot. Only the
   * lifecycle should have access to this, hence the package local access.
   */
  DefaultRegularTransferRepository<P> copyOnWrite() {
    return new DefaultRegularTransferRepository<>(paths);
  }

  /**
   * Build a {@link RaptorTransferStore} from the paths stored under the user preferences closest
   * to {@code userPreferences}, re-costed under {@code userPreferences}. Paths exceeding the
   * duration limit of the preferences are left out.
   */
  private RaptorRegularTransferService createRaptorTransfers(
    AbstractUserPreferences<?> userPreferences
  ) {
    var builder = RaptorTransferStore.of(stopIndex.size());
    for (var stored : paths.pathsFor(userPreferences)) {
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
