package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Set;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfers;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created by
 * {@link RegularTransferRepositoryLifecycle#freeze}. The snapshot owns its
 * {@link OverlaidTemplatePaths}, which is never written to after creation. Hence, the paths are
 * effectively immutable and safe to read from any number of request threads. The cache of the
 * Raptor transfers derived from them is the only mutable state, and it is thread-safe.
 *
 * @param <P> the street path type
 * @param <R> the request type
 */
class DefaultRegularTransferRepositorySnapshot<
  P,
  R
> implements RegularTransferRepositorySnapshot<P, R> {

  private final OverlaidTemplatePaths<P> paths;
  private final RegularTransferServiceCache<RaptorTransferStore> cache;
  private final CacheFactory cacheFactory;
  private final TransferRequestMapper<R> mapper;

  DefaultRegularTransferRepositorySnapshot(
    OverlaidTemplatePaths<P> paths,
    RegularTransferServiceCache<RaptorTransferStore> cache,
    RaptorTransfersFactory<P, R> raptorTransfersFactory,
    TransferRequestMapper<R> mapper
  ) {
    this.paths = paths;
    this.cache = cache;
    this.mapper = mapper;
    this.cacheFactory = new CacheFactory(raptorTransfersFactory);
  }

  @Override
  public RegularTransfers<P> forRequest(R request) {
    var userPreferences = mapper.toUserPreferences(request);
    return new DefaultRegularTransfers<>(
      paths,
      cache,
      cacheFactory,
      userPreferences,
      paths.closestProfile(userPreferences)
    );
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

    private final RaptorTransfersFactory<P, R> raptorTransfersFactory;

    private CacheFactory(RaptorTransfersFactory<P, R> raptorTransfersFactory) {
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
