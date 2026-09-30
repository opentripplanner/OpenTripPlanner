package org.opentripplanner.transit.transfer.regular.spi;

import java.util.Collection;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

/**
 * The only boundary {@code raptor-data}'s regular-transfer pipeline has into street-search
 * territory. {@code P} (the path/template type) and {@code U} (the preferences type) stay fully
 * opaque here - {@code raptor-data} never sees {@code Edge}/{@code State}/{@code RouteRequest}.
 * The real implementation (backed by a street search) lives in {@code application} and is
 * injected in.
 *
 * @param <P> the transfer path/template type
 * @param <U> the user preferences type
 */
public interface TransferPathProvider<P, U> {
  /**
   * Discover candidate paths from {@code fromStop} under {@code (profileId, preferences)}, each
   * paired with the {@link PathCriteria} it was found under. Called once per stop per profile at
   * graph-build time.
   */
  Collection<TransferPath<P>> findNearbyStops(
    FeedScopedId fromStop,
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  );

  /**
   * Re-cost a previously discovered path under a different {@code (profileId, preferences)} than
   * the one that found it. Used for {@code deduplicationProfile}/dedup comparisons, to re-cost a
   * deduplicationProfile's path under a dependent profile's preferences. Empty if the path exceeds
   * that profile's duration limit.
   */
  Optional<PathCriteria> computePathCriteria(
    P path,
    TransferProfileType profileType,
    AbstractUserPreferences<?> preferences
  );
}
