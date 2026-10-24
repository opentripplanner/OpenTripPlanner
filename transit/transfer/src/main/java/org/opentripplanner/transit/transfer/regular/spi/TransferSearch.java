package org.opentripplanner.transit.transfer.regular.spi;

import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.MaxDurationsPreference;

/**
 * The street search and re-costing of transfer paths under one set of user preferences, see
 * {@link TransferPathProvider#searchFor}. Creating a search is expensive compared to re-costing
 * one path with it, so a search should be reused for all paths under the same preferences.
 * <p>
 * Implementations are immutable and thread-safe.
 *
 * @param <P> the street path type
 */
public interface TransferSearch<P> {
  /**
   * Discover candidate paths from {@code fromStop}, each paired with the {@link PathCriteria} it
   * was found under. There may be several candidates to the same stop.
   *
   * @param maxDurations how far to search, the limit of the profile type. It bounds this
   *                     discovery search only, see {@link #computePathCriteria}.
   */
  Collection<TransferPath<P>> findNearbyStops(
    FeedScopedId fromStop,
    MaxDurationsPreference maxDurations
  );

  /**
   * Re-cost a previously discovered path, which may have been found under other user preferences
   * than the ones of this search. Empty if the path can not be traversed with this search. The max
   * duration of the profile type is not applied, it bounds the discovery search only.
   */
  Optional<PathCriteria> computePathCriteria(P path);
}
