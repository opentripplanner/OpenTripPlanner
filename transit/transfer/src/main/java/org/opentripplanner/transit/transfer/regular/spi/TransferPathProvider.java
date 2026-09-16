package org.opentripplanner.transit.transfer.regular.spi;

import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;
import org.opentripplanner.core.model.id.FeedScopedId;

import java.util.Collection;
import java.util.Optional;

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
   * Discover candidate paths from {@code fromStop} under {@code (profileId, preferences)}.
   * Called once per stop per profile at graph-build time.
   */
  Collection<NearbyPath<P>> findNearbyStops(
    FeedScopedId fromStop,
    RaptorTransferProfile profileId,
    U preferences
  );

  /**
   * Re-cost a previously discovered path under a specific {@code (profileId, preferences)}.
   * Called both to cost a profile's own candidates and, for {@code base}/dedup comparisons, to
   * re-cost a base profile's path under a dependent profile's preferences. Empty if the path
   * exceeds that profile's duration limit.
   */
  Optional<PathCriteria> computePathCriteria(P path, RaptorTransferProfile profileId, U preferences);
}
