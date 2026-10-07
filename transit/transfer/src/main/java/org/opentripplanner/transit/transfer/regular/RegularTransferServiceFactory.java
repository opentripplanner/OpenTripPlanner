package org.opentripplanner.transit.transfer.regular;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * Provides the {@link RaptorRegularTransferService} for a {@code (profileType, preferences)} by
 * re-costing the path templates of a {@link RegularTransferRepositorySnapshot} under the
 * request-time preferences, and recovers the path template of a transfer used in an itinerary.
 * Results are cached in the snapshot, keyed on
 * {@code (profileType, preferences)} - normalizing the preferences so equivalent requests share a
 * cache entry is the caller's responsibility, not this factory's.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferServiceFactory<P> {
  /**
   * Return the transfer service for {@code (profileType, preferences)}, built on a cache miss. See
   * {@link RegularTransferRepositorySnapshot#getOrCreateTransferService} for the caching.
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
