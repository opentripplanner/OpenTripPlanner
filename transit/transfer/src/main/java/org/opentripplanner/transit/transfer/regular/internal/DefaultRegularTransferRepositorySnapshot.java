package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created by
 * {@link RegularTransferRepositoryLifecycle#freeze}. The snapshot owns its
 * {@link OverlaidTemplatePaths}, which is never written to after creation. Hence, the paths are
 * effectively immutable and safe to read from any number of request threads. The cache of the
 * Raptor transfers derived from them is the only mutable state, and it is thread-safe.
 *
 * @param <P> the street path type
 */
class DefaultRegularTransferRepositorySnapshot<P> implements RegularTransferRepositorySnapshot<P> {

  private final OverlaidTemplatePaths<P> paths;
  private final RegularTransferServiceCache<RaptorTransferStore> cache;
  private final CacheFactory cacheFactory;

  DefaultRegularTransferRepositorySnapshot(
    OverlaidTemplatePaths<P> paths,
    RegularTransferServiceCache<RaptorTransferStore> cache,
    RaptorTransfersFactory<P> raptorTransfersFactory
  ) {
    this.paths = paths;
    this.cache = cache;
    this.cacheFactory = new CacheFactory(raptorTransfersFactory);
  }

  @Override
  public RaptorRegularTransferService raptorTransfers(AbstractUserPreferences<?> userPreferences) {
    return cache.get(userPreferences, cacheFactory);
  }

  @Nullable
  @Override
  public P findPath(AbstractUserPreferences<?> userPreferences, int fromStop, int toStop) {
    return paths.findPath(paths.closestProfile(userPreferences), fromStop, toStop);
  }

  /** The paths of this snapshot, for the lifecycle. */
  OverlaidTemplatePaths<P> paths() {
    return paths;
  }

  /** The cache of the Raptor transfers of this snapshot, for the lifecycle. */
  RegularTransferServiceCache<RaptorTransferStore> cache() {
    return cache;
  }

  /** Creates and patches the cached Raptor transfers from the paths of this snapshot. */
  private final class CacheFactory
    implements RegularTransferServiceCache.Factory<RaptorTransferStore>
  {

    private final RaptorTransfersFactory<P> raptorTransfersFactory;

    private CacheFactory(RaptorTransfersFactory<P> raptorTransfersFactory) {
      this.raptorTransfersFactory = raptorTransfersFactory;
    }

    @Override
    public RaptorTransferStore create(AbstractUserPreferences<?> userPreferences) {
      return raptorTransfersFactory.create(
        paths,
        userPreferences,
        paths.closestProfile(userPreferences)
      );
    }

    @Override
    public RaptorTransferStore patch(
      AbstractUserPreferences<?> userPreferences,
      RaptorTransferStore previous,
      Set<Integer> changedFromStops
    ) {
      return raptorTransfersFactory.patch(
        paths,
        userPreferences,
        paths.closestProfile(userPreferences),
        previous,
        changedFromStops
      );
    }
  }
}
