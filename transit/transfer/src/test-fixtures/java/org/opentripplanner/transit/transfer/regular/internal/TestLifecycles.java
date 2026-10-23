package org.opentripplanner.transit.transfer.regular.internal;

import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

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

  private static final class UnsupportedPathProvider<P> implements TransferPathProvider<P> {

    @Override
    public TransferSearch<P> searchFor(AbstractUserPreferences<?> userPreferences) {
      throw new UnsupportedOperationException();
    }
  }
}
