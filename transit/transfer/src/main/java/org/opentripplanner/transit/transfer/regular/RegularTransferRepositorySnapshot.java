package org.opentripplanner.transit.transfer.regular;

import javax.annotation.Nullable;
import org.opentripplanner.raptor.data.transfers.regular.RaptorRegularTransferService;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * A read-only snapshot of the {@link RegularTransferRepository}, and the read access to the
 * regular transfers. A new snapshot is published each time a transaction that touched the
 * repository commits. A request resolves one snapshot at its start and uses it for both routing
 * and recovering the path of a transfer used in an itinerary, so the two are always consistent.
 * <p>
 * The paths are immutable. The only mutable state is a cache of the Raptor transfers derived from
 * them, see {@link #raptorTransfers}.
 *
 * @param <P> the street path type
 */
public interface RegularTransferRepositorySnapshot<P> {
  /**
   * The Raptor transfers for {@code userPreferences}: the paths stored under the user preferences
   * closest to {@code userPreferences}, re-costed under {@code userPreferences}.
   * <p>
   * The result is cached in this snapshot, in a bounded (LRU) and thread-safe cache keyed on
   * {@code userPreferences}. The cached transfers are always derived from the paths of this
   * snapshot: a value cached by the previous snapshot is reused if the paths it was derived from
   * did not change, and updated for the stops whose transfers changed otherwise. Normalizing the
   * preferences, so that equivalent requests share a cache entry, is the caller's
   * responsibility.
   */
  RaptorRegularTransferService raptorTransfers(AbstractUserPreferences<?> userPreferences);

  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since a Raptor transfer only carries
   * {@code (stop, duration, c1)}, not the path itself. The path is read from the paths stored
   * under the user preferences closest to {@code userPreferences}.
   */
  @Nullable
  P findPath(AbstractUserPreferences<?> userPreferences, int fromStop, int toStop);
}
