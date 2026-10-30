package org.opentripplanner.transit.transfer.regular.spi;

import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.maxduration.AllowedMode;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;

/**
 * Which modes a stop is allowed for, that is, which modes are allowed on a trip that visits the
 * stop. It is used to select the max duration of a transfer, see
 * {@link MaxDurationsPreference}.
 * <p>
 * Implementations are immutable and thread-safe.
 */
@FunctionalInterface
public interface AllowedModesLookup {
  /** No stop is allowed for any mode. */
  AllowedModesLookup NONE = stopId -> Set.of();

  /** The modes the stop is allowed for, empty if there are none. */
  Set<AllowedMode> modesAt(FeedScopedId stopId);
}
