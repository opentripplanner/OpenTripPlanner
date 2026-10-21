package org.opentripplanner.transit.transfer.regular.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.raptor.data.stop.StopIndex;
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
    return new RegularTransferRepositoryLifecycle<>(
      new StopIndex(0, List.<FeedScopedId>of(), id -> 0, id -> id),
      new UnsupportedPathProvider<>()
    );
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
