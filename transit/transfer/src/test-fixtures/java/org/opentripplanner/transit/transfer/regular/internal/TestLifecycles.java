package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Lifecycles for tests that only read and write the stored paths, not the Raptor transfers
 * derived from them.
 */
final class TestLifecycles {

  private TestLifecycles() {}

  /** A lifecycle whose snapshots cannot derive Raptor transfers. */
  static <P> RegularTransferRepositoryLifecycle<P> pathsOnly() {
    return new RegularTransferRepositoryLifecycle<>(new UnsupportedPathProvider<>(), 0);
  }

  /**
   * Create the runtime repository from the build repository, and return its initial snapshot,
   * which cannot derive Raptor transfers.
   */
  static <P> RegularTransferRepositorySnapshot<P> initialSnapshot(
    RegularTransferBuildRepository<P> buildRepository
  ) {
    return TestLifecycles.<P>pathsOnly().freeze(buildRepository.createRuntimeRepository());
  }

  private static final class UnsupportedPathProvider<P> implements
    TransferPathProvider<P, AbstractUserPreferences<?>> {

    @Override
    public Collection<TransferPath<P>> findNearbyStops(
      FeedScopedId fromStop,
      AbstractUserPreferences<?> userPreferences
    ) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<PathCriteria> computePathCriteria(
      P path,
      AbstractUserPreferences<?> userPreferences
    ) {
      throw new UnsupportedOperationException();
    }
  }
}
