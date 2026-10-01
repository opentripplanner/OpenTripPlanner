package org.opentripplanner.transit.transfer.regular;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * An immutable, read-only snapshot of the {@link RegularTransferRepository}. A new snapshot is
 * published each time a transaction that touched the repository commits. A request resolves one
 * snapshot at its start and uses it for both building the transfers and recovering the path of a
 * transfer used in an itinerary, so the two are always consistent.
 *
 * @param <P> the transfer path/template type
 */
public interface RegularTransferRepositorySnapshot<P> {
  /**
   * Recover the real street path template for a resolved transfer - used by itinerary mapping to
   * build a transfer leg's geometry/walk-steps, since the routing-time {@code RaptorTransfers}
   * lookup only carries {@code (stop, duration, c1)}, not the path itself.
   */
  @Nullable
  P findPath(TransferProfileType profileType, int fromStop, int toStop);

  /** Every stored {@code (fromStop, toStop, path)} for a profile. Empty if it has none. */
  List<StoredPath<P>> pathsFor(TransferProfileType profileType);

  record StoredPath<P>(int fromStop, int toStop, P path) {}
}
