package org.opentripplanner.transit.transfer.regular;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Builds a {@link RaptorTransferStore} for a given {@code (profileId, preferences)} by re-costing the
 * path templates in a {@link RegularTransferRepositorySnapshot}, generated at graph-build time, under
 * request-time preferences. Results are cached, bounded LRU, keyed on
 * {@code (profileId, preferences)} - normalizing {@code U} so equivalent requests share a cache
 * entry is the caller's responsibility, not this factory's.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferServiceFactory<P> {
  /**
   * Coarse-grained lock: a cache miss rebuilds the whole {@link RaptorTransferStore} for that
   * {@code (profileId, preferences)}, which is CPU-bound, not I/O - acceptable for the expected
   * handful of distinct combinations, revisit if contention shows up under load.
   */
  public RaptorRegularTransferService create(
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  );

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since a {@link RaptorTransferStore} lookup only
   * carries {@code (stop, duration, c1)}, not the path itself.
   */
  @Nullable
  public P findPath(TransferProfileType profileType, int fromStop, int toStop);
}
