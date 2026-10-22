package org.opentripplanner.transit.transfer.regular.spi;

import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;

/**
 * The only boundary the regular-transfer pipeline has into street-search territory. The street
 * path type {@code P} stays fully opaque to this module - it never sees
 * {@code Edge}/{@code State}/{@code RouteRequest}. The real implementation (backed by a street
 * search) lives in {@code application} and is injected in.
 *
 * @param <P> the street path type
 */
public interface TransferPathProvider<P> {
  /**
   * Discover candidate paths from {@code fromStop} under {@code userPreferences}, each
   * paired with the {@link PathCriteria} it was found under. Called once per stop per profile at
   * graph-build time.
   */
  Collection<TransferPath<P>> findNearbyStops(
    FeedScopedId fromStop,
    AbstractUserPreferences<?> userPreferences
  );

  /**
   * Re-cost a previously discovered path under different {@code userPreferences} than
   * the one that found it. Used for {@code deduplicationProfile}/dedup comparisons, to re-cost a
   * deduplicationProfile's path under a dependent profile's preferences. Empty if the path exceeds
   * that profile's duration limit.
   */
  Optional<PathCriteria> computePathCriteria(P path, AbstractUserPreferences<?> userPreferences);
}
