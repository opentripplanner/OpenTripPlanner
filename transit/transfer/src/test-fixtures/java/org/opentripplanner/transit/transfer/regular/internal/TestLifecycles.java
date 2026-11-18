package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Map;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferSearch;

/**
 * Lifecycles for tests that only read and write the stored paths, not the Raptor transfers
 * derived from them.
 */
final class TestLifecycles {

  private TestLifecycles() {}

  /** A lifecycle whose snapshots cannot derive Raptor transfers. */
  static <P> RegularTransferRepositoryLifecycle<P, WalkPreferences> pathsOnly() {
    return new RegularTransferRepositoryLifecycle<>(
      new UnsupportedPathProvider<>(),
      FakeRequestMapper.INSTANCE,
      Map.<AbstractUserPreferences<?>, WalkPreferences>of(),
      0
    );
  }

  /**
   * Create the runtime repository from the build repository, and return its initial snapshot,
   * which cannot derive Raptor transfers.
   */
  static <P> RegularTransferRepositorySnapshot<P, WalkPreferences> initialSnapshot(
    RegularTransferBuildRepository<P> buildRepository
  ) {
    return TestLifecycles.<P>pathsOnly().freeze(buildRepository.createRuntimeRepository());
  }

  private static final class UnsupportedPathProvider<P> implements
    TransferPathProvider<P, WalkPreferences> {

    @Override
    public TransferSearch<P> searchFor(WalkPreferences request) {
      throw new UnsupportedOperationException();
    }
  }
}
