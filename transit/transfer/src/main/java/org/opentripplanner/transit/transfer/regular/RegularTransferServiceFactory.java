package org.opentripplanner.transit.transfer.regular;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorTransferStore;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The regular transfers of one request, read from the {@link RegularTransferRepositorySnapshot} of
 * the request. It provides the {@link RaptorRegularTransferService} for a
 * {@code userPreferences} by re-costing the snapshot's path templates - generated at
 * graph build or committed at runtime - under the request-time preferences, and recovers the path
 * template of a transfer used in an itinerary. Results are cached in the snapshot, keyed on
 * {@code userPreferences} - normalizing the preferences so equivalent requests share a
 * cache entry is the caller's responsibility, not this factory's.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferServiceFactory<P> {
  /**
   * Return the transfer service for {@code userPreferences}, built on a cache miss. See
   * {@link RegularTransferRepositorySnapshot#getOrCreateTransferService} for the caching.
   */
  public RaptorRegularTransferService create(AbstractUserPreferences<?> userPreferences);

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since a {@link RaptorTransferStore} lookup only
   * carries {@code (stop, duration, c1)}, not the path itself.
   */
  @Nullable
  public P findPath(AbstractUserPreferences<?> userPreferences, int fromStop, int toStop);
}
