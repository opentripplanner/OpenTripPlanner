package org.opentripplanner.transit.transfer.regular.internal;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Default {@link RegularTransferRepositorySnapshot}, created from the build repository or by
 * {@link DefaultRegularTransferRepository#freeze()}. The snapshot owns its {@link TransferPathMap},
 * which is never written to after creation. Hence, the paths are effectively immutable and safe to
 * read from any number of request threads. The transfer-service cache is the only mutable state,
 * and it is thread-safe.
 *
 * @param <P> the transfer path/template type
 */
class DefaultRegularTransferRepositorySnapshot<P> implements RegularTransferRepositorySnapshot<P> {

  private final TransferPathMap<P> paths;
  private final RegularTransferServiceCache transferServiceCache =
    new RegularTransferServiceCache();

  DefaultRegularTransferRepositorySnapshot(TransferPathMap<P> paths) {
    this.paths = paths;
  }

  @Nullable
  @Override
  public P findPath(TransferProfileType profileType, int fromStop, int toStop) {
    return paths.findPath(profileType, fromStop, toStop);
  }

  @Override
  public List<StoredPath<P>> pathsFor(TransferProfileType profileType) {
    return paths.pathsFor(profileType);
  }

  @Override
  public RaptorRegularTransferService getOrCreateTransferService(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences,
    Supplier<RaptorRegularTransferService> factory
  ) {
    return transferServiceCache.getOrCreate(profileType, preferences, factory);
  }

  /**
   * Create a new mutable repository initialized with the state of this snapshot. Only the
   * lifecycle should have access to this, hence the package local access.
   */
  DefaultRegularTransferRepository<P> copyOnWrite() {
    return new DefaultRegularTransferRepository<>(paths);
  }
}
